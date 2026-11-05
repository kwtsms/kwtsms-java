import com.kwtsms.*;

import java.util.Arrays;
import java.util.List;

public class BulkSms {
    public static void main(String[] args) {
        KwtSMS sms = KwtSMS.fromEnv();

        List<String> recipients = Arrays.asList(
            "965XXXXXXXX",
            "96512345678",
            "965XXXXXXXX"
            // ... up to thousands of numbers
        );

        String message = "Important announcement from our company.";

        // sendBulk() auto-splits into batches of 200
        // 0.5s delay between batches
        // ERR013 (queue full) retried up to 3x with 30s/60s/120s backoff
        BulkSendResult result = sms.sendBulk(recipients, message);

        System.out.println("Result: " + result.getResult());
        System.out.println("Batches: " + result.getBatches());
        System.out.println("Numbers sent: " + result.getNumbers());
        System.out.println("Points charged: " + result.getPointsCharged());
        System.out.println("Balance after: " + result.getBalanceAfter());

        if (!result.getMsgIds().isEmpty()) {
            System.out.println("Message IDs: " + result.getMsgIds());
        }

        if (!result.getErrors().isEmpty()) {
            System.err.println("Batch errors:");
            for (BatchError error : result.getErrors()) {
                System.err.println("  Batch " + error.getBatch() + ": " + error.getCode() + " - " + error.getDescription());
            }
        }

        if (!result.getInvalid().isEmpty()) {
            System.err.println("Invalid numbers:");
            for (InvalidEntry entry : result.getInvalid()) {
                System.err.println("  " + entry.getInput() + ": " + entry.getError());
            }
        }
    }
}
