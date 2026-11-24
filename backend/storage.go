package main

import (
	"context"
	"crypto/rand"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"errors"
	"sort"
	"strings"
	"sync"

	"github.com/jackc/pgx/v5/pgconn"
)

var (
	errNotFound    = errors.New("not found")
	errConflict    = errors.New("conflict")
	errConverted   = errors.New("converted draft")
	errInvalidLink = errors.New("invalid link")
)

type account struct {
	ID, Business, Email, PasswordHash string
}

type storedRecord struct {
	data         json.RawMessage
	confirmation json.RawMessage
	order        int
}

type recordStore interface {
	list(kind, from, to string) ([]json.RawMessage, error)
	get(kind, id string) (storedRecord, error)
	insert(kind, id string, data json.RawMessage, draftID string) error
	update(kind, id string, data, confirmation json.RawMessage) error
	remove(kind, id string) error
}

type storage interface {
	health(context.Context) error
	register(context.Context, account, string) error
	accountByEmail(context.Context, string) (account, error)
	businessByID(context.Context, string) (string, error)
	setRefresh(context.Context, string, string) error
	rotateRefresh(context.Context, string, string, string) (string, error)
	revokeRefresh(context.Context, string, string) error
	records(context.Context, string, bool, func(recordStore) error) error
}

func newID() string {
	var value [16]byte
	_, _ = rand.Read(value[:])
	return hex.EncodeToString(value[:])
}

func migrate(ctx context.Context, db *sql.DB) error {
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()
	for _, statement := range []string{
		`SELECT pg_advisory_xact_lock(904235510)`,
		`CREATE TABLE IF NOT EXISTS accounts (
			id text PRIMARY KEY, business text NOT NULL, email text UNIQUE NOT NULL,
			password text NOT NULL, refresh_hash text
		)`,
		`CREATE TABLE IF NOT EXISTS categories (` + recordColumns + `)`,
		`CREATE TABLE IF NOT EXISTS payers (` + recordColumns + `)`,
		`CREATE TABLE IF NOT EXISTS saved_recaps (` + recordColumns + `)`,
		`CREATE TABLE IF NOT EXISTS drafts (` + recordColumns + `,
			confirmation jsonb,
			category_id text GENERATED ALWAYS AS (
				CASE WHEN data->>'status' = 'draft' THEN nullif(data->>'categoryId','') END
			) STORED,
			payer_id text GENERATED ALWAYS AS (
				CASE WHEN data->>'status' = 'draft' THEN nullif(data->>'payerId','') END
			) STORED,` + incomeLinks + `) `,
		`CREATE TABLE IF NOT EXISTS transactions (` + recordColumns + `,
			draft_id text,
			category_id text GENERATED ALWAYS AS (nullif(data->>'categoryId','')) STORED,
			payer_id text GENERATED ALWAYS AS (nullif(data->>'payerId','')) STORED,
			paid_on text GENERATED ALWAYS AS (data->>'paidOn') STORED,
			FOREIGN KEY(account, draft_id) REFERENCES drafts(account, id),
			UNIQUE(account, draft_id),` + incomeLinks + `) `,
		`CREATE INDEX IF NOT EXISTS transactions_workspace_date ON transactions (account, paid_on)`,
	} {
		if _, err := tx.ExecContext(ctx, statement); err != nil {
			return err
		}
	}
	return tx.Commit()
}

const recordColumns = `
	account text NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
	id text NOT NULL,
	data jsonb NOT NULL,
	created timestamptz NOT NULL DEFAULT now(),
	PRIMARY KEY(account, id)
`

const incomeLinks = `
	FOREIGN KEY(account, category_id) REFERENCES categories(account, id),
	FOREIGN KEY(account, payer_id) REFERENCES payers(account, id)
`

// Identifiers are selected only from the API's fixed resource kinds.
func recordTable(kind string) string {
	switch kind {
	case "category":
		return "categories"
	case "payer":
		return "payers"
	case "savedRecap":
		return "saved_recaps"
	case "draft":
		return "drafts"
	case "transaction":
		return "transactions"
	default:
		panic("unexpected record kind")
	}
}

type postgresStore struct{ db *sql.DB }

func (s postgresStore) health(ctx context.Context) error { return s.db.PingContext(ctx) }

func (s postgresStore) register(ctx context.Context, a account, refresh string) error {
	_, err := s.db.ExecContext(ctx, `INSERT INTO accounts(id,business,email,password,refresh_hash) VALUES($1,$2,$3,$4,$5)`,
		a.ID, a.Business, a.Email, a.PasswordHash, refresh)
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23505" {
		return errConflict
	}
	return err
}

func (s postgresStore) accountByEmail(ctx context.Context, email string) (account, error) {
	a := account{Email: email}
	err := s.db.QueryRowContext(ctx, `SELECT id,business,password FROM accounts WHERE email=$1`, email).
		Scan(&a.ID, &a.Business, &a.PasswordHash)
	return a, normalError(err)
}

func (s postgresStore) businessByID(ctx context.Context, id string) (string, error) {
	var business string
	err := s.db.QueryRowContext(ctx, "SELECT business FROM accounts WHERE id=$1", id).Scan(&business)
	return business, normalError(err)
}

func (s postgresStore) setRefresh(ctx context.Context, id, refresh string) error {
	result, err := s.db.ExecContext(ctx, `UPDATE accounts SET refresh_hash=$2 WHERE id=$1`, id, refresh)
	return checkAffected(result, err)
}

func (s postgresStore) rotateRefresh(ctx context.Context, id, previous, next string) (string, error) {
	var business string
	err := s.db.QueryRowContext(ctx, `UPDATE accounts SET refresh_hash=$3 WHERE id=$1 AND refresh_hash=$2 RETURNING business`,
		id, previous, next).Scan(&business)
	return business, normalError(err)
}

func (s postgresStore) revokeRefresh(ctx context.Context, id, refresh string) error {
	_, err := s.db.ExecContext(ctx, `UPDATE accounts SET refresh_hash=NULL WHERE id=$1 AND refresh_hash=$2`, id, refresh)
	return err
}

func (s postgresStore) records(ctx context.Context, id string, mutate bool, fn func(recordStore) error) error {
	if !mutate {
		return fn(postgresRecords{ctx, s.db, id})
	}
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()
	// Serializing writes in this workspace protects draft conversion and linked records.
	var lockedID string
	if err := tx.QueryRowContext(ctx, `SELECT id FROM accounts WHERE id=$1 FOR UPDATE`, id).Scan(&lockedID); err != nil {
		return normalError(err)
	}
	if err := fn(postgresRecords{ctx, tx, id}); err != nil {
		return err
	}
	return tx.Commit()
}

type queryable interface {
	ExecContext(context.Context, string, ...any) (sql.Result, error)
	QueryContext(context.Context, string, ...any) (*sql.Rows, error)
	QueryRowContext(context.Context, string, ...any) *sql.Row
}

type postgresRecords struct {
	ctx     context.Context
	db      queryable
	account string
}

func (s postgresRecords) list(kind, from, to string) ([]json.RawMessage, error) {
	query := `SELECT data FROM ` + recordTable(kind) + ` WHERE account=$1`
	args := []any{s.account}
	if from != "" || to != "" {
		query += ` AND ($2='' OR paid_on>=$2) AND ($3='' OR paid_on<=$3)`
		args = append(args, from, to)
	}
	rows, err := s.db.QueryContext(s.ctx, query+` ORDER BY created DESC,id`, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	results := []json.RawMessage{}
	for rows.Next() {
		var data json.RawMessage
		if err := rows.Scan(&data); err != nil {
			return nil, err
		}
		results = append(results, data)
	}
	return results, rows.Err()
}

func (s postgresRecords) get(kind, id string) (storedRecord, error) {
	var record storedRecord
	var confirmation []byte
	columns := "data"
	target := []any{&record.data}
	if kind == "draft" {
		columns += ",confirmation"
		target = append(target, &confirmation)
	}
	err := s.db.QueryRowContext(s.ctx, `SELECT `+columns+` FROM `+recordTable(kind)+` WHERE account=$1 AND id=$2`,
		s.account, id).Scan(target...)
	record.confirmation = confirmation
	return record, normalError(err)
}

func (s postgresRecords) insert(kind, id string, data json.RawMessage, draftID string) error {
	if draftID != "" {
		_, err := s.db.ExecContext(s.ctx, `INSERT INTO transactions(account,id,data,draft_id) VALUES($1,$2,$3,$4)`,
			s.account, id, data, draftID)
		return err
	}
	_, err := s.db.ExecContext(s.ctx, `INSERT INTO `+recordTable(kind)+`(account,id,data) VALUES($1,$2,$3)`,
		s.account, id, data)
	return err
}

func (s postgresRecords) update(kind, id string, data, confirmation json.RawMessage) error {
	query := `UPDATE ` + recordTable(kind) + ` SET data=$3`
	args := []any{s.account, id, data}
	if len(confirmation) > 0 {
		query += `,confirmation=$4`
		args = append(args, confirmation)
	}
	result, err := s.db.ExecContext(s.ctx, query+` WHERE account=$1 AND id=$2`, args...)
	return checkAffected(result, err)
}

func (s postgresRecords) remove(kind, id string) error {
	if kind == "draft" {
		// Discarding a draft preserves the corresponding confirmed income.
		if _, err := s.db.ExecContext(s.ctx, `UPDATE transactions SET draft_id=NULL WHERE account=$1 AND draft_id=$2`,
			s.account, id); err != nil {
			return err
		}
	}
	result, err := s.db.ExecContext(s.ctx, `DELETE FROM `+recordTable(kind)+` WHERE account=$1 AND id=$2`, s.account, id)
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23503" {
		return errConflict
	}
	return checkAffected(result, err)
}

func normalError(err error) error {
	if errors.Is(err, sql.ErrNoRows) {
		return errNotFound
	}
	return err
}

func checkAffected(result sql.Result, err error) error {
	if err != nil {
		return err
	}
	n, err := result.RowsAffected()
	if err == nil && n == 0 {
		return errNotFound
	}
	return err
}

type memoryAccount struct {
	account
	refresh string
	records memoryRecords
}

type memoryStore struct {
	mu       sync.Mutex
	accounts map[string]*memoryAccount
	emailIDs map[string]string
}

func newMemoryStore() *memoryStore {
	return &memoryStore{accounts: make(map[string]*memoryAccount), emailIDs: make(map[string]string)}
}

func (*memoryStore) health(context.Context) error { return nil }

func (s *memoryStore) register(_ context.Context, a account, refresh string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, exists := s.emailIDs[a.Email]; exists {
		return errConflict
	}
	s.accounts[a.ID] = &memoryAccount{a, refresh, memoryRecords{items: make(map[string]storedRecord)}}
	s.emailIDs[a.Email] = a.ID
	return nil
}

func (s *memoryStore) accountByEmail(_ context.Context, email string) (account, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if a := s.accounts[s.emailIDs[email]]; a != nil {
		return a.account, nil
	}
	return account{}, errNotFound
}

func (s *memoryStore) businessByID(_ context.Context, id string) (string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if a := s.accounts[id]; a != nil {
		return a.Business, nil
	}
	return "", errNotFound
}

func (s *memoryStore) setRefresh(_ context.Context, id, refresh string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	a := s.accounts[id]
	if a == nil {
		return errNotFound
	}
	a.refresh = refresh
	return nil
}

func (s *memoryStore) rotateRefresh(_ context.Context, id, previous, next string) (string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	a := s.accounts[id]
	if a == nil || a.refresh != previous {
		return "", errNotFound
	}
	a.refresh = next
	return a.Business, nil
}

func (s *memoryStore) revokeRefresh(_ context.Context, id, refresh string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if a := s.accounts[id]; a != nil && a.refresh == refresh {
		a.refresh = ""
	}
	return nil
}

func (s *memoryStore) records(_ context.Context, id string, mutate bool, fn func(recordStore) error) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	a := s.accounts[id]
	if a == nil {
		return errNotFound
	}
	if !mutate {
		return fn(&a.records)
	}
	next := memoryRecords{items: make(map[string]storedRecord, len(a.records.items)), nextOrder: a.records.nextOrder}
	for key, record := range a.records.items {
		next.items[key] = record
	}
	if err := fn(&next); err != nil {
		return err
	}
	a.records = next
	return nil
}

type memoryRecords struct {
	items     map[string]storedRecord
	nextOrder int
}

func (s *memoryRecords) list(kind, from, to string) ([]json.RawMessage, error) {
	matches := []storedRecord{}
	for key, record := range s.items {
		if !strings.HasPrefix(key, kind+"/") {
			continue
		}
		if from != "" || to != "" {
			var entry incomeFields
			if err := json.Unmarshal(record.data, &entry); err != nil {
				return nil, err
			}
			if (from != "" && entry.PaidOn < from) || (to != "" && entry.PaidOn > to) {
				continue
			}
		}
		matches = append(matches, record)
	}
	sort.Slice(matches, func(i, j int) bool { return matches[i].order > matches[j].order })
	items := make([]json.RawMessage, 0, len(matches))
	for _, match := range matches {
		items = append(items, match.data)
	}
	return items, nil
}

func (s *memoryRecords) get(kind, id string) (storedRecord, error) {
	record, ok := s.items[kind+"/"+id]
	if !ok {
		return storedRecord{}, errNotFound
	}
	return record, nil
}

func (s *memoryRecords) insert(kind, id string, data json.RawMessage, _ string) error {
	key := kind + "/" + id
	if _, exists := s.items[key]; exists {
		return errConflict
	}
	s.nextOrder++
	s.items[key] = storedRecord{data: data, order: s.nextOrder}
	return nil
}

func (s *memoryRecords) update(kind, id string, data, confirmation json.RawMessage) error {
	record, err := s.get(kind, id)
	if err != nil {
		return err
	}
	record.data, record.confirmation = data, confirmation
	s.items[kind+"/"+id] = record
	return nil
}

func (s *memoryRecords) remove(kind, id string) error {
	if _, err := s.get(kind, id); err != nil {
		return err
	}
	if kind == "category" || kind == "payer" {
		used, err := s.inUse(kind, id)
		if err != nil {
			return err
		}
		if used {
			return errConflict
		}
	}
	delete(s.items, kind+"/"+id)
	return nil
}

func (s *memoryRecords) inUse(kind, id string) (bool, error) {
	for key, stored := range s.items {
		recordKind, _, _ := strings.Cut(key, "/")
		switch recordKind {
		case "draft", "transaction":
			var record incomeRecord
			if err := json.Unmarshal(stored.data, &record); err != nil {
				return false, err
			}
			if record.Status != "converted" && ((kind == "category" && record.CategoryID == id) ||
				(kind == "payer" && record.PayerID == id)) {
				return true, nil
			}
		}
	}
	return false, nil
}
