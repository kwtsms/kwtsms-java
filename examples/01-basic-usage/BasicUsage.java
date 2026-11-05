import com.kwtsms.*;

public class BasicUsage {
    public static void main(String[] args) {
        // Load credentials from environment variables or .env file
        KwtSMS sms = KwtSMS.fromEnv();

        // Verify credentials and check balance
        VerifyResult verify = sms.verify();
        if (verify.isOk()) {
            System.out.println("Connected. Balance: " + verify.getBalance());
        } else {
            System.err.println("Auth failed: " + verify.getError());
            return;
        }

        // Send a single SMS
        SendResult result = sms.send("965XXXXXXXX", "Hello from Java!");
        if ("OK".equals(result.getResult())) {
            System.out.println("Sent! Message ID: " + result.getMsgId());
            System.out.println("Balance after: " + result.getBalanceAfter());
        } else {
            System.err.println("Send failed: " + result.getDescription());
            if (result.getAction() != null) {
                System.err.println("Action: " + result.getAction());
            }
        }

        // Send to multiple numbers
        SendResult multi = sms.send("965XXXXXXXX,96512345678", "Bulk test");
        System.out.println("Multi result: " + multi.getResult());
    }
}
