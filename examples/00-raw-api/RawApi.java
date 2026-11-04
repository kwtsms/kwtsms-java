import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Raw kwtSMS API example — no client library, no dependencies.
 *
 * Demonstrates every kwtSMS REST endpoint using only java.net.HttpURLConnection.
 * Copy-paste any section into your project and adjust the credentials.
 *
 * API docs: https://www.kwtsms.com/doc/KwtSMS.com_API_Documentation_v41.pdf
 */
public class RawApi {

    // ┌──────────────────────────────────────────────────┐
    // │  CONFIGURE THESE — your kwtSMS API credentials   │
    // └──────────────────────────────────────────────────┘
    static final String USERNAME  = "java_your_api_username";
    static final String PASSWORD  = "java_your_api_password";
    static final String SENDER_ID = "KWT-SMS";       // use your private sender ID in production
    static final String TEST_MODE = "1";             // "1" = test (no delivery), "0" = live

    static final String BASE_URL = "https://www.kwtsms.com/API";

    // ─── Helper: POST JSON to a kwtSMS endpoint and return the response ───

    static String post(String endpoint, String jsonBody) throws Exception {
        URL url = new URL(BASE_URL + "/" + endpoint + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        // Read response — kwtSMS returns JSON even for error status codes
        java.io.InputStream stream;
        try {
            stream = conn.getInputStream();
        } catch (Exception e) {
            stream = conn.getErrorStream();
        }

        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        }
        conn.disconnect();
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 1. VERIFY CREDENTIALS
        //    POST /API/balance/
        //    Quick way to check if username/password are correct.
        //    Returns available and purchased credits.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 1. Verify credentials (balance) ──");

        String balanceResponse = post("balance",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"" +
            "}"
        );

        System.out.println("Response: " + balanceResponse);
        // Success: {"result":"OK","available":500,"purchased":1000}
        // Error:   {"result":"ERROR","code":"ERR003","description":"Authentication error..."}
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 2. LIST SENDER IDs
        //    POST /API/senderid/
        //    Returns all sender IDs registered on your account.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 2. Sender IDs ──");

        String senderIdResponse = post("senderid",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"" +
            "}"
        );

        System.out.println("Response: " + senderIdResponse);
        // Success: {"result":"OK","senderid":["KWT-SMS","MY-APP"]}
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 3. CHECK COVERAGE
        //    POST /API/coverage/
        //    Returns country prefixes active on your account.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 3. Coverage ──");

        String coverageResponse = post("coverage",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"" +
            "}"
        );

        System.out.println("Response: " + coverageResponse);
        // Success: {"result":"OK","coverage":["965","966","971"]}
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 4. VALIDATE PHONE NUMBERS
        //    POST /API/validate/
        //    Check if numbers are valid and routable before sending.
        //    Numbers must be digits only, international format.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 4. Validate numbers ──");

        String validateResponse = post("validate",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"," +
                "\"mobile\":\"96598765432,966558724477,invalid\"" +
            "}"
        );

        System.out.println("Response: " + validateResponse);
        // Success: {"result":"OK","mobile":{"OK":["96598765432"],"ER":["invalid"],"NR":["966558724477"]}}
        // OK = valid and routable
        // ER = format error
        // NR = no route (country not activated on account)
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 5. SEND SMS
        //    POST /API/send/
        //    Send a message to one or more numbers (max 200 per request).
        //    Numbers: digits only, comma-separated, international format.
        //    IMPORTANT: Save msg-id and balance-after from every response.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 5. Send SMS ──");
        if ("1".equals(TEST_MODE)) {
            System.out.println("   (TEST MODE — message queued but NOT delivered)");
        }

        String sendResponse = post("send",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"," +
                "\"sender\":\"" + SENDER_ID + "\"," +
                "\"mobile\":\"96598765432\"," +
                "\"message\":\"Hello from Java raw API example\"," +
                "\"test\":\"" + TEST_MODE + "\"" +
            "}"
        );

        System.out.println("Response: " + sendResponse);
        // Success: {"result":"OK","msg-id":"abc123...","numbers":1,"points-charged":1,"balance-after":499,"unix-timestamp":1684763355}
        // Error:   {"result":"ERROR","code":"ERR003","description":"Authentication error..."}
        //
        // IMPORTANT fields to save:
        //   msg-id        → needed for status() and dlr() later
        //   balance-after → your new balance, no need to call /balance/ again
        //   unix-timestamp → server time in GMT+3 (Asia/Kuwait), NOT UTC
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 6. SEND TO MULTIPLE NUMBERS
        //    Comma-separated, max 200 per request.
        //    For >200 numbers, split into batches yourself.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 6. Send to multiple numbers ──");

        String multiResponse = post("send",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"," +
                "\"sender\":\"" + SENDER_ID + "\"," +
                "\"mobile\":\"96598765432,96512345678\"," +
                "\"message\":\"Bulk test from Java\"," +
                "\"test\":\"" + TEST_MODE + "\"" +
            "}"
        );

        System.out.println("Response: " + multiResponse);
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 7. CHECK MESSAGE STATUS
        //    POST /API/status/
        //    Check if a sent message is queued, sent, or has an error.
        //    Requires the msg-id from the send response.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 7. Message status ──");

        // Replace with a real msg-id from a send response
        String msgId = "replace_with_real_msg_id";

        String statusResponse = post("status",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"," +
                "\"msgid\":\"" + msgId + "\"" +
            "}"
        );

        System.out.println("Response: " + statusResponse);
        // Success: {"result":"OK","status":"sent","description":"Message successfully sent to gateway"}
        // Error:   {"result":"ERROR","code":"ERR029","description":"Message does not exist"}
        // ERR030:  Message stuck in queue — delete at kwtsms.com to recover credits
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 8. DELIVERY REPORT (international numbers only)
        //    POST /API/dlr/
        //    Check if an international message was delivered to the handset.
        //    NOT available for Kuwait numbers.
        //    Wait at least 5 minutes after sending before checking.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── 8. Delivery report (international only) ──");

        String dlrResponse = post("dlr",
            "{" +
                "\"username\":\"" + USERNAME + "\"," +
                "\"password\":\"" + PASSWORD + "\"," +
                "\"msgid\":\"" + msgId + "\"" +
            "}"
        );

        System.out.println("Response: " + dlrResponse);
        // Success: {"result":"OK","report":[{"Number":"96598765432","Status":"Received by recipient"}]}
        // Error:   {"result":"ERROR","code":"ERR019","description":"No reports found"}
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // DONE
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        System.out.println("── Done ──");
        System.out.println("All endpoints demonstrated. See comments in the code for response formats.");
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  - Replace credentials with your own API username and password");
        System.out.println("  - Set TEST_MODE to \"0\" when ready for production");
        System.out.println("  - Register a private Sender ID (don't use KWT-SMS in production)");
        System.out.println("  - Save msg-id and balance-after from every send response");
        System.out.println("  - Parse the JSON responses to extract fields you need");
        System.out.println();
        System.out.println("For a full-featured client library with phone normalization,");
        System.out.println("message cleaning, auto-batching, and error handling, use kwtsms-java:");
        System.out.println("  https://github.com/kwtsms/kwtsms-java");
    }
}
