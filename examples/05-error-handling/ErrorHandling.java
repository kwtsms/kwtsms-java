import com.kwtsms.*;

public class ErrorHandling {
    public static void main(String[] args) {
        KwtSMS sms = KwtSMS.fromEnv();

        // Example: send to various input types
        String[] testInputs = {
            "965XXXXXXXX",           // valid format (X = digits)
            "user@example.com",      // email
            "123",                   // too short
            "abcdefgh",             // no digits
            "",                      // empty
            "+965 XXXX XXXX",        // valid format with spaces
        };

        for (String input : testInputs) {
            System.out.println("\nSending to: '" + input + "'");

            PhoneUtils.ValidationResult vr = PhoneUtils.validatePhoneInput(input);
            if (!vr.isValid()) {
                System.out.println("  Local validation failed: " + vr.getError());
                continue;
            }

            System.out.println("  Normalized: " + vr.getNormalized());
            SendResult result = sms.send(input, "Error handling test");

            switch (result.getResult()) {
                case "OK":
                    System.out.println("  Sent. ID: " + result.getMsgId());
                    break;
                case "ERROR":
                    System.out.println("  Error: " + result.getCode() + " - " + result.getDescription());
                    if (result.getAction() != null) {
                        System.out.println("  Action: " + result.getAction());
                    }
                    break;
            }

            // Report any invalid numbers from the batch
            for (InvalidEntry inv : result.getInvalid()) {
                System.out.println("  Invalid: " + inv.getInput() + " - " + inv.getError());
            }
        }

        // Example: user-facing error messages
        System.out.println("\n--- User-Facing Error Mapping ---");
        SendResult r = sms.send("965XXXXXXXX", "Test");
        if (!"OK".equals(r.getResult())) {
            String userMessage = mapToUserMessage(r.getCode());
            System.out.println("Show to user: " + userMessage);
            System.out.println("Log for admin: " + r.getCode() + " - " + r.getDescription());
        }
    }

    /** Map API errors to user-friendly messages. Never expose raw codes to end users. */
    static String mapToUserMessage(String code) {
        if (code == null) return "Something went wrong. Please try again.";
        switch (code) {
            case "ERR006":
            case "ERR025":
                return "Please enter a valid phone number in international format (e.g., +965 XXXX XXXX).";
            case "ERR003":
            case "ERR010":
            case "ERR011":
                return "SMS service is temporarily unavailable. Please try again later.";
            case "ERR026":
                return "SMS delivery to this country is not available. Please contact support.";
            case "ERR028":
                return "Please wait a moment before requesting another code.";
            case "ERR031":
            case "ERR032":
                return "Your message could not be sent. Please try again with different content.";
            case "ERR013":
                return "SMS service is busy. Please try again in a few minutes.";
            default:
                return "Something went wrong. Please try again.";
        }
    }
}
