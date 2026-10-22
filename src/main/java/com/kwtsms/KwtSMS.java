package com.kwtsms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Official Java client for the kwtSMS SMS gateway API.
 *
 * Thread-safe: this class can be shared across threads.
 * All methods are blocking (synchronous).
 *
 * <pre>{@code
 * // Constructor
 * KwtSMS sms = new KwtSMS("username", "password");
 *
 * // Or from environment variables / .env file
 * KwtSMS sms = KwtSMS.fromEnv();
 *
 * // Verify credentials
 * VerifyResult v = sms.verify();
 * System.out.println(v.isOk() + " balance=" + v.getBalance());
 *
 * // Send SMS
 * SendResult r = sms.send("96598765432", "Hello from Java");
 * System.out.println(r.getResult());
 * }</pre>
 */
public class KwtSMS {

    private final String username;
    private final String password;
    private final String senderId;
    private final boolean testMode;
    private final String logFile;

    private volatile Double cachedBalance;
    private volatile Double cachedPurchased;

    private static final int MAX_BATCH_SIZE = 200;
    private static final long BATCH_DELAY_MS = 500L;
    private static final long[] ERR013_RETRY_DELAYS = {30_000L, 60_000L, 120_000L};

    /**
     * Create a KwtSMS client.
     *
     * @param username API username (not your account mobile number)
     * @param password API password
     * @param senderId sender ID (default: "KWT-SMS" for testing only)
     * @param testMode when true, messages enter the queue but are not delivered; their credits are held
     *                 until you delete them from the Sending Queue in your kwtsms.com account
     *                 (2026-10-22 15:35 +03: corrected, test sends are not free)
     * @param logFile  path to JSONL log file (null or empty to disable logging)
     */
    public KwtSMS(String username, String password, String senderId, boolean testMode, String logFile) {
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
        this.senderId = senderId != null ? senderId : "KWT-SMS";
        this.testMode = testMode;
        this.logFile = logFile != null ? logFile : "kwtsms.log";
    }

    public KwtSMS(String username, String password, String senderId, boolean testMode) {
        this(username, password, senderId, testMode, "kwtsms.log");
    }

    public KwtSMS(String username, String password, String senderId) {
        this(username, password, senderId, false, "kwtsms.log");
    }

    public KwtSMS(String username, String password) {
        this(username, password, "KWT-SMS", false, "kwtsms.log");
    }

    /**
     * Create a KwtSMS client from environment variables and/or .env file.
     *
     * Reads: KWTSMS_USERNAME, KWTSMS_PASSWORD, KWTSMS_SENDER_ID, KWTSMS_TEST_MODE, KWTSMS_LOG_FILE.
     * Environment variables take precedence over .env file values.
     *
     * @param envFile path to .env file (default: ".env")
     * @return a configured KwtSMS client
     */
    public static KwtSMS fromEnv(String envFile) {
        Map<String, String> fileVars = EnvLoader.loadEnvFile(envFile);

        String username = resolve("KWTSMS_USERNAME", "", fileVars);
        String password = resolve("KWTSMS_PASSWORD", "", fileVars);
        String senderId = resolve("KWTSMS_SENDER_ID", "KWT-SMS", fileVars);
        boolean testMode = "1".equals(resolve("KWTSMS_TEST_MODE", "0", fileVars));
        String logFile = resolve("KWTSMS_LOG_FILE", "kwtsms.log", fileVars);

        return new KwtSMS(username, password, senderId, testMode, logFile);
    }

    public static KwtSMS fromEnv() {
        return fromEnv(".env");
    }

    private static String resolve(String key, String defaultValue, Map<String, String> fileVars) {
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal;
        }
        String fileVal = fileVars.get(key);
        if (fileVal != null && !fileVal.trim().isEmpty()) {
            return fileVal;
        }
        return defaultValue;
    }

    /** Cached available balance from the last verify() or successful send() call. */
    public Double getCachedBalance() { return cachedBalance; }

    /** Cached total purchased credits from the last verify() call. */
    public Double getCachedPurchased() { return cachedPurchased; }

    private Map<String, Object> basePayload() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("username", username);
        map.put("password", password);
        return map;
    }

    // ──────────────────────────────────────────────
    // verify()
    // ──────────────────────────────────────────────

    /**
     * Test credentials and get balance. Never throws.
     */
    public VerifyResult verify() {
        try {
            Map<String, Object> response = ApiRequest.post("balance", basePayload(), logFile);
            String result = objToString(response.get("result"));

            if ("OK".equals(result)) {
                Double available = objToDouble(response.get("available"));
                Double purchased = objToDouble(response.get("purchased"));
                cachedBalance = available;
                cachedPurchased = purchased;
                return new VerifyResult(true, available, null);
            } else {
                Map<String, Object> enriched = ApiErrors.enrichError(response);
                String desc = objToString(enriched.get("description"));
                String action = objToString(enriched.get("action"));
                String errorMsg = action != null ? desc + " " + action : desc;
                return new VerifyResult(false, null, errorMsg != null ? errorMsg : "Unknown error");
            }
        } catch (Exception e) {
            return new VerifyResult(false, null, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    // ──────────────────────────────────────────────
    // balance()
    // ──────────────────────────────────────────────

    /**
     * Get current SMS credit balance.
     * Returns the live balance on success, or the cached value if the API call fails.
     * Returns null if no cached value exists and the API call fails.
     */
    public Double balance() {
        try {
            Map<String, Object> response = ApiRequest.post("balance", basePayload(), logFile);
            if ("OK".equals(objToString(response.get("result")))) {
                Double available = objToDouble(response.get("available"));
                Double purchased = objToDouble(response.get("purchased"));
                cachedBalance = available;
                cachedPurchased = purchased;
                return available;
            }
            return cachedBalance;
        } catch (Exception e) {
            return cachedBalance;
        }
    }

    // ──────────────────────────────────────────────
    // send()
    // ──────────────────────────────────────────────

    /**
     * Send an SMS to one or more phone numbers (comma-separated string).
     */
    public SendResult send(String mobile, String message) {
        return send(mobile, message, null);
    }

    /**
     * Send an SMS to one or more phone numbers (comma-separated string).
     *
     * @param mobile  comma-separated phone numbers
     * @param message message text (cleaned automatically)
     * @param sender  sender ID override (null to use default)
     * @return send result
     */
    public SendResult send(String mobile, String message, String sender) {
        String[] parts = (mobile != null ? mobile : "").split(",");
        List<String> phones = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                phones.add(trimmed);
            }
        }
        return sendToPhones(phones, message, sender);
    }

    /**
     * Send an SMS to a list of phone numbers.
     */
    public SendResult send(List<String> mobiles, String message) {
        return sendToPhones(mobiles, message, null);
    }

    /**
     * Send an SMS to a list of phone numbers.
     */
    public SendResult send(List<String> mobiles, String message, String sender) {
        return sendToPhones(mobiles, message, sender);
    }

    private SendResult sendToPhones(List<String> phones, String message, String sender) {
        List<String> validPhones = new ArrayList<>();
        List<InvalidEntry> invalidEntries = new ArrayList<>();

        for (String phone : phones) {
            PhoneUtils.ValidationResult vr = PhoneUtils.validatePhoneInput(String.valueOf(phone));
            if (vr.isValid()) {
                validPhones.add(vr.getNormalized());
            } else {
                invalidEntries.add(new InvalidEntry(phone, vr.getError() != null ? vr.getError() : "Invalid phone number"));
            }
        }

        // Deduplicate valid numbers
        List<String> deduplicated = PhoneUtils.deduplicatePhones(validPhones);

        // If no valid numbers remain, return error
        if (deduplicated.isEmpty()) {
            return new SendResult("ERROR", null, null, null, null, null,
                    "ERR_INVALID_INPUT", "No valid phone numbers provided",
                    ApiErrors.API_ERRORS.get("ERR_INVALID_INPUT"), invalidEntries);
        }

        // Clean the message
        String cleanedMessage = MessageUtils.cleanMessage(message != null ? message : "");
        if (cleanedMessage.trim().isEmpty()) {
            return new SendResult("ERROR", null, null, null, null, null,
                    "ERR009", "Message is empty after cleaning",
                    ApiErrors.API_ERRORS.get("ERR009"), invalidEntries);
        }

        // If >200 numbers, use bulk send
        if (deduplicated.size() > MAX_BATCH_SIZE) {
            BulkSendResult bulkResult = sendBulkInternal(deduplicated, cleanedMessage, sender, invalidEntries);
            return new SendResult(bulkResult.getResult(),
                    bulkResult.getMsgIds().isEmpty() ? null : bulkResult.getMsgIds().get(0),
                    bulkResult.getNumbers(), bulkResult.getPointsCharged(),
                    bulkResult.getBalanceAfter(), null,
                    bulkResult.getCode(), bulkResult.getDescription(), null, bulkResult.getInvalid());
        }

        // Single batch send
        return sendSingleBatch(deduplicated, cleanedMessage, sender, invalidEntries);
    }

    private SendResult sendSingleBatch(List<String> phones, String cleanedMessage,
                                        String sender, List<InvalidEntry> invalidEntries) {
        Map<String, Object> payload = basePayload();
        payload.put("sender", sender != null ? sender : senderId);
        payload.put("mobile", join(phones, ","));
        payload.put("message", cleanedMessage);
        payload.put("test", testMode ? "1" : "0");

        try {
            Map<String, Object> response = ApiRequest.post("send", payload, logFile);
            String result = objToString(response.get("result"));

            if ("OK".equals(result)) {
                Double balanceAfter = objToDouble(response.get("balance-after"));
                if (balanceAfter != null) {
                    cachedBalance = balanceAfter;
                }
                return new SendResult("OK",
                        objToString(response.get("msg-id")),
                        objToInt(response.get("numbers")),
                        objToInt(response.get("points-charged")),
                        balanceAfter,
                        objToLong(response.get("unix-timestamp")),
                        null, null, null, invalidEntries);
            } else {
                Map<String, Object> enriched = ApiErrors.enrichError(response);
                return new SendResult("ERROR", null, null, null, null, null,
                        objToString(enriched.get("code")),
                        objToString(enriched.get("description")),
                        objToString(enriched.get("action")),
                        invalidEntries);
            }
        } catch (Exception e) {
            return new SendResult("ERROR", null, null, null, null, null,
                    null, e.getMessage() != null ? e.getMessage() : "Unknown error",
                    null, invalidEntries);
        }
    }

    // ──────────────────────────────────────────────
    // sendBulk()
    // ──────────────────────────────────────────────

    /**
     * Send SMS to a large list of phone numbers with automatic batching.
     * Numbers are split into batches of 200, with 0.5s delay between batches.
     * ERR013 (queue full) is retried up to 3 times with exponential backoff.
     */
    public BulkSendResult sendBulk(List<String> mobiles, String message) {
        return sendBulk(mobiles, message, null);
    }

    public BulkSendResult sendBulk(List<String> mobiles, String message, String sender) {
        List<String> validPhones = new ArrayList<>();
        List<InvalidEntry> invalidEntries = new ArrayList<>();

        for (String phone : mobiles) {
            PhoneUtils.ValidationResult vr = PhoneUtils.validatePhoneInput(String.valueOf(phone));
            if (vr.isValid()) {
                validPhones.add(vr.getNormalized());
            } else {
                invalidEntries.add(new InvalidEntry(phone, vr.getError() != null ? vr.getError() : "Invalid phone number"));
            }
        }

        List<String> deduplicated = PhoneUtils.deduplicatePhones(validPhones);

        if (deduplicated.isEmpty()) {
            return new BulkSendResult("ERROR", true, 0, 0, 0, null,
                    null, null, invalidEntries, "ERR_INVALID_INPUT", "No valid phone numbers provided");
        }

        String cleanedMessage = MessageUtils.cleanMessage(message != null ? message : "");
        if (cleanedMessage.trim().isEmpty()) {
            return new BulkSendResult("ERROR", true, 0, 0, 0, null,
                    null, null, invalidEntries, "ERR009", "Message is empty after cleaning");
        }

        return sendBulkInternal(deduplicated, cleanedMessage, sender, invalidEntries);
    }

    private BulkSendResult sendBulkInternal(List<String> phones, String cleanedMessage,
                                             String sender, List<InvalidEntry> invalidEntries) {
        List<List<String>> batches = partition(phones, MAX_BATCH_SIZE);
        List<String> msgIds = new ArrayList<>();
        List<BatchError> errors = new ArrayList<>();
        int totalNumbers = 0;
        int totalPoints = 0;
        Double lastBalance = null;

        for (int index = 0; index < batches.size(); index++) {
            if (index > 0) {
                try { Thread.sleep(BATCH_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }

            SendResult result = sendBatchWithRetry(batches.get(index), cleanedMessage, sender, index + 1);

            if ("OK".equals(result.getResult())) {
                if (result.getMsgId() != null) msgIds.add(result.getMsgId());
                if (result.getNumbers() != null) totalNumbers += result.getNumbers();
                if (result.getPointsCharged() != null) totalPoints += result.getPointsCharged();
                if (result.getBalanceAfter() != null) lastBalance = result.getBalanceAfter();
            } else {
                errors.add(new BatchError(index + 1,
                        result.getCode() != null ? result.getCode() : "UNKNOWN",
                        result.getDescription() != null ? result.getDescription() : "Unknown error"));
            }
        }

        String overallResult;
        if (errors.isEmpty()) {
            overallResult = "OK";
        } else if (errors.size() == batches.size()) {
            overallResult = "ERROR";
        } else {
            overallResult = "PARTIAL";
        }

        return new BulkSendResult(overallResult, true, batches.size(), totalNumbers,
                totalPoints, lastBalance, msgIds, errors, invalidEntries, null, null);
    }

    private SendResult sendBatchWithRetry(List<String> phones, String cleanedMessage,
                                           String sender, int batchNumber) {
        SendResult lastResult = null;

        for (int attempt = 0; attempt <= ERR013_RETRY_DELAYS.length; attempt++) {
            SendResult result = sendSingleBatch(phones, cleanedMessage, sender, Collections.emptyList());
            lastResult = result;

            if (!"ERR013".equals(result.getCode()) || attempt >= ERR013_RETRY_DELAYS.length) {
                return result;
            }

            try { Thread.sleep(ERR013_RETRY_DELAYS[attempt]); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return result; }
        }

        return lastResult != null ? lastResult : new SendResult("ERROR", null, null, null, null, null,
                null, "Retry exhausted for batch " + batchNumber, null, Collections.emptyList());
    }

    // ──────────────────────────────────────────────
    // validate()
    // ──────────────────────────────────────────────

    /**
     * Validate phone numbers with the kwtSMS API.
     * Runs local validation first, then sends valid numbers to the API.
     */
    public ValidateResult validate(List<String> phones) {
        List<String> validPhones = new ArrayList<>();
        List<InvalidEntry> rejected = new ArrayList<>();

        for (String phone : phones) {
            PhoneUtils.ValidationResult vr = PhoneUtils.validatePhoneInput(String.valueOf(phone));
            if (vr.isValid()) {
                validPhones.add(vr.getNormalized());
            } else {
                rejected.add(new InvalidEntry(phone, vr.getError() != null ? vr.getError() : "Invalid phone number"));
            }
        }

        List<String> deduplicated = PhoneUtils.deduplicatePhones(validPhones);

        if (deduplicated.isEmpty()) {
            return new ValidateResult(null, null, null, rejected, "No valid phone numbers to validate", null);
        }

        try {
            Map<String, Object> payload = basePayload();
            payload.put("mobile", join(deduplicated, ","));
            Map<String, Object> response = ApiRequest.post("validate", payload, logFile);
            String result = objToString(response.get("result"));

            if ("OK".equals(result)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mobile = (Map<String, Object>) response.get("mobile");
                if (mobile == null) mobile = Collections.emptyMap();
                return new ValidateResult(
                        toStringList(mobile.get("OK")),
                        toStringList(mobile.get("ER")),
                        toStringList(mobile.get("NR")),
                        rejected, null, response);
            } else {
                Map<String, Object> enriched = ApiErrors.enrichError(response);
                return new ValidateResult(null, null, null, rejected,
                        objToString(enriched.get("description")), response);
            }
        } catch (Exception e) {
            return new ValidateResult(null, null, null, rejected,
                    e.getMessage() != null ? e.getMessage() : "Unknown error", null);
        }
    }

    // ──────────────────────────────────────────────
    // senderIds()
    // ──────────────────────────────────────────────

    /**
     * List available sender IDs on this account.
     */
    public SenderIdResult senderIds() {
        try {
            Map<String, Object> response = ApiRequest.post("senderid", basePayload(), logFile);
            String result = objToString(response.get("result"));

            if ("OK".equals(result)) {
                return new SenderIdResult("OK", toStringList(response.get("senderid")), null, null, null);
            } else {
                Map<String, Object> enriched = ApiErrors.enrichError(response);
                return new SenderIdResult("ERROR", null,
                        objToString(enriched.get("code")),
                        objToString(enriched.get("description")),
                        objToString(enriched.get("action")));
            }
        } catch (Exception e) {
            return new SenderIdResult("ERROR", null, null,
                    e.getMessage() != null ? e.getMessage() : "Unknown error", null);
        }
    }

    // ──────────────────────────────────────────────
    // coverage()
    // ──────────────────────────────────────────────

    /**
     * List active country prefixes on this account.
     */
    public CoverageResult coverage() {
        try {
            Map<String, Object> response = ApiRequest.post("coverage", basePayload(), logFile);
            String result = objToString(response.get("result"));

            if ("OK".equals(result)) {
                return new CoverageResult("OK", toStringList(response.get("prefixes")), null, null, null);
            } else {
                Map<String, Object> enriched = ApiErrors.enrichError(response);
                return new CoverageResult("ERROR", null,
                        objToString(enriched.get("code")),
                        objToString(enriched.get("description")),
                        objToString(enriched.get("action")));
            }
        } catch (Exception e) {
            return new CoverageResult("ERROR", null, null,
                    e.getMessage() != null ? e.getMessage() : "Unknown error", null);
        }
    }

    // ──────────────────────────────────────────────
    // status()
    // ──────────────────────────────────────────────

    /**
     * Check the queue status of a sent message.
     *
     * @param msgId message ID from a send() response
     */
    public StatusResult status(String msgId) {
        try {
            Map<String, Object> payload = basePayload();
            payload.put("msgid", msgId);
            Map<String, Object> response = ApiRequest.post("status", payload, logFile);
            String result = objToString(response.get("result"));

            if ("OK".equals(result)) {
                return new StatusResult("OK",
                        objToString(response.get("status")),
                        objToString(response.get("description")),
                        null, null, null);
            } else {
                Map<String, Object> enriched = ApiErrors.enrichError(response);
                return new StatusResult("ERROR", null, null,
                        objToString(enriched.get("code")),
                        objToString(enriched.get("description")),
                        objToString(enriched.get("action")));
            }
        } catch (Exception e) {
            return new StatusResult("ERROR", null, null, null,
                    e.getMessage() != null ? e.getMessage() : "Unknown error", null);
        }
    }

    // ──────────────────────────────────────────────
    // deliveryReport()
    // ──────────────────────────────────────────────

    /**
     * Get delivery reports for a sent message (international numbers only).
     * Kuwait numbers do not support delivery reports.
     *
     * @param msgId message ID from a send() response
     */
    @SuppressWarnings("unchecked")
    public DeliveryReportResult deliveryReport(String msgId) {
        try {
            Map<String, Object> payload = basePayload();
            payload.put("msgid", msgId);
            Map<String, Object> response = ApiRequest.post("dlr", payload, logFile);
            String result = objToString(response.get("result"));

            if ("OK".equals(result)) {
                List<Map<String, Object>> reportList = (List<Map<String, Object>>) response.get("report");
                if (reportList == null) reportList = Collections.emptyList();
                List<DeliveryReportEntry> entries = new ArrayList<>();
                for (Map<String, Object> entry : reportList) {
                    entries.add(new DeliveryReportEntry(
                            objToString(entry.get("Number")),
                            objToString(entry.get("Status"))));
                }
                return new DeliveryReportResult("OK", entries, null, null, null);
            } else {
                Map<String, Object> enriched = ApiErrors.enrichError(response);
                return new DeliveryReportResult("ERROR", null,
                        objToString(enriched.get("code")),
                        objToString(enriched.get("description")),
                        objToString(enriched.get("action")));
            }
        } catch (Exception e) {
            return new DeliveryReportResult("ERROR", null, null,
                    e.getMessage() != null ? e.getMessage() : "Unknown error", null);
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private static String objToString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Double objToDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static Integer objToInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static Long objToLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static List<String> toStringList(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static String join(List<String> list, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
