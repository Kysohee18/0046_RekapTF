package main

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"net/http"
	"net/mail"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

const (
	issuer          = "rekaptf-api"
	audience        = "rekaptf-android"
	accessLifetime  = 15 * time.Minute
	refreshLifetime = 30 * 24 * time.Hour
)

var dummyPasswordHash, _ = bcrypt.GenerateFromPassword([]byte("rekaptf-dummy-password"), bcrypt.DefaultCost)

type authClaims struct {
	Kind string `json:"kind"`
	jwt.RegisteredClaims
}

type tokenPair struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	BusinessName string `json:"businessName"`
}

type refreshInput struct {
	RefreshToken string `json:"refreshToken"`
}

type identityKey struct{}

func workspaceID(r *http.Request) string {
	return r.Context().Value(identityKey{}).(string)
}

func (a *app) sign(id, kind string, lifetime time.Duration) (string, error) {
	now := time.Now()
	return jwt.NewWithClaims(jwt.SigningMethodHS256, authClaims{
		Kind: kind,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer: issuer, Audience: jwt.ClaimStrings{audience}, Subject: id, ID: newID(),
			IssuedAt: jwt.NewNumericDate(now), ExpiresAt: jwt.NewNumericDate(now.Add(lifetime)),
		},
	}).SignedString(a.secret)
}

func (a *app) parse(value, kind string) (string, error) {
	claims := new(authClaims)
	token, err := jwt.ParseWithClaims(value, claims, func(*jwt.Token) (any, error) {
		return a.secret, nil
	}, jwt.WithValidMethods([]string{"HS256"}), jwt.WithIssuer(issuer), jwt.WithAudience(audience),
		jwt.WithExpirationRequired(), jwt.WithIssuedAt())
	if err != nil || !token.Valid || claims.Kind != kind || claims.Subject == "" || claims.ID == "" {
		return "", errors.New("invalid session token")
	}
	return claims.Subject, nil
}

func tokenHash(value string) string {
	hash := sha256.Sum256([]byte(value))
	return hex.EncodeToString(hash[:])
}

func (a *app) tokens(id, business string) (tokenPair, error) {
	access, err := a.sign(id, "access", accessLifetime)
	if err != nil {
		return tokenPair{}, err
	}
	refresh, err := a.sign(id, "refresh", refreshLifetime)
	if err != nil {
		return tokenPair{}, err
	}
	return tokenPair{access, refresh, business}, nil
}

func (a *app) register(w http.ResponseWriter, r *http.Request) {
	var input struct {
		Business string `json:"businessName"`
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if err := body(w, r, &input); err != nil {
		fail(w, http.StatusBadRequest, "Data pendaftaran tidak valid.")
		return
	}
	business := strings.TrimSpace(input.Business)
	email := strings.ToLower(strings.TrimSpace(input.Email))
	parsed, err := mail.ParseAddress(email)
	if business == "" || utf8.RuneCountInString(business) > 120 || len(email) > 254 ||
		err != nil || parsed.Address != email || utf8.RuneCountInString(input.Password) < 8 || len(input.Password) > 72 {
		fail(w, http.StatusBadRequest, "Isi nama usaha, email yang valid, dan kata sandi minimal 8 karakter (maksimal 72 byte).")
		return
	}
	hash, err := bcrypt.GenerateFromPassword([]byte(input.Password), bcrypt.DefaultCost)
	if err != nil {
		fail(w, http.StatusInternalServerError, "Akun gagal dibuat.")
		return
	}
	id := newID()
	pair, err := a.tokens(id, business)
	if err == nil {
		err = a.store.register(r.Context(), account{id, business, email, string(hash)}, tokenHash(pair.RefreshToken))
	}
	switch {
	case errors.Is(err, errConflict):
		fail(w, http.StatusConflict, "Email sudah terdaftar.")
	case err != nil:
		fail(w, http.StatusInternalServerError, "Akun gagal dibuat.")
	default:
		write(w, http.StatusCreated, pair)
	}
}

func (a *app) login(w http.ResponseWriter, r *http.Request) {
	var input struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if err := body(w, r, &input); err != nil || len(input.Email) > 254 || len(input.Password) > 72 {
		fail(w, http.StatusBadRequest, "Data masuk tidak valid.")
		return
	}
	entry, err := a.store.accountByEmail(r.Context(), strings.ToLower(strings.TrimSpace(input.Email)))
	if errors.Is(err, errNotFound) {
		// Do comparable password work even when no email matches.
		_ = bcrypt.CompareHashAndPassword(dummyPasswordHash, []byte(input.Password))
		fail(w, http.StatusUnauthorized, "Email atau kata sandi salah.")
		return
	}
	if err != nil {
		fail(w, http.StatusInternalServerError, "Tidak dapat masuk. Coba lagi.")
		return
	}
	if bcrypt.CompareHashAndPassword([]byte(entry.PasswordHash), []byte(input.Password)) != nil {
		fail(w, http.StatusUnauthorized, "Email atau kata sandi salah.")
		return
	}
	pair, err := a.tokens(entry.ID, entry.Business)
	if err == nil {
		err = a.store.setRefresh(r.Context(), entry.ID, tokenHash(pair.RefreshToken))
	}
	if err != nil {
		fail(w, http.StatusInternalServerError, "Tidak dapat masuk. Coba lagi.")
		return
	}
	write(w, http.StatusOK, pair)
}

func (a *app) refresh(w http.ResponseWriter, r *http.Request) {
	var input refreshInput
	if err := body(w, r, &input); err != nil {
		fail(w, http.StatusBadRequest, "Token sesi tidak valid.")
		return
	}
	id, err := a.parse(input.RefreshToken, "refresh")
	if err != nil {
		fail(w, http.StatusUnauthorized, "Sesi berakhir. Silakan masuk kembali.")
		return
	}
	pair, err := a.tokens(id, "")
	if err == nil {
		pair.BusinessName, err = a.store.rotateRefresh(r.Context(), id, tokenHash(input.RefreshToken), tokenHash(pair.RefreshToken))
	}
	switch {
	case errors.Is(err, errNotFound):
		fail(w, http.StatusUnauthorized, "Sesi berakhir. Silakan masuk kembali.")
	case err != nil:
		fail(w, http.StatusInternalServerError, "Sesi gagal diperbarui. Coba lagi.")
	default:
		write(w, http.StatusOK, pair)
	}
}

func (a *app) logout(w http.ResponseWriter, r *http.Request) {
	var input refreshInput
	if err := body(w, r, &input); err != nil {
		fail(w, http.StatusBadRequest, "Token sesi tidak valid.")
		return
	}
	id, err := a.parse(input.RefreshToken, "refresh")
	if err == nil {
		if err := a.store.revokeRefresh(r.Context(), id, tokenHash(input.RefreshToken)); err != nil {
			fail(w, http.StatusInternalServerError, "Sesi gagal diakhiri. Coba lagi.")
			return
		}
	}
	w.WriteHeader(http.StatusNoContent)
}

func (a *app) auth(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		scheme, value, ok := strings.Cut(r.Header.Get("Authorization"), " ")
		if !ok || !strings.EqualFold(scheme, "Bearer") {
			fail(w, http.StatusUnauthorized, "Sesi tidak valid. Silakan masuk kembali.")
			return
		}
		id, err := a.parse(value, "access")
		if err != nil {
			fail(w, http.StatusUnauthorized, "Sesi tidak valid. Silakan masuk kembali.")
			return
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), identityKey{}, id)))
	})
}
