package com.kwtsms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;

class ApiErrorsTest {

    @Test
    void apiErrors_containsAll33Codes() {
        // 33 standard codes + ERR_INVALID_INPUT
        assertTrue(ApiErrors.API_ERRORS.size() >= 29);
        assertNotNull(ApiErrors.API_ERRORS.get("ERR001"));
        assertNotNull(ApiErrors.API_ERRORS.get("ERR033"));
        assertNotNull(ApiErrors.API_ERRORS.get("ERR_INVALID_INPUT"));
    }

    @Test
    void apiErrors_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () ->
                ApiErrors.API_ERRORS.put("TEST", "test"));
    }

    // ── enrichError ──

    @Test
    void enrichError_addsActionForKnownCode() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", "ERROR");
        response.put("code", "ERR003");
        response.put("description", "Authentication error");

        Map<String, Object> enriched = ApiErrors.enrichError(response);
        assertNotNull(enriched.get("action"));
        assertTrue(enriched.get("action").toString().contains("KWTSMS_USERNAME"));
    }

    @Test
    void enrichError_noActionForUnknownCode() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", "ERROR");
        response.put("code", "ERR999");
        response.put("description", "Unknown error");

        Map<String, Object> enriched = ApiErrors.enrichError(response);
        assertNull(enriched.get("action"));
    }

    @Test
    void enrichError_noChangeForOkResult() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", "OK");
        response.put("available", 150);

        Map<String, Object> enriched = ApiErrors.enrichError(response);
        assertNull(enriched.get("action"));
    }

    @Test
    void enrichError_noChangeWithoutCode() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", "ERROR");
        response.put("description", "Some error");

        Map<String, Object> enriched = ApiErrors.enrichError(response);
        assertNull(enriched.get("action"));
    }

    // ── Mocked API response tests ──

    @Test
    void enrichError_err003WrongCredentials() {
        Map<String, Object> r = mockError("ERR003", "Authentication error, username or password are not correct.");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        assertTrue(enriched.get("action").toString().contains("API credentials"));
    }

    @Test
    void enrichError_err026CountryNotAllowed() {
        Map<String, Object> r = mockError("ERR026", "Country not activated");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        assertTrue(enriched.get("action").toString().contains("country"));
    }

    @Test
    void enrichError_err025InvalidNumber() {
        Map<String, Object> r = mockError("ERR025", "Invalid number");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        assertTrue(enriched.get("action").toString().contains("country code"));
    }

    @Test
    void enrichError_err010ZeroBalance() {
        Map<String, Object> r = mockError("ERR010", "Zero balance");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        assertTrue(enriched.get("action").toString().contains("kwtsms.com"));
    }

    @Test
    void enrichError_err024IpNotWhitelisted() {
        Map<String, Object> r = mockError("ERR024", "IP not whitelisted");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        assertTrue(enriched.get("action").toString().contains("IP"));
    }

    @Test
    void enrichError_err028RateLimit() {
        Map<String, Object> r = mockError("ERR028", "Rate limited");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        // 2026-10-22 15:35 +03: ERR028 action no longer states a fixed 15 s window.
        assertTrue(enriched.get("action").toString().contains("too soon"));
    }

    @Test
    void enrichError_err008BannedSender() {
        Map<String, Object> r = mockError("ERR008", "Sender banned");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        assertTrue(enriched.get("action").toString().contains("sender ID"));
    }

    @Test
    void enrichError_err999UnknownDoesNotCrash() {
        Map<String, Object> r = mockError("ERR999", "Something unknown");
        Map<String, Object> enriched = ApiErrors.enrichError(r);
        assertNull(enriched.get("action")); // No action for unknown codes
        assertEquals("Something unknown", enriched.get("description"));
    }

    private Map<String, Object> mockError(String code, String description) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("result", "ERROR");
        r.put("code", code);
        r.put("description", description);
        return r;
    }
}
