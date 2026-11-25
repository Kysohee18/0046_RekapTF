package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"strings"
	"time"

	_ "github.com/jackc/pgx/v5/stdlib"
)

type app struct {
	store     storage
	secret    []byte
	extractor receiptExtractor
	providers bool
}

func main() {
	demo := envOr("REKAP_DEMO_MODE", "true")
	if demo != "true" && demo != "false" {
		log.Fatal("REKAP_DEMO_MODE harus true atau false")
	}
	addr := os.Getenv("ADDR")
	if addr == "" {
		addr = "127.0.0.1:8080"
		if demo == "false" && os.Getenv("PORT") != "" {
			addr = ":" + os.Getenv("PORT")
		}
	}
	host, _, err := net.SplitHostPort(addr)
	if ip := net.ParseIP(host); err != nil || (demo == "true" && host != "localhost" && (ip == nil || !ip.IsLoopback())) {
		log.Fatal("ADDR tidak valid atau mode demo tidak memakai alamat loopback")
	}

	dsn := os.Getenv("DATABASE_URL")
	secret := os.Getenv("JWT_SECRET")
	if demo == "false" && (dsn == "" || len(secret) < 32) {
		log.Fatal("DATABASE_URL dan JWT_SECRET minimal 32 byte wajib di luar mode demo")
	}
	if (dsn != "" || secret != "") && len(secret) < 32 {
		log.Fatal("JWT_SECRET minimal 32 byte jika menggunakan PostgreSQL atau menetapkan rahasia sendiri")
	}
	if secret == "" {
		secret = newID() + newID()
	}
	var extractor receiptExtractor = demoExtractor{}
	if demo == "false" {
		provider := configuredExtractor()
		if provider == nil {
			log.Fatal("DEEPSEEK_API_KEY wajib dan konfigurasi Baidu harus lengkap jika dipakai")
		}
		extractor = provider
	}

	var store storage = newMemoryStore()
	storageMode := "memory"
	if dsn != "" {
		db, err := sql.Open("pgx", dsn)
		if err != nil {
			log.Fatal("Konfigurasi PostgreSQL tidak valid")
		}
		defer db.Close()
		ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
		err = migrate(ctx, db)
		cancel()
		if err != nil {
			log.Fatal("PostgreSQL gagal diinisialisasi")
		}
		store = postgresStore{db: db}
		storageMode = "postgres"
	}

	api := &app{store: store, secret: []byte(secret), extractor: extractor, providers: demo == "false"}
	server := &http.Server{
		Addr:              addr,
		Handler:           api.routes(storageMode),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       20 * time.Second,
		WriteTimeout:      65 * time.Second,
		IdleTimeout:       60 * time.Second,
	}
	log.Printf("API RekapTF mendengarkan di %s (penyimpanan: %s, demo: %s)", addr, storageMode, demo)
	log.Fatal(server.ListenAndServe())
}

func (a *app) routes(storageMode string) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()
		if err := a.store.health(ctx); err != nil {
			fail(w, http.StatusServiceUnavailable, "Penyimpanan sedang tidak tersedia.")
			return
		}
		mode := "demo"
		if a.providers {
			mode = "production"
		}
		write(w, http.StatusOK, map[string]string{"status": "ok", "mode": mode, "storage": storageMode})
	})
	mux.HandleFunc("POST /v1/auth/register", a.register)
	mux.HandleFunc("POST /v1/auth/login", a.login)
	mux.HandleFunc("POST /v1/auth/refresh", a.refresh)
	mux.HandleFunc("POST /v1/auth/logout", a.logout)
	mux.Handle("GET /v1/account", a.auth(http.HandlerFunc(a.currentAccount)))
	a.registerResources(mux)
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		mux.ServeHTTP(w, r)
	})
}

func (a *app) currentAccount(w http.ResponseWriter, r *http.Request) {
	business, err := a.store.businessByID(r.Context(), workspaceID(r))
	switch {
	case errors.Is(err, errNotFound):
		fail(w, http.StatusUnauthorized, "Sesi tidak valid. Silakan masuk kembali.")
	case err != nil:
		fail(w, http.StatusInternalServerError, "Akun gagal dimuat. Coba lagi.")
	default:
		write(w, http.StatusOK, map[string]string{"businessName": business, "role": "UMKM_USER"})
	}
}

func write(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}

func fail(w http.ResponseWriter, status int, message string) {
	write(w, status, map[string]string{"message": message})
}

func body(w http.ResponseWriter, r *http.Request, value any) error {
	decoder := json.NewDecoder(http.MaxBytesReader(w, r.Body, 32<<10))
	var object json.RawMessage
	if err := decoder.Decode(&object); err != nil {
		return err
	}
	if len(object) == 0 || object[0] != '{' {
		return errors.New("expected a JSON object")
	}
	if err := decoder.Decode(new(any)); !errors.Is(err, io.EOF) {
		return errors.New("expected one JSON value")
	}
	return json.Unmarshal(object, value)
}

func envOr(name, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(name)); value != "" {
		return value
	}
	return fallback
}
