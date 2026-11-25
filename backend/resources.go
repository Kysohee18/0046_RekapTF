package main

import (
	"encoding/hex"
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"
)

const (
	sourceLivin = "Livin’ by Mandiri"
	dateLayout  = "2006-01-02"
)

type validationError string

func (e validationError) Error() string { return string(e) }

type incomeFields struct {
	Amount     int64  `json:"amount"`
	PaidOn     string `json:"paidOn"`
	PayerName  string `json:"payerName"`
	Reference  string `json:"reference"`
	Note       string `json:"note"`
	Source     string `json:"source"`
	CategoryID string `json:"categoryId"`
	PayerID    string `json:"payerId"`
}

type incomeRecord struct {
	ID string `json:"id"`
	incomeFields
	Status string `json:"status,omitempty"`
}

type namedRecord struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	Note string `json:"note"`
}

type recapRecord struct {
	ID     string `json:"id"`
	Name   string `json:"name"`
	Period string `json:"period"`
	From   string `json:"from"`
	To     string `json:"to"`
}

type recordList struct {
	Items []json.RawMessage `json:"items"`
}

type recapResponse struct {
	From  string            `json:"from"`
	To    string            `json:"to"`
	Total int64             `json:"total"`
	Count int               `json:"count"`
	Items []json.RawMessage `json:"items"`
}

func (a *app) registerResources(mux *http.ServeMux) {
	bind := func(pattern string, handle func(http.ResponseWriter, *http.Request, string, string)) {
		mux.Handle(pattern, a.auth(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			id := r.PathValue("id")
			if id != "" && !validRecordID(id) {
				fail(w, http.StatusNotFound, "Data tidak ditemukan.")
				return
			}
			handle(w, r, workspaceID(r), id)
		})))
	}
	for _, resource := range []struct{ path, kind string }{
		{"drafts", "draft"}, {"transactions", "transaction"}, {"categories", "category"},
		{"payers", "payer"}, {"saved-recaps", "savedRecap"},
	} {
		path := "/v1/" + resource.path
		item := path + "/{id}"
		bind("GET "+path, func(w http.ResponseWriter, r *http.Request, workspace, _ string) {
			a.list(w, r, workspace, resource.kind)
		})
		if resource.kind != "transaction" && resource.kind != "draft" {
			bind("POST "+path, func(w http.ResponseWriter, r *http.Request, workspace, _ string) {
				a.save(w, r, workspace, resource.kind, "")
			})
		}
		bind("GET "+item, func(w http.ResponseWriter, r *http.Request, workspace, id string) {
			a.get(w, r, workspace, resource.kind, id)
		})
		bind("PUT "+item, func(w http.ResponseWriter, r *http.Request, workspace, id string) {
			a.save(w, r, workspace, resource.kind, id)
		})
		bind("DELETE "+item, func(w http.ResponseWriter, r *http.Request, workspace, id string) {
			a.remove(w, r, workspace, resource.kind, id)
		})
	}
	bind("POST /v1/drafts/extract", func(w http.ResponseWriter, r *http.Request, workspace, _ string) {
		a.extract(w, r, workspace)
	})
	bind("POST /v1/drafts/extract-text", func(w http.ResponseWriter, r *http.Request, workspace, _ string) {
		a.extractText(w, r, workspace)
	})
	bind("POST /v1/drafts/{id}/confirm", a.confirm)
	bind("GET /v1/recap", func(w http.ResponseWriter, r *http.Request, workspace, _ string) {
		a.recap(w, r, workspace)
	})
}

func validRecordID(value string) bool {
	if len(value) != 32 {
		return false
	}
	_, err := hex.DecodeString(value)
	return err == nil
}

func (a *app) list(w http.ResponseWriter, r *http.Request, workspace, kind string) {
	query := r.URL.Query()
	q := strings.TrimSpace(query.Get("q"))
	if !utf8.ValidString(q) || strings.ContainsRune(q, 0) {
		fail(w, http.StatusBadRequest, "Kata kunci tidak valid.")
		return
	}
	q = strings.ToLower(q)
	from, to := "", ""
	if kind == "transaction" {
		from, to = query.Get("from"), query.Get("to")
		if err := validRange(from, to, false); err != nil {
			recordError(w, err, "Data gagal dimuat.")
			return
		}
	}
	items, err := a.find(r, workspace, kind, from, to)
	if err != nil {
		recordError(w, err, "Data gagal dimuat.")
		return
	}
	if q != "" {
		var matchingNames map[string]bool
		if kind == "draft" || kind == "transaction" {
			matchingNames, err = a.matchLinkedNames(r, workspace, q)
			if err != nil {
				recordError(w, err, "Data gagal dimuat.")
				return
			}
		}
		filtered := items[:0]
		for _, item := range items {
			if recordMatches(kind, item, q, matchingNames) {
				filtered = append(filtered, item)
			}
		}
		items = filtered
	}
	write(w, http.StatusOK, recordList{items})
}

func (a *app) find(r *http.Request, workspace, kind, from, to string) ([]json.RawMessage, error) {
	var items []json.RawMessage
	err := a.store.records(r.Context(), workspace, false, func(s recordStore) error {
		var err error
		items, err = s.list(kind, from, to)
		return err
	})
	return items, err
}

func (a *app) matchLinkedNames(r *http.Request, workspace, q string) (map[string]bool, error) {
	names := make(map[string]bool)
	err := a.store.records(r.Context(), workspace, false, func(s recordStore) error {
		for _, kind := range []string{"category", "payer"} {
			items, err := s.list(kind, "", "")
			if err != nil {
				return err
			}
			for _, item := range items {
				var record namedRecord
				if err := json.Unmarshal(item, &record); err != nil {
					return err
				}
				if strings.Contains(strings.ToLower(record.Name), q) {
					names[record.ID] = true
				}
			}
		}
		return nil
	})
	return names, err
}

func recordMatches(kind string, data json.RawMessage, query string, matchingNames map[string]bool) bool {
	var fields []string
	switch kind {
	case "draft", "transaction":
		var v incomeRecord
		if json.Unmarshal(data, &v) != nil {
			return false
		}
		if matchingNames[v.CategoryID] || matchingNames[v.PayerID] {
			return true
		}
		fields = []string{
			strconv.FormatInt(v.Amount, 10), v.PaidOn, v.PayerName, v.Reference, v.Note, v.Source, v.Status,
		}
	case "savedRecap":
		var v recapRecord
		if json.Unmarshal(data, &v) != nil {
			return false
		}
		fields = []string{v.Name, v.Period, v.From, v.To}
	default:
		var v namedRecord
		if json.Unmarshal(data, &v) != nil {
			return false
		}
		fields = []string{v.Name}
	}
	for _, field := range fields {
		if strings.Contains(strings.ToLower(field), query) {
			return true
		}
	}
	return false
}

func (a *app) get(w http.ResponseWriter, r *http.Request, workspace, kind, id string) {
	var record storedRecord
	err := a.store.records(r.Context(), workspace, false, func(s recordStore) error {
		var err error
		record, err = s.get(kind, id)
		return err
	})
	if err != nil {
		recordError(w, err, "Data gagal dimuat.")
		return
	}
	write(w, http.StatusOK, record.data)
}

func (a *app) save(w http.ResponseWriter, r *http.Request, workspace, kind, id string) {
	var input json.RawMessage
	if err := body(w, r, &input); err != nil {
		fail(w, http.StatusBadRequest, "Data tidak valid.")
		return
	}
	updating := id != ""
	if !updating {
		id = newID()
	}
	var data json.RawMessage
	err := a.store.records(r.Context(), workspace, true, func(s recordStore) error {
		var previous storedRecord
		if updating {
			var err error
			previous, err = s.get(kind, id)
			if err != nil {
				return err
			}
		}
		value, err := readRecord(input, s, kind, id, previous.data)
		if err != nil {
			return err
		}
		data, err = json.Marshal(value)
		if err != nil {
			return err
		}
		if updating {
			return s.update(kind, id, data, nil)
		}
		return s.insert(kind, id, data, "")
	})
	if err != nil {
		recordError(w, err, "Data gagal disimpan.")
		return
	}
	status := http.StatusCreated
	if updating {
		status = http.StatusOK
	}
	write(w, status, data)
}

func readRecord(input json.RawMessage, s recordStore, kind, id string, previous json.RawMessage) (any, error) {
	switch kind {
	case "draft", "transaction":
		var fields incomeFields
		if err := json.Unmarshal(input, &fields); err != nil {
			return nil, validationError("Data pemasukan tidak valid.")
		}
		isDraft := kind == "draft"
		value := incomeRecord{ID: id, incomeFields: fields}
		value.Source = sourceLivin
		if len(previous) > 0 {
			var old incomeRecord
			if err := json.Unmarshal(previous, &old); err != nil {
				return nil, err
			}
			if old.Status == "converted" {
				return nil, errConverted
			}
			if old.Source != "" {
				value.Source = old.Source
			}
		}
		if isDraft {
			value.Status = "draft"
		}
		if err := value.clean(isDraft); err != nil {
			return nil, err
		}
		return value, validateLinks(s, value.incomeFields)
	case "savedRecap":
		var value recapRecord
		if err := json.Unmarshal(input, &value); err != nil {
			return nil, validationError("Data rekap tidak valid.")
		}
		value.ID = id
		value.Name = strings.TrimSpace(value.Name)
		if !validName(value.Name) {
			return nil, validationError("Isi nama rekap (maksimal 120 karakter).")
		}
		switch value.Period {
		case "day", "week", "month", "custom":
		default:
			return nil, validationError("Pilih periode rekap yang valid.")
		}
		if err := validRange(value.From, value.To, true); err != nil {
			return nil, err
		}
		return value, nil
	default:
		var value namedRecord
		if err := json.Unmarshal(input, &value); err != nil {
			return nil, validationError("Data tidak valid.")
		}
		value.ID = id
		value.Name = strings.TrimSpace(value.Name)
		value.Note = strings.TrimSpace(value.Note)
		if !validName(value.Name) || utf8.RuneCountInString(value.Note) > 1000 || strings.ContainsRune(value.Note, 0) {
			return nil, validationError("Isi nama (maksimal 120 karakter) dan catatan maksimal 1000 karakter.")
		}
		return value, nil
	}
}

func validName(value string) bool {
	return value != "" && utf8.RuneCountInString(value) <= 120 && !strings.ContainsRune(value, 0)
}

func (v *incomeFields) clean(draft bool) error {
	v.PaidOn = strings.TrimSpace(v.PaidOn)
	v.PayerName = strings.TrimSpace(v.PayerName)
	v.Reference = strings.TrimSpace(v.Reference)
	v.Note = strings.TrimSpace(v.Note)
	v.CategoryID = strings.TrimSpace(v.CategoryID)
	v.PayerID = strings.TrimSpace(v.PayerID)
	if v.Amount < 0 || (!draft && v.Amount == 0) {
		return validationError("Nominal pemasukan harus lebih dari Rp0.")
	}
	if (!draft || v.PaidOn != "") && !validDate(v.PaidOn) {
		return validationError("Isi tanggal pembayaran yang valid.")
	}
	if utf8.RuneCountInString(v.PayerName) > 120 || utf8.RuneCountInString(v.Reference) > 120 ||
		utf8.RuneCountInString(v.Note) > 1000 {
		return validationError("Data pemasukan terlalu panjang.")
	}
	for _, field := range []string{v.PayerName, v.Reference, v.Note} {
		if strings.ContainsRune(field, 0) {
			return validationError("Isian pemasukan tidak valid.")
		}
	}
	return nil
}

func validateLinks(s recordStore, v incomeFields) error {
	for _, link := range []struct{ kind, id string }{{"category", v.CategoryID}, {"payer", v.PayerID}} {
		if link.id == "" {
			continue
		}
		if !validRecordID(link.id) {
			return errInvalidLink
		}
		if _, err := s.get(link.kind, link.id); err != nil {
			if errors.Is(err, errNotFound) {
				return errInvalidLink
			}
			return err
		}
	}
	return nil
}

func (a *app) remove(w http.ResponseWriter, r *http.Request, workspace, kind, id string) {
	err := a.store.records(r.Context(), workspace, true, func(s recordStore) error {
		return s.remove(kind, id)
	})
	if err != nil {
		recordError(w, err, "Data gagal dihapus.")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (a *app) confirm(w http.ResponseWriter, r *http.Request, workspace, id string) {
	var fields incomeFields
	if err := body(w, r, &fields); err != nil {
		fail(w, http.StatusBadRequest, "Periksa kembali data pemasukan.")
		return
	}
	var result json.RawMessage
	status := http.StatusCreated
	err := a.store.records(r.Context(), workspace, true, func(s recordStore) error {
		previous, err := s.get("draft", id)
		if err != nil {
			return err
		}
		if len(previous.confirmation) > 0 {
			result = previous.confirmation
			status = http.StatusOK
			return nil
		}
		var draft incomeRecord
		if err := json.Unmarshal(previous.data, &draft); err != nil {
			return err
		}
		if draft.Status == "converted" {
			return errConverted
		}
		fields.Source = draft.Source
		if fields.Source == "" {
			fields.Source = sourceLivin
		}
		if err := fields.clean(false); err != nil {
			return err
		}
		if err := validateLinks(s, fields); err != nil {
			return err
		}
		transaction := incomeRecord{ID: newID(), incomeFields: fields}
		result, err = json.Marshal(transaction)
		if err != nil {
			return err
		}
		draft.Status = "converted"
		draft.incomeFields = fields
		data, err := json.Marshal(draft)
		if err != nil {
			return err
		}
		if err := s.insert("transaction", transaction.ID, result, id); err != nil {
			return err
		}
		return s.update("draft", id, data, result)
	})
	if err != nil {
		recordError(w, err, "Pemasukan gagal disimpan.")
		return
	}
	write(w, status, result)
}

func (a *app) recap(w http.ResponseWriter, r *http.Request, workspace string) {
	from, to := r.URL.Query().Get("from"), r.URL.Query().Get("to")
	if err := validRange(from, to, true); err != nil {
		recordError(w, err, "Rekap gagal dimuat.")
		return
	}
	items, err := a.find(r, workspace, "transaction", from, to)
	if err != nil {
		recordError(w, err, "Rekap gagal dimuat.")
		return
	}
	var total int64
	for _, item := range items {
		var value incomeFields
		if err := json.Unmarshal(item, &value); err != nil || value.Amount > math.MaxInt64-total {
			fail(w, http.StatusInternalServerError, "Total pemasukan tidak dapat dihitung.")
			return
		}
		total += value.Amount
	}
	write(w, http.StatusOK, recapResponse{from, to, total, len(items), items})
}

func validDate(value string) bool {
	parsed, err := time.Parse(dateLayout, value)
	return err == nil && parsed.Year() > 0 && parsed.Format(dateLayout) == value
}

func validRange(from, to string, required bool) error {
	if (required || from != "") && !validDate(from) {
		return validationError("Tanggal awal periode tidak valid.")
	}
	if (required || to != "") && !validDate(to) {
		return validationError("Tanggal akhir periode tidak valid.")
	}
	if from != "" && to != "" && from > to {
		return validationError("Tanggal akhir harus sama dengan atau setelah tanggal awal.")
	}
	return nil
}

func recordError(w http.ResponseWriter, err error, message string) {
	var validation validationError
	switch {
	case errors.As(err, &validation):
		fail(w, http.StatusBadRequest, validation.Error())
	case errors.Is(err, errNotFound):
		fail(w, http.StatusNotFound, "Data tidak ditemukan.")
	case errors.Is(err, errConverted):
		fail(w, http.StatusConflict, "Draf sudah menjadi pemasukan.")
	case errors.Is(err, errConflict):
		fail(w, http.StatusConflict, "Data masih dipakai pada draf atau pemasukan.")
	case errors.Is(err, errInvalidLink):
		fail(w, http.StatusBadRequest, "Kategori atau sumber pembayar tidak ditemukan.")
	default:
		fail(w, http.StatusInternalServerError, message)
	}
}
