package main

import (
	"net/http"
	"testing"
)

func TestNamedResourcesAPI(t *testing.T) {
	t.Run("memory", func(t *testing.T) { testNamedResources(t, newMemoryStore(), "memory") })
	t.Run("postgres", func(t *testing.T) { testNamedResources(t, testPostgres(t), "postgres") })
}

func testNamedResources(t *testing.T, store storage, storageMode string) {
	t.Helper()
	a := &app{store: store, secret: []byte("fixture-secret-for-account-test-only")}
	api := a.routes(storageMode)
	owner := registerFixture(t, api, "Usaha A", "owner-a@example.test")
	other := registerFixture(t, api, "Usaha B", "owner-b@example.test")
	otherWorkspace, err := a.parse(other.AccessToken, "access")
	if err != nil {
		t.Fatal("parse second owner's access token:", err)
	}
	assertAccount(t, api, owner.AccessToken, "/v1/account", "Usaha A")
	assertAccount(t, api, other.AccessToken, "/v1/account", "Usaha B")
	groupPaths := []string{"/v1/categories", "/v1/payers"}
	for _, path := range groupPaths {
		t.Run(path, func(t *testing.T) {
			request(t, api, "GET", path, "", nil, 401)
			request(t, api, "GET", path, owner.RefreshToken, nil, 401)
			request(t, api, "GET", path+"/%00", owner.AccessToken, nil, 404)
			request(t, api, "GET", path+"?q=%00", owner.AccessToken, nil, 400)
			assertNamedList(t, api, path, owner.AccessToken, nil)
			request(t, api, "POST", path, owner.AccessToken, map[string]string{"name": "  "}, 400)

			var created namedRecord
			decodeResponse(t, request(t, api, "POST", path, owner.AccessToken, map[string]string{
				"id": otherWorkspace, "workspaceId": otherWorkspace, "name": " Rekap Utama ", "note": "  Data awal  ",
			}, 201), &created)
			if !validRecordID(created.ID) || created.ID == otherWorkspace || created.Name != "Rekap Utama" || created.Note != "Data awal" {
				t.Fatal("create did not return the server-owned ID and normalized fields")
			}

			itemPath := path + "/" + created.ID
			var actual namedRecord
			decodeResponse(t, request(t, api, "GET", itemPath, owner.AccessToken, nil, 200), &actual)
			if created != actual {
				t.Fatalf("created record differs from read result: %v versus %v", created, actual)
			}
			assertNamedList(t, api, path+"?q=UTAMA", owner.AccessToken, []namedRecord{created})
			assertNamedList(t, api, path+"?q=AWAL", owner.AccessToken, nil)
			assertNamedList(t, api, path+"?q=UTAMA", other.AccessToken, nil)
			request(t, api, "GET", itemPath, other.AccessToken, nil, 404)
			request(t, api, "PUT", itemPath, other.AccessToken, map[string]string{"name": "Milik B"}, 404)
			request(t, api, "DELETE", itemPath, other.AccessToken, nil, 404)

			decodeResponse(t, request(t, api, "PUT", itemPath, owner.AccessToken, map[string]string{"name": "Rekap Baru"}, 200), &actual)
			if actual != (namedRecord{ID: created.ID, Name: "Rekap Baru"}) {
				t.Fatal("update did not replace the existing fields")
			}
			assertNamedList(t, api, path+"?q=UTAMA", owner.AccessToken, nil)
			assertNamedList(t, api, path+"?q=baru", owner.AccessToken, []namedRecord{actual})

			request(t, api, "DELETE", itemPath, owner.AccessToken, nil, 204)
			request(t, api, "GET", itemPath, owner.AccessToken, nil, 404)
			assertNamedList(t, api, path, owner.AccessToken, nil)
		})
	}
}

func assertNamedList(t *testing.T, api http.Handler, path, token string, want []namedRecord) {
	t.Helper()
	var got struct {
		Items []namedRecord
	}
	decodeResponse(t, request(t, api, "GET", path, token, nil, 200), &got)
	if got.Items == nil || len(got.Items) != len(want) {
		t.Fatalf("GET %s did not return an array of %d items", path, len(want))
	}
	for i := range want {
		if got.Items[i] != want[i] {
			t.Fatalf("GET %s returned unexpected item %v", path, got.Items[i])
		}
	}
}
