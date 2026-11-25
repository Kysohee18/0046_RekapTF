package main

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/stdlib"
)

func TestAccountAPI(t *testing.T) {
	t.Run("memory", func(t *testing.T) { testAccountAPI(t, newMemoryStore(), "memory") })
	t.Run("postgres", func(t *testing.T) { testAccountAPI(t, testPostgres(t), "postgres") })
}

func testAccountAPI(t *testing.T, store storage, storageMode string) {
	t.Helper()
	a := &app{store: store, secret: []byte("fixture-secret-for-account-test-only")}
	api := a.routes(storageMode)
	var health map[string]string
	decodeResponse(t, request(t, api, "GET", "/health", "", nil, 200), &health)
	if health["mode"] != "demo" || health["status"] != "ok" || health["storage"] != storageMode {
		t.Fatalf("unexpected health response: %v", health)
	}

	const email = "owner-a@example.test"
	alice := registerFixture(t, api, "Usaha A", email)
	bob := registerFixture(t, api, "Usaha B", "owner-b@example.test")
	request(t, api, "POST", "/v1/auth/register", "", map[string]string{
		"businessName": "Usaha lain", "email": strings.ToUpper(email), "password": "fixture-123",
	}, 409)
	request(t, api, "POST", "/v1/auth/login", "", map[string]string{"email": email, "password": "incorrect"}, 401)
	request(t, api, "GET", "/v1/account", "", nil, 401)
	request(t, api, "GET", "/v1/account", alice.RefreshToken, nil, 401)
	tampered := alice.AccessToken[:strings.LastIndex(alice.AccessToken, ".")] + ".aW52YWxpZA"
	request(t, api, "GET", "/v1/account", tampered, nil, 401)

	bobID, err := a.parse(bob.AccessToken, "access")
	if err != nil {
		t.Fatal("parse Bob's access token:", err)
	}
	assertAccount(t, api, bob.AccessToken, "/v1/account", "Usaha B")
	assertAccount(t, api, alice.AccessToken, "/v1/account?id="+bobID, "Usaha A")

	var loggedIn tokenPair
	decodeResponse(t, request(t, api, "POST", "/v1/auth/login", "", map[string]string{
		"email": strings.ToUpper(email), "password": "fixture-123",
	}, 200), &loggedIn)
	if loggedIn.BusinessName != "Usaha A" || loggedIn.RefreshToken == alice.RefreshToken {
		t.Fatal("login must return the same workspace with a different refresh token")
	}
	request(t, api, "POST", "/v1/auth/refresh", "", refreshInput{alice.RefreshToken}, 401)

	var renewed tokenPair
	decodeResponse(t, request(t, api, "POST", "/v1/auth/refresh", "", refreshInput{loggedIn.RefreshToken}, 200), &renewed)
	if renewed.BusinessName != "Usaha A" || renewed.RefreshToken == loggedIn.RefreshToken {
		t.Fatal("refresh must return the same workspace with a different refresh token")
	}
	request(t, api, "POST", "/v1/auth/refresh", "", refreshInput{loggedIn.RefreshToken}, 401)
	assertAccount(t, api, renewed.AccessToken, "/v1/account", "Usaha A")
	request(t, api, "POST", "/v1/auth/logout", "", refreshInput{renewed.RefreshToken}, 204)
	request(t, api, "POST", "/v1/auth/refresh", "", refreshInput{renewed.RefreshToken}, 401)
	assertAccount(t, api, bob.AccessToken, "/v1/account", "Usaha B")
}

func testPostgres(t *testing.T) postgresStore {
	t.Helper()
	dsn := os.Getenv("REKAP_TEST_DATABASE_URL")
	if dsn == "" {
		t.Skip("REKAP_TEST_DATABASE_URL is unset")
	}
	config, err := pgx.ParseConfig(dsn)
	if err != nil {
		t.Fatal("invalid test database configuration")
	}
	control := stdlib.OpenDB(*config)
	t.Cleanup(func() { _ = control.Close() })
	schema := pgx.Identifier{"rekaptf_test_" + newID()}.Sanitize()
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	if _, err := control.ExecContext(ctx, "CREATE SCHEMA "+schema); err != nil {
		t.Fatal("prepare test schema:", err)
	}
	t.Cleanup(func() {
		ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
		defer cancel()
		if _, err := control.ExecContext(ctx, "DROP SCHEMA "+schema+" CASCADE"); err != nil {
			t.Error("remove test schema:", err)
		}
	})
	config.RuntimeParams["search_path"] = schema
	db := stdlib.OpenDB(*config)
	t.Cleanup(func() { _ = db.Close() })
	for range 2 {
		if err := migrate(ctx, db); err != nil {
			t.Fatal("migrate test schema:", err)
		}
	}
	return postgresStore{db}
}

func registerFixture(t *testing.T, api http.Handler, businessName, email string) tokenPair {
	t.Helper()
	var pair tokenPair
	decodeResponse(t, request(t, api, "POST", "/v1/auth/register", "", map[string]string{
		"businessName": businessName, "email": email, "password": "fixture-123",
	}, 201), &pair)
	if pair.AccessToken == "" || pair.RefreshToken == "" || pair.BusinessName != businessName {
		t.Fatal("registration did not return both tokens and the workspace name")
	}
	return pair
}

func assertAccount(t *testing.T, api http.Handler, token, path, businessName string) {
	t.Helper()
	var profile map[string]string
	decodeResponse(t, request(t, api, "GET", path, token, nil, 200), &profile)
	if len(profile) != 2 || profile["businessName"] != businessName || profile["role"] != "UMKM_USER" {
		t.Fatalf("account response for %s did not match its token", path)
	}
}

func request(t *testing.T, api http.Handler, method, path, token string, payload any, status int) *httptest.ResponseRecorder {
	t.Helper()
	var encoded []byte
	if payload != nil {
		var err error
		encoded, err = json.Marshal(payload)
		if err != nil {
			t.Fatal(err)
		}
	}
	req := httptest.NewRequest(method, path, bytes.NewReader(encoded))
	if payload != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	response := httptest.NewRecorder()
	api.ServeHTTP(response, req)
	if response.Code != status {
		t.Fatalf("%s %s returned %d, want %d", method, path, response.Code, status)
	}
	return response
}

func decodeResponse(t *testing.T, response *httptest.ResponseRecorder, result any) {
	t.Helper()
	if err := json.NewDecoder(response.Body).Decode(result); err != nil {
		t.Fatal("decode API response:", err)
	}
}
