import com.kwtsms.*;

import java.security.SecureRandom;

public class OtpFlow {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static void main(String[] args) {
        KwtSMS sms = KwtSMS.fromEnv();

        String phone = "965XXXXXXXX";
        String appName = "MyApp";

        // Generate a 6-digit OTP
        int otp = 100000 + RANDOM.nextInt(900000);
        String message = "Your OTP for " + appName + " is: " + otp;

        // Send OTP (use transactional sender ID for OTP, not promotional)
        SendResult result = sms.send(phone, message);

        if ("OK".equals(result.getResult())) {
            System.out.println("OTP sent to " + phone);
            System.out.println("Message ID: " + result.getMsgId());
            // Store: phone, otp, msgId, timestamp (expires in 5 min)
        } else {
            System.err.println("OTP send failed: " + result.getDescription());
            if (result.getAction() != null) {
                System.err.println("Action: " + result.getAction());
            }
        }

        // OTP best practices:
        // - Always include app name in the message (telecom compliance)
        // - Use transactional sender ID (promotional blocked by DND)
        // - Set 3-5 minute expiry
        // - Generate new code on resend, invalidate previous
        // - Minimum 3-4 minute resend timer (KNET standard: 4 min)
        // - Send to one number per request (avoid ERR028 batch rejection)
    }
}
