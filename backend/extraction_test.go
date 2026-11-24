package main

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"testing"
)

type transportFunc func(*http.Request) (*http.Response, error)

func (f transportFunc) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }

func TestProviderPipelineMinimizesOCRText(t *testing.T) {
	imageBytes := []byte("synthetic-provider-fixture")
	baiduCalls, deepSeekCalls := 0, 0
	e := providerExtractor{
		baiduKey: "test-id", baiduSecret: "test-secret", deepSeekKey: "test-deepseek", model: "deepseek-flash",
		client: &http.Client{Transport: transportFunc(func(r *http.Request) (*http.Response, error) {
			switch r.URL.Host + r.URL.Path {
			case "aip.baidubce.com/oauth/2.0/token":
				baiduCalls++
				query := r.URL.Query()
				if query.Get("grant_type") != "client_credentials" ||
					query.Get("client_id") != "test-id" || query.Get("client_secret") != "test-secret" {
					t.Fatal("token exchange did not send expected query parameters")
				}
				return providerResponse(`{"access_token":"test-token","expires_in":2592000}`), nil
			case "aip.baidubce.com/rest/2.0/ocr/v1/general_basic":
				if err := r.ParseForm(); err != nil || r.Form.Get("image") != "c3ludGhldGljLXByb3ZpZGVyLWZpeHR1cmU=" ||
					r.URL.Query().Get("access_token") != "test-token" {
					t.Fatal("Baidu request did not send expected data")
				}
				return providerResponse(`{"words_result":[
					{"words":"Livin' by Mandiri"},{"words":"Transfer Berhasil"},{"words":"Rp125.000,00"},
					{"words":"22 Nov 2026, 12:34 WIB"},{"words":"Dari"},{"words":"Pembayar Contoh"},
					{"words":"No. Referensi: DEMO-001"},{"words":"Biaya Rp2.500"},
					{"words":"Rekening Tujuan 000111222333"},{"words":"pengguna@example.test"}
				]}`), nil
			case "api.deepseek.com/chat/completions":
				deepSeekCalls++
				if r.Header.Get("Authorization") != "Bearer test-deepseek" {
					t.Fatal("DeepSeek request missing bearer header")
				}
				var request struct {
					Messages       []providerMessage `json:"messages"`
					ResponseFormat map[string]string `json:"response_format"`
				}
				if err := json.NewDecoder(r.Body).Decode(&request); err != nil || len(request.Messages) != 2 ||
					request.ResponseFormat["type"] != "json_object" {
					t.Fatal("DeepSeek request did not ask for JSON-only output")
				}
				userMessage := request.Messages[1].Content
				for _, word := range []string{"125.000", "22 Nov 2026", "Pembayar Contoh", "DEMO-001"} {
					if !strings.Contains(userMessage, word) {
						t.Errorf("DeepSeek did not receive relevant fixture field %q", word)
					}
				}
				for _, word := range []string{"000111222333", "2.500", "pengguna@example.test", string(imageBytes), "WIB"} {
					if strings.Contains(userMessage, word) {
						t.Errorf("DeepSeek received unrelated fixture data %q", word)
					}
				}
				return providerResponse(`{"choices":[{"finish_reason":"stop","message":{"content":"{\"amount\":125000,\"paidOn\":\"2026-11-22\",\"payerName\":\"Pembayar Contoh\",\"reference\":\"DEMO-001\",\"note\":\"ignored\"}"}}]}`), nil
			default:
				t.Fatalf("unexpected host and path %s", r.URL.Host+r.URL.Path)
				return nil, nil
			}
		})},
	}
	for i := 0; i < 2; i++ {
		fields, err := e.extract(context.Background(), imageBytes)
		if err != nil || fields.Amount != 125000 || fields.PaidOn != "2026-11-22" ||
			fields.PayerName != "Pembayar Contoh" || fields.Reference != "DEMO-001" || fields.Note != "" {
			t.Fatalf("extraction got fields %+v and error %v", fields, err)
		}
	}
	if baiduCalls != 1 || deepSeekCalls != 2 {
		t.Fatalf("expected one Baidu token request and two DeepSeek requests, got %d and %d", baiduCalls, deepSeekCalls)
	}
}

func TestUnsupportedReceipt(t *testing.T) {
	for _, lines := range [][]string{
		{"Bank Contoh", "Transfer Berhasil", "Rp125.000"},
		{"Livin' by Mandiri", "Transfer Gagal", "Rp125.000"},
		{"Livin' by Mandiri", "Transfer Unsuccessful", "Rp125.000"},
		{"Livin' by Mandiri", "Transfer Tidak Sukses", "Rp125.000"},
		{"Livin' by Mandiri", "Transfer", "Status pending", "Rp125.000"},
		{"Livin' by Mandiri", "Transfer Berhasil", "Transaksi belum berhasil", "Rp125.000"},
		{"Livin' by Mandiri", "Transaksi Berhasil", "Pembayaran QR", "Rp125.000"},
		{"Livin' by Mandiri", "Transaksi Berhasil", "Nominal Transfer: Rp125.000", "Status pending"},
		{"Bank Contoh", "Transaksi Berhasil", "Nominal Transfer: Rp125.000"},
	} {
		if _, err := relevantLines(lines); err != errUnsupportedEvidence {
			t.Fatalf("unsupported fixture got error %v", err)
		}
	}
	for _, status := range []string{"Transfer Berhasil", "Transfer BI-FAST Sukses", "Successful Transfer"} {
		if _, err := relevantLines([]string{"Livin’ by Mandiri", status, "Rp125.000"}); err != nil {
			t.Fatalf("supported status %q got error %v", status, err)
		}
	}
	lines := []string{"Livin’ by Mandiri", "Transaksi Berhasil", "Nominal Transfer: Rp125.000"}
	got, err := relevantLines(lines)
	if err != nil || len(got) != 1 || got[0] != "Nominal: Rp125.000" {
		t.Fatalf("transfer receipt got %q and error %v", got, err)
	}
}

func TestTextEndpointForwardsOnlyRelevantOCR(t *testing.T) {
	calls := 0
	extractor := &providerExtractor{
		deepSeekKey: "test-deepseek", model: "deepseek-flash",
		client: &http.Client{Transport: transportFunc(func(r *http.Request) (*http.Response, error) {
			calls++
			if r.URL.String() != deepSeekURL || r.Header.Get("Authorization") != "Bearer test-deepseek" {
				t.Fatal("text path must only contact DeepSeek")
			}
			var payload struct {
				Messages []providerMessage `json:"messages"`
			}
			var submitted []string
			if err := json.NewDecoder(r.Body).Decode(&payload); err != nil || len(payload.Messages) != 2 ||
				json.Unmarshal([]byte(payload.Messages[1].Content), &submitted) != nil {
				t.Fatal("DeepSeek did not receive selected OCR strings")
			}
			want := []string{"Nominal: Rp125.000", "Tanggal: 2026-11-22", "Pengirim: Pembayar Contoh", "Referensi: DEMO-001"}
			if len(submitted) != len(want) {
				t.Fatalf("DeepSeek received %d strings, expected %d", len(submitted), len(want))
			}
			for i := range want {
				if submitted[i] != want[i] {
					t.Fatalf("unexpected forwarded OCR string at index %d", i)
				}
			}
			return providerResponse(`{"choices":[{"finish_reason":"stop","message":{"content":"{\"amount\":125000,\"paidOn\":\"2026-11-22\",\"payerName\":\"Pembayar Contoh\",\"reference\":\"DEMO-001\"}"}}]}`), nil
		})},
	}
	api := (&app{store: newMemoryStore(), secret: []byte("fixture-secret-for-privacy-test-only"), extractor: extractor}).routes("memory")
	token := registerFixture(t, api, "Usaha A", "owner-a@example.test").AccessToken
	text := supportedOCRText + "\nRekening Tujuan 000111222333\nBiaya Rp2.500\npengguna@example.test"
	request(t, api, "POST", "/v1/drafts/extract-text", token, map[string]string{"text": text, "image": "fixture-bytes"}, 400)
	if calls != 0 {
		t.Fatal("invalid payload reached a provider")
	}
	var draft incomeRecord
	decodeResponse(t, request(t, api, "POST", "/v1/drafts/extract-text", token, map[string]string{"text": text}, 201), &draft)
	if calls != 1 || draft.Status != "draft" || draft.Amount != 125000 || draft.PaidOn != "2026-11-22" ||
		draft.Reference != "DEMO-001" || draft.PayerName != "Pembayar Contoh" {
		t.Fatal("the valid text request did not result in one minimized provider call and a draft")
	}
}

func providerResponse(body string) *http.Response {
	return &http.Response{StatusCode: http.StatusOK, Body: io.NopCloser(strings.NewReader(body))}
}
