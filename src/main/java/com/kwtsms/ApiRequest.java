package com.kwtsms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP request layer for kwtSMS API. Uses java.net.HttpURLConnection (no dependencies).
 * Includes a minimal JSON serializer and parser.
 */
final class ApiRequest {

    private ApiRequest() {}

    private static final String BASE_URL = "https://www.kwtsms.com/API";
    private static final int TIMEOUT_MS = 15_000;
    private static final int MAX_RESPONSE_SIZE = 1_048_576; // 1 MB

    /**
     * Make a POST request to a kwtSMS API endpoint.
     *
     * @param endpoint API endpoint name (e.g., "send", "balance")
     * @param payload  request body as a map
     * @param logFile  path to JSONL log file (empty string to disable logging)
     * @param userAgent User-Agent header value (see KwtSMS.buildUserAgent)
     * @return parsed JSON response as a map
     * @throws ApiException on network or parsing errors
     * 2026-11-05 14:20 +03: userAgent parameter added, sent on every call (#939).
     */
    static Map<String, Object> post(String endpoint, Map<String, Object> payload, String logFile, String userAgent) {
        return post(BASE_URL, endpoint, payload, logFile, userAgent);
    }

    /**
     * Same as {@link #post(String, Map, String, String)} against a given base URL; package-private so unit
     * tests can point it at a local server. 2026-11-05 14:20 +03: added for the User-Agent test (#939).
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> post(String baseUrl, String endpoint, Map<String, Object> payload, String logFile,
                                    String userAgent) {
        String url = baseUrl + "/" + endpoint + "/";
        String jsonBody = toJson(payload);
        String responseBody = "";

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            // 2026-11-05 14:20 +03: kwtsms-java/<version>, app token first when given (#939).
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);

            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Read response (including error responses: kwtSMS returns JSON in 4xx bodies)
            java.io.InputStream stream;
            try {
                stream = connection.getInputStream();
            } catch (Exception e) {
                stream = connection.getErrorStream();
            }

            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int charsRead;
                    while ((charsRead = reader.read(buf)) != -1) {
                        if (sb.length() + charsRead > MAX_RESPONSE_SIZE) {
                            throw new ApiException("Response too large (>" + MAX_RESPONSE_SIZE + " bytes)");
                        }
                        sb.append(buf, 0, charsRead);
                    }
                    responseBody = sb.toString();
                }
            }

            connection.disconnect();

            if (responseBody.isEmpty()) {
                KwtSMSLogger.writeLog(logFile, endpoint, payload, "", false, "Empty response");
                throw new ApiException("Empty response from API");
            }

            Object parsed = parseJson(responseBody.trim());
            if (!(parsed instanceof Map)) {
                throw new ApiException("Invalid JSON response");
            }

            Map<String, Object> result = (Map<String, Object>) parsed;
            boolean isOk = "OK".equals(result.get("result") == null ? null : result.get("result").toString());
            KwtSMSLogger.writeLog(logFile, endpoint, payload, responseBody, isOk);

            return result;
        } catch (SocketTimeoutException e) {
            KwtSMSLogger.writeLog(logFile, endpoint, payload, responseBody, false,
                    "Request timed out after " + TIMEOUT_MS + "ms");
            throw new ApiException("Request timed out after " + TIMEOUT_MS + "ms");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown network error";
            KwtSMSLogger.writeLog(logFile, endpoint, payload, responseBody, false, msg);
            throw new ApiException("Network error: " + msg);
        }
    }

    // ── JSON Serializer ──

    static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escapeJson(entry.getKey())).append("\":");
            appendValue(sb, entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append('"').append(escapeJson((String) value)).append('"');
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof List) {
            sb.append('[');
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                appendValue(sb, list.get(i));
            }
            sb.append(']');
        } else if (value instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escapeJson(e.getKey().toString())).append("\":");
                appendValue(sb, e.getValue());
            }
            sb.append('}');
        } else {
            sb.append('"').append(escapeJson(value.toString())).append('"');
        }
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    // ── JSON Parser ──

    static Object parseJson(String json) {
        JsonParser parser = new JsonParser(json);
        return parser.parseValue();
    }

    private static final class JsonParser {
        private static final int MAX_DEPTH = 50;
        private final String json;
        private int pos;
        private int depth;

        JsonParser(String json) {
            this.json = json;
            this.pos = 0;
            this.depth = 0;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char ch = json.charAt(pos);
            switch (ch) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': case 'f': return parseBoolean();
                case 'n': return parseNull();
                default: return parseNumber();
            }
        }

        private Map<String, Object> parseObject() {
            if (++depth > MAX_DEPTH) throw new IllegalArgumentException("JSON nesting too deep (>" + MAX_DEPTH + ")");
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip {
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < json.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else {
                    break;
                }
            }
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == '}') pos++;
            depth--;
            return map;
        }

        private List<Object> parseArray() {
            if (++depth > MAX_DEPTH) throw new IllegalArgumentException("JSON nesting too deep (>" + MAX_DEPTH + ")");
            List<Object> list = new ArrayList<>();
            pos++; // skip [
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < json.length()) {
                list.add(parseValue());
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else {
                    break;
                }
            }
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ']') pos++;
            depth--;
            return list;
        }

        private String parseString() {
            skipWhitespace();
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < json.length() && json.charAt(pos) != '"') {
                if (json.charAt(pos) == '\\') {
                    pos++;
                    if (pos < json.length()) {
                        char esc = json.charAt(pos);
                        switch (esc) {
                            case '"':  sb.append('"'); break;
                            case '\\': sb.append('\\'); break;
                            case '/':  sb.append('/'); break;
                            case 'n':  sb.append('\n'); break;
                            case 'r':  sb.append('\r'); break;
                            case 't':  sb.append('\t'); break;
                            case 'b':  sb.append('\b'); break;
                            case 'f':  sb.append('\f'); break;
                            case 'u':
                                if (pos + 4 < json.length()) {
                                    String hex = json.substring(pos + 1, pos + 5);
                                    char unit = (char) Integer.parseInt(hex, 16);
                                    pos += 4;
                                    // Handle surrogate pairs: \uD800-\uDBFF followed by \uDC00-\uDFFF
                                    if (Character.isHighSurrogate(unit)
                                            && pos + 1 < json.length() && json.charAt(pos + 1) == '\\'
                                            && pos + 2 < json.length() && json.charAt(pos + 2) == 'u'
                                            && pos + 6 < json.length()) {
                                        String hex2 = json.substring(pos + 3, pos + 7);
                                        char low = (char) Integer.parseInt(hex2, 16);
                                        if (Character.isLowSurrogate(low)) {
                                            sb.appendCodePoint(Character.toCodePoint(unit, low));
                                            pos += 6; // skip low surrogate escape
                                        } else {
                                            sb.append(unit);
                                        }
                                    } else {
                                        sb.append(unit);
                                    }
                                }
                                break;
                            default: sb.append(esc);
                        }
                    }
                } else {
                    sb.append(json.charAt(pos));
                }
                pos++;
            }
            if (pos < json.length()) pos++; // skip closing "
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < json.length() && json.charAt(pos) == '-') pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
            boolean isDouble = false;
            if (pos < json.length() && json.charAt(pos) == '.') {
                isDouble = true;
                pos++;
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
            }
            if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
                isDouble = true;
                pos++;
                if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) pos++;
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
            }
            String numStr = json.substring(start, pos);
            if (isDouble) {
                return Double.parseDouble(numStr);
            } else {
                long longVal = Long.parseLong(numStr);
                if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
                    return (int) longVal;
                }
                return longVal;
            }
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return true;
            } else if (json.startsWith("false", pos)) {
                pos += 5;
                return false;
            } else {
                throw new IllegalArgumentException("Expected 'true' or 'false' at position " + pos);
            }
        }

        private Object parseNull() {
            pos += 4;
            return null;
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        }

        private void expect(char ch) {
            if (pos >= json.length() || json.charAt(pos) != ch) {
                throw new IllegalArgumentException("Expected '" + ch + "' at position " + pos);
            }
            pos++;
        }
    }
}

/**
 * Exception for API communication errors.
 */
class ApiException extends RuntimeException {
    ApiException(String message) {
        super(message);
    }
}
