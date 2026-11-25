package main

import (
	"bytes"
	"encoding/json"
	"image"
	"image/png"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

const supportedOCRText = "Livin’ by Mandiri\nTransfer Berhasil\nNominal: Rp125.000\nTanggal: 2026-11-22\nPengirim: Pembayar Contoh\nReferensi: DEMO-001"

func TestIncomeResourcesAPI(t *testing.T) {
	t.Run("memory", func(t *testing.T) { testIncomeResources(t, newMemoryStore(), "memory") })
	t.Run("postgres", func(t *testing.T) { testIncomeResources(t, testPostgres(t), "postgres") })
}

func testIncomeResources(t *testing.T, store storage, storageMode string) {
	t.Helper()
	a := &app{store: store, secret: []byte("fixture-secret-for-income-test-only"), extractor: demoExtractor{}}
	api := a.routes(storageMode)
	owner := registerFixture(t, api, "Usaha A", "owner-a@example.test").AccessToken
	other := registerFixture(t, api, "Usaha B", "owner-b@example.test").AccessToken
	cat := createNamedFixture(t, api, "/v1/categories", owner, "Pesanan November")
	payer := createNamedFixture(t, api, "/v1/payers", owner, "Pembayar A")
	foreignCat := createNamedFixture(t, api, "/v1/categories", other, "Kategori Asing")

	const date = "2026-11-22"
	const recapPath = "/v1/recap?from=" + date + "&to=" + date
	request(t, api, "GET", "/v1/recap", owner, nil, 400)
	request(t, api, "GET", "/v1/recap?from=2026-02-30&to=2026-03-02", owner, nil, 400)
	request(t, api, "GET", "/v1/recap?from=2026-12-01&to=2026-11-22", owner, nil, 400)
	request(t, api, "POST", "/v1/transactions", owner, incomeFields{Amount: 12000, PaidOn: date}, 405)
	assertRecap(t, api, recapPath, owner, 0, 0)

	var draft, imageDraft incomeRecord
	decodeResponse(t, request(t, api, "POST", "/v1/drafts/extract-text", owner,
		map[string]string{"text": supportedOCRText}, 201), &draft)
	decodeResponse(t, uploadPNGFixture(t, api, owner, "/v1/drafts/extract", 32, 32), &imageDraft)
	if !validRecordID(draft.ID) || !validRecordID(imageDraft.ID) || draft.ID == imageDraft.ID ||
		draft.Amount != 125000 || draft.Status != "draft" || draft.Source != sourceLivin || imageDraft.Status != "draft" {
		t.Fatal("the extraction routes did not create distinct draft records")
	}
	imagePath := "/v1/drafts/" + imageDraft.ID
	request(t, api, "GET", imagePath, owner, nil, 200)
	request(t, api, "DELETE", imagePath, owner, nil, 204)
	request(t, api, "GET", imagePath, owner, nil, 404)
	assertRecordCount(t, api, "/v1/drafts?q=DRAFT", owner, 1)
	assertRecordCount(t, api, "/v1/drafts?q=DEMO-001", owner, 1)
	assertRecordCount(t, api, "/v1/drafts", other, 0)
	assertRecap(t, api, recapPath, owner, 0, 0)

	path := "/v1/drafts/" + draft.ID
	fields := incomeFields{Amount: 32000, PaidOn: date, Reference: "UJI-01",
		PayerName: "Pembayar A", CategoryID: cat.ID, PayerID: payer.ID, Source: "Sumber palsu"}
	request(t, api, "GET", path, other, nil, 404)
	request(t, api, "PUT", path, other, fields, 404)
	request(t, api, "DELETE", path, other, nil, 404)
	request(t, api, "POST", path+"/confirm", other, fields, 404)
	invalid := fields
	invalid.CategoryID = foreignCat.ID
	request(t, api, "PUT", path, owner, invalid, 400)
	request(t, api, "POST", path+"/confirm", owner, invalid, 400)
	invalid = fields
	invalid.Amount = 0
	request(t, api, "POST", path+"/confirm", owner, invalid, 400)
	request(t, api, "PUT", path, owner, fields, 200)
	assertRecap(t, api, recapPath, owner, 0, 0)

	id := confirmConcurrently(t, api, path+"/confirm", owner, fields)
	incomePath := "/v1/transactions/" + id
	request(t, api, "PUT", path, owner, fields, 409)
	assertRecordCount(t, api, "/v1/drafts?q=CONVERTED", owner, 1)
	assertRecordCount(t, api, "/v1/transactions?q=UJI-01&from="+date+"&to="+date, owner, 1)
	assertRecordCount(t, api, "/v1/transactions?q=NOVEMBER", owner, 1)
	assertRecordCount(t, api, "/v1/transactions?from=2026-11-23", owner, 0)
	assertRecordCount(t, api, "/v1/transactions", other, 0)
	assertRecap(t, api, recapPath, owner, 32000, 1)
	assertRecap(t, api, recapPath, other, 0, 0)

	request(t, api, "GET", incomePath, other, nil, 404)
	request(t, api, "PUT", incomePath, other, fields, 404)
	request(t, api, "DELETE", incomePath, other, nil, 404)
	request(t, api, "DELETE", "/v1/categories/"+cat.ID, owner, nil, 409)
	request(t, api, "DELETE", "/v1/payers/"+payer.ID, owner, nil, 409)
	fields.Amount = 49000
	var changed incomeRecord
	decodeResponse(t, request(t, api, "PUT", incomePath, owner, fields, 200), &changed)
	if changed.Source != sourceLivin || changed.Amount != 49000 {
		t.Fatal("edit did not preserve the source and update the amount")
	}
	request(t, api, "DELETE", path, owner, nil, 204)
	request(t, api, "GET", incomePath, owner, nil, 200)
	assertRecap(t, api, recapPath, owner, 49000, 1)
	request(t, api, "DELETE", incomePath, owner, nil, 204)
	request(t, api, "DELETE", "/v1/categories/"+cat.ID, owner, nil, 204)
	request(t, api, "DELETE", "/v1/payers/"+payer.ID, owner, nil, 204)
	assertRecap(t, api, recapPath, owner, 0, 0)

	testSavedRecapCRUDS(t, api, owner, other)
}

func testSavedRecapCRUDS(t *testing.T, api http.Handler, owner, other string) {
	t.Helper()
	const collection = "/v1/saved-recaps"
	cfg := recapRecord{Name: "November Lama", Period: "month", From: "2026-11-01", To: "2026-11-30"}
	var saved recapRecord
	decodeResponse(t, request(t, api, "POST", collection, owner, cfg, 201), &saved)
	if !validRecordID(saved.ID) {
		t.Fatal("saved recap has no server ID")
	}
	path := collection + "/" + saved.ID
	request(t, api, "GET", path, owner, nil, 200)
	request(t, api, "GET", path, other, nil, 404)
	request(t, api, "PUT", path, other, cfg, 404)
	request(t, api, "DELETE", path, other, nil, 404)
	assertRecordCount(t, api, collection+"?q=NOVEMBER", owner, 1)
	assertRecordCount(t, api, collection+"?q=month", owner, 1)
	assertRecordCount(t, api, collection+"?q=NOVEMBER", other, 0)
	cfg.Name, cfg.Period, cfg.From, cfg.To = "Rekap Baru", "custom", "2026-11-22", "2026-11-22"
	request(t, api, "PUT", path, owner, cfg, 200)
	assertRecordCount(t, api, collection+"?q=LAMA", owner, 0)
	assertRecordCount(t, api, collection+"?q=BARU", owner, 1)
	request(t, api, "DELETE", path, owner, nil, 204)
	assertRecordCount(t, api, collection, owner, 0)
}

func createNamedFixture(t *testing.T, api http.Handler, path, token, name string) namedRecord {
	t.Helper()
	var item namedRecord
	decodeResponse(t, request(t, api, "POST", path, token, namedRecord{Name: name}, 201), &item)
	return item
}

func assertRecordCount(t *testing.T, api http.Handler, path, token string, count int) {
	t.Helper()
	var response recordList
	decodeResponse(t, request(t, api, "GET", path, token, nil, 200), &response)
	if response.Items == nil || len(response.Items) != count {
		t.Fatalf("GET %s did not return an array of %d items", path, count)
	}
}

func assertRecap(t *testing.T, api http.Handler, path, token string, amount int64, count int) {
	t.Helper()
	var response recapResponse
	decodeResponse(t, request(t, api, "GET", path, token, nil, 200), &response)
	if response.Total != amount || response.Count != count || response.Items == nil || len(response.Items) != count {
		t.Fatalf("recap returned total %d, count %d and %d items; expected %d and %d records",
			response.Total, response.Count, len(response.Items), amount, count)
	}
}

func confirmConcurrently(t *testing.T, api http.Handler, path, token string, fields incomeFields) string {
	t.Helper()
	payload, err := json.Marshal(fields)
	if err != nil {
		t.Fatal(err)
	}
	const attempts = 4
	results := make(chan *httptest.ResponseRecorder, attempts)
	for range attempts {
		go func() { results <- serveFixture(api, "POST", path, token, payload, "application/json") }()
	}
	id, created := "", 0
	for range attempts {
		res := <-results
		switch res.Code {
		case http.StatusCreated:
			created++
		case http.StatusOK:
		default:
			t.Fatalf("unexpected confirmation status %d", res.Code)
		}
		var item incomeRecord
		decodeResponse(t, res, &item)
		if !validRecordID(item.ID) || (id != "" && item.ID != id) || item.Amount != fields.Amount || item.Source != sourceLivin {
			t.Fatal("retry responses did not identify the same confirmed income")
		}
		id = item.ID
	}
	if created != 1 {
		t.Fatalf("confirmation created %d items, expected one", created)
	}
	return id
}

func uploadPNGFixture(t *testing.T, api http.Handler, token, path string, width, height int) *httptest.ResponseRecorder {
	t.Helper()
	var payload bytes.Buffer
	form := multipart.NewWriter(&payload)
	part, err := form.CreateFormFile("image", "fixture.png")
	if err != nil {
		t.Fatal(err)
	}
	if err := png.Encode(part, image.NewRGBA(image.Rect(0, 0, width, height))); err != nil {
		t.Fatal(err)
	}
	if err := form.Close(); err != nil {
		t.Fatal(err)
	}
	response := serveFixture(api, "POST", path, token, payload.Bytes(), form.FormDataContentType())
	if path == "/v1/drafts/extract" && response.Code != http.StatusCreated {
		t.Fatalf("image draft returned %d, expected 201", response.Code)
	}
	return response
}

func serveFixture(api http.Handler, method, path, token string, data []byte, contentType string) *httptest.ResponseRecorder {
	request := httptest.NewRequest(method, path, bytes.NewReader(data))
	request.Header.Set("Content-Type", contentType)
	request.Header.Set("Authorization", "Bearer "+token)
	response := httptest.NewRecorder()
	api.ServeHTTP(response, request)
	return response
}

func TestTextExtractionRejectsUnsupportedPayload(t *testing.T) {
	api := (&app{store: newMemoryStore(), secret: []byte("fixture-secret-for-income-test-only"), extractor: demoExtractor{}}).routes("memory")
	token := registerFixture(t, api, "Usaha A", "owner-a@example.test").AccessToken
	const path = "/v1/drafts/extract-text"
	request(t, api, "POST", path, "", map[string]string{"text": supportedOCRText}, 401)
	request(t, api, "POST", path, token, map[string]string{"text": supportedOCRText, "image": "fixture"}, 400)
	request(t, api, "POST", path, token, map[string]string{"text": "   \n  "}, 400)
	request(t, api, "POST", path, token, map[string]string{"text": strings.Repeat("a", maxOCRTextBytes+1)}, 413)
	request(t, api, "POST", path, token, map[string]string{"text": "Livin' by Mandiri\nTransfer Gagal"}, 422)
	request(t, api, "POST", path, token, map[string]string{"text": "Livin' by Mandiri\nTransfer Tidak Sukses\nRp125.000"}, 422)
	request(t, api, "POST", path, token, map[string]string{"text": "Livin' by Mandiri\nTransfer Unsuccessful\nRp125.000"}, 422)
	response := uploadPNGFixture(t, api, token, path, 32, 32)
	if response.Code != http.StatusUnsupportedMediaType {
		t.Fatalf("multipart to text endpoint returned %d, expected 415", response.Code)
	}
	assertRecordCount(t, api, "/v1/drafts", token, 0)
}
