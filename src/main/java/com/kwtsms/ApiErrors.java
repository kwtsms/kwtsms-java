package com.kwtsms;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Complete mapping of kwtSMS error codes to developer-friendly action messages.
 */
public final class ApiErrors {

    private ApiErrors() {}

    /** Read-only map of all kwtSMS error codes to action messages. */
    public static final Map<String, String> API_ERRORS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ERR001", "API is disabled on this account. Enable it at kwtsms.com \u2192 Account \u2192 API.");
        m.put("ERR002", "A required parameter is missing. Check that username, password, sender, mobile, and message are all provided.");
        m.put("ERR003", "Wrong API username or password. Check KWTSMS_USERNAME and KWTSMS_PASSWORD. These are your API credentials, not your account mobile number.");
        m.put("ERR004", "This account does not have API access. Contact kwtSMS support to enable it.");
        m.put("ERR005", "This account is blocked. Contact kwtSMS support.");
        // 2026-11-05 09:01 +03: ERR006/ERR025 sample is now the 965XXXXXXXX placeholder, the old one belongs to a real person. Task: int-testnumber
        m.put("ERR006", "No valid phone numbers. Make sure each number includes the country code (e.g., 965XXXXXXXX for Kuwait, not XXXXXXXX).");
        m.put("ERR007", "Too many numbers in a single request (maximum 200). Split into smaller batches.");
        m.put("ERR008", "This sender ID is banned. Use a different sender ID registered on your kwtSMS account.");
        m.put("ERR009", "Message is empty. Provide a non-empty message text.");
        m.put("ERR010", "Account balance is zero. Recharge credits at kwtsms.com.");
        m.put("ERR011", "Insufficient balance for this send. Buy more credits at kwtsms.com.");
        m.put("ERR012", "Message is too long (over 6 SMS pages). Shorten your message.");
        m.put("ERR013", "Send queue is full (1000 messages). Wait a moment and try again.");
        m.put("ERR019", "No delivery reports found for this message.");
        m.put("ERR020", "Message ID does not exist. Make sure you saved the msg-id from the send response.");
        m.put("ERR021", "No delivery report available for this message yet.");
        m.put("ERR022", "Delivery reports are not ready yet. Try again after 24 hours.");
        m.put("ERR023", "Unknown delivery report error. Contact kwtSMS support.");
        m.put("ERR024", "Your IP address is not in the API whitelist. Add it at kwtsms.com \u2192 Account \u2192 API \u2192 IP Lockdown, or disable IP lockdown.");
        m.put("ERR025", "Invalid phone number. Make sure the number includes the country code (e.g., 965XXXXXXXX for Kuwait, not XXXXXXXX).");
        m.put("ERR026", "This country is not activated on your account. Contact kwtSMS support to enable the destination country.");
        m.put("ERR027", "HTML tags are not allowed in the message. Remove any HTML content and try again.");
        // 2026-10-22 15:35 +03: ERR028 text corrected, protection is opt-in per account with a per-account window (10 s default), not a fixed 15 s.
        m.put("ERR028", "Sent to the same number too soon. Duplicate SMS protection is on for this account, so the whole request was rejected and nothing was charged. Wait for the account's window (10 seconds by default) before resending.");
        m.put("ERR029", "Message ID does not exist or is incorrect.");
        m.put("ERR030", "Message is stuck in the send queue with an error. Delete it at kwtsms.com \u2192 Queue to recover credits.");
        m.put("ERR031", "Message rejected: bad language detected.");
        m.put("ERR032", "Message rejected: spam detected.");
        m.put("ERR033", "No active coverage found. Contact kwtSMS support.");
        m.put("ERR_INVALID_INPUT", "One or more phone numbers are invalid. See details above.");
        API_ERRORS = Collections.unmodifiableMap(m);
    }

    /**
     * Enrich an API error response with a developer-friendly action message.
     * If the response contains result=ERROR and a known error code,
     * an "action" field is added with guidance.
     *
     * @param response the raw API response map
     * @return a new map with the "action" field added, or the original map if not applicable
     */
    public static Map<String, Object> enrichError(Map<String, Object> response) {
        Object result = response.get("result");
        if (!"ERROR".equals(result == null ? null : result.toString())) {
            return response;
        }
        Object codeObj = response.get("code");
        if (codeObj == null) {
            return response;
        }
        String code = codeObj.toString();
        String action = API_ERRORS.get(code);
        if (action == null) {
            return response;
        }
        Map<String, Object> enriched = new LinkedHashMap<>(response);
        enriched.put("action", action);
        return enriched;
    }
}
