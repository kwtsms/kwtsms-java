// Spring Boot REST endpoint example
// Add to your Spring Boot application

// import org.springframework.web.bind.annotation.*;
// import org.springframework.http.ResponseEntity;
import com.kwtsms.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Example Spring Boot controller for sending SMS.
 *
 * Usage:
 *   POST /api/sms/send
 *   {"phone": "965XXXXXXXX", "message": "Hello"}
 *
 * Dependencies (add to build.gradle):
 *   implementation 'com.github.boxlinknet:kwtsms-java:0.1.0'
 */
// @RestController
// @RequestMapping("/api/sms")
public class SpringEndpoint {

    // Inject as a singleton bean
    // @Bean
    // public KwtSMS kwtSms() {
    //     return KwtSMS.fromEnv();
    // }

    private final KwtSMS sms = KwtSMS.fromEnv();

    // @PostMapping("/send")
    public Map<String, Object> sendSms(/* @RequestBody */ Map<String, String> body) {
        String phone = body.get("phone");
        String message = body.get("message");

        Map<String, Object> response = new HashMap<>();

        if (phone == null || phone.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Phone number is required");
            return response;
        }

        if (message == null || message.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Message is required");
            return response;
        }

        SendResult result = sms.send(phone, message);

        if ("OK".equals(result.getResult())) {
            response.put("success", true);
            response.put("msgId", result.getMsgId());
            response.put("balanceAfter", result.getBalanceAfter());
        } else {
            response.put("success", false);
            // Show generic message to end users (never expose raw API errors)
            response.put("error", "SMS service is temporarily unavailable. Please try again later.");
            // Log the real error for the admin
            System.err.println("SMS send failed: " + result.getCode() + " - " + result.getDescription());
        }

        return response;
    }

    // @GetMapping("/balance")
    public Map<String, Object> getBalance() {
        Map<String, Object> response = new HashMap<>();
        VerifyResult verify = sms.verify();
        if (verify.isOk()) {
            response.put("balance", verify.getBalance());
        } else {
            response.put("error", verify.getError());
        }
        return response;
    }
}
