package com.kwtsms;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;

/**
 * Integration tests that hit the live kwtSMS API with test_mode=true.
 * Skipped if JAVA_USERNAME / JAVA_PASSWORD are not set.
 *
 * balance() is called ONCE in BeforeAll. The total credits needed is computed
 * upfront. Tests run in order and track a running expected balance so every
 * send result can be verified against it without extra API calls.
 *
 * 2026-11-05 09:00 +03: every send goes only to KWTSMS_TEST_NUMBER (TestNumber helper,
 * skips when unset); the 250-number generated bulk send was removed. Why: the old
 * hardcoded and generated numbers belong to real people. Task: int-testnumber
 */
@EnabledIfEnvironmentVariable(named = "JAVA_USERNAME", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static KwtSMS sms;
    private static KwtSMS badSms;

    /** Balance retrieved once in BeforeAll. Updated after each send. */
    private static double expectedBalance;

    // Credits consumed by send tests:
    //   6 single sends (1 each) + wrongSenderId (1, API accepts it) = 7
    // emptySenderId returns ERROR (0 credits), local failures = 0
    private static final int TOTAL_CREDITS_NEEDED = 7;

    @BeforeAll
    static void setUp() {
        String username = System.getenv("JAVA_USERNAME");
        String password = System.getenv("JAVA_PASSWORD");
        sms = new KwtSMS(username, password, "KWT-SMS", true, "");
        badSms = new KwtSMS("java_wrong_user", "java_wrong_pass", "KWT-SMS", true, "");

        // ONE balance call for the entire test class
        Double bal = sms.balance();
        assertNotNull(bal, "Should retrieve starting balance");
        expectedBalance = bal;
        System.out.println("=== IntegrationTest: starting balance = " + expectedBalance
                + " | credits needed = " + TOTAL_CREDITS_NEEDED + " ===");
    }

    // ── 1. Read-only tests (0 credits) ──

    @Test @Order(1)
    void verify_validCredentials() {
        VerifyResult result = sms.verify();
        assertTrue(result.isOk(), "verify() should succeed");
        assertNotNull(result.getBalance(), "Balance should be a number");
    }

    @Test @Order(2)
    void verify_wrongCredentials() {
        VerifyResult result = badSms.verify();
        assertFalse(result.isOk(), "verify() should fail with wrong credentials");
        assertNotNull(result.getError(), "Should have an error message");
    }

    @Test @Order(3)
    void balance_returnsNumber() {
        Double bal = sms.balance();
        assertNotNull(bal, "Balance should not be null");
        assertTrue(bal >= 0, "Balance should be non-negative");
    }

    @Test @Order(4)
    void senderIds_returnsList() {
        SenderIdResult result = sms.senderIds();
        assertEquals("OK", result.getResult());
        assertNotNull(result.getSenderIds());
        assertFalse(result.getSenderIds().isEmpty(), "Should have at least one sender ID");
    }

    @Test @Order(5)
    void coverage_returnsPrefixes() {
        CoverageResult result = sms.coverage();
        assertEquals("OK", result.getResult());
        assertNotNull(result.getPrefixes());
    }

    @Test @Order(6)
    void validate_validNumbers() {
        ValidateResult result = sms.validate(Arrays.asList(TestNumber.get()));
        assertNotNull(result);
    }

    @Test @Order(7)
    void validate_mixedInputs() {
        ValidateResult result = sms.validate(
                Arrays.asList(TestNumber.get(), "invalid", "user@email.com"));
        assertNotNull(result);
        assertFalse(result.getRejected().isEmpty(), "Should have rejected entries");
    }

    @Test @Order(8)
    void status_invalidMsgId() {
        StatusResult result = sms.status("nonexistent_msgid");
        assertEquals("ERROR", result.getResult());
    }

    @Test @Order(9)
    void deliveryReport_invalidMsgId() {
        DeliveryReportResult result = sms.deliveryReport("nonexistent_msgid");
        assertEquals("ERROR", result.getResult());
    }

    // ── 2. Local validation failures (0 credits, never hit API) ──

    @Test @Order(10)
    void send_invalidInputEmail() {
        SendResult result = sms.send("user@example.com", "Test");
        assertEquals("ERROR", result.getResult());
        assertFalse(result.getInvalid().isEmpty(), "Should have invalid entries");
    }

    @Test @Order(11)
    void send_invalidInputTooShort() {
        SendResult result = sms.send("123", "Test");
        assertEquals("ERROR", result.getResult());
        assertFalse(result.getInvalid().isEmpty());
    }

    @Test @Order(12)
    void send_invalidInputLetters() {
        SendResult result = sms.send("abcdefgh", "Test");
        assertEquals("ERROR", result.getResult());
    }

    @Test @Order(13)
    void send_emptyMessage() {
        SendResult result = sms.send(TestNumber.get(), "");
        assertEquals("ERROR", result.getResult());
    }

    @Test @Order(14)
    void send_emojiOnlyMessage() {
        SendResult result = sms.send(TestNumber.get(),"\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02");
        assertEquals("ERROR", result.getResult());
    }

    @Test @Order(15)
    void send_emptySenderId() {
        // Empty sender ID is rejected by the API: no credits consumed
        SendResult result = sms.send(TestNumber.get(),"Test empty sender", "");
        assertEquals("ERROR", result.getResult());
        System.out.println("send_emptySenderId | result: ERROR | balance unchanged: " + expectedBalance);
    }

    // ── 3. Re-snapshot balance before send tests ──
    // Balance may have changed since BeforeAll due to queued messages from prior runs
    // being processed. Re-fetch to establish an accurate baseline for send tracking.

    @Test @Order(19)
    void balance_snapshotBeforeSends() {
        Double bal = sms.balance();
        assertNotNull(bal, "Should retrieve balance");
        expectedBalance = bal;
        System.out.println("Balance snapshot before sends: " + bal
                + " | credits needed: " + TOTAL_CREDITS_NEEDED);
    }

    // ── 4. Send tests (consume credits, need sufficient balance) ──

    @Test @Order(20)
    void send_validKuwaitNumber() {
        assumeTrue(expectedBalance >= TOTAL_CREDITS_NEEDED,
                "Insufficient balance: " + expectedBalance + " < " + TOTAL_CREDITS_NEEDED);

        SendResult result = sms.send(TestNumber.get(),"Java integration test");

        assertEquals("OK", result.getResult());
        assertEquals(1, (int) result.getPointsCharged(), "1 number = 1 credit");
        expectedBalance -= 1;
        assertEquals(expectedBalance, result.getBalanceAfter(), 0.01,
                "Expected balance: " + expectedBalance);
        System.out.println("send_validKuwaitNumber | -1 | balance: " + expectedBalance);
    }

    @Test @Order(21)
    void send_mixedValidAndInvalid() {
        assumeTrue(expectedBalance >= 1, "Insufficient balance");

        SendResult result = sms.send(TestNumber.get() + ",invalid,user@email.com","Test mixed");

        assertEquals("OK", result.getResult());
        assertEquals(2, result.getInvalid().size(), "2 invalid entries");
        assertEquals(1, (int) result.getPointsCharged(), "1 valid = 1 credit");
        expectedBalance -= 1;
        assertEquals(expectedBalance, result.getBalanceAfter(), 0.01,
                "Expected balance: " + expectedBalance);
        System.out.println("send_mixedValidAndInvalid | -1 | balance: " + expectedBalance);
    }

    @Test @Order(22)
    void send_normalizationPlusPrefix() {
        assumeTrue(expectedBalance >= 1, "Insufficient balance");

        SendResult result = sms.send(TestNumber.variant("plus"),"Test plus prefix");

        assertEquals("OK", result.getResult());
        assertEquals(1, (int) result.getPointsCharged(), "1 credit");
        expectedBalance -= 1;
        assertEquals(expectedBalance, result.getBalanceAfter(), 0.01,
                "Expected balance: " + expectedBalance);
        System.out.println("send_normalizationPlusPrefix | -1 | balance: " + expectedBalance);
    }

    @Test @Order(23)
    void send_normalization00Prefix() {
        assumeTrue(expectedBalance >= 1, "Insufficient balance");

        SendResult result = sms.send(TestNumber.variant("00"),"Test 00 prefix");

        assertEquals("OK", result.getResult());
        assertEquals(1, (int) result.getPointsCharged(), "1 credit");
        expectedBalance -= 1;
        assertEquals(expectedBalance, result.getBalanceAfter(), 0.01,
                "Expected balance: " + expectedBalance);
        System.out.println("send_normalization00Prefix | -1 | balance: " + expectedBalance);
    }

    @Test @Order(24)
    void send_normalizationArabicDigits() {
        assumeTrue(expectedBalance >= 1, "Insufficient balance");

        // The test number written in Arabic-Indic digits
        SendResult result = sms.send(TestNumber.variant("arabic"), "Test Arabic digits");

        assertEquals("OK", result.getResult());
        assertEquals(1, (int) result.getPointsCharged(), "1 credit");
        expectedBalance -= 1;
        assertEquals(expectedBalance, result.getBalanceAfter(), 0.01,
                "Expected balance: " + expectedBalance);
        System.out.println("send_normalizationArabicDigits | -1 | balance: " + expectedBalance);
    }

    @Test @Order(25)
    void send_duplicateNormalized() {
        assumeTrue(expectedBalance >= 1, "Insufficient balance");

        SendResult result = sms.send(TestNumber.variant("plus") + "," + TestNumber.variant("00"), "Test dedup");

        assertEquals("OK", result.getResult());
        assertEquals(1, (int) result.getNumbers(), "Deduplicated to 1 number");
        assertEquals(1, (int) result.getPointsCharged(), "1 credit (deduped)");
        expectedBalance -= 1;
        assertEquals(expectedBalance, result.getBalanceAfter(), 0.01,
                "Expected balance: " + expectedBalance);
        System.out.println("send_duplicateNormalized | -1 | balance: " + expectedBalance);
    }

    @Test @Order(26)
    void send_wrongSenderId() {
        assumeTrue(expectedBalance >= 1, "Insufficient balance");

        SendResult result = sms.send(TestNumber.get(), "Test wrong sender", "NONEXISTENT-SENDER");

        // API accepts unknown sender IDs (falls back to default): consumes 1 credit
        assertEquals("OK", result.getResult());
        assertEquals(1, (int) result.getPointsCharged(), "1 credit");
        expectedBalance -= 1;
        assertEquals(expectedBalance, result.getBalanceAfter(), 0.01,
                "Expected balance: " + expectedBalance);
        System.out.println("send_wrongSenderId | -1 | balance: " + expectedBalance);
    }

    // ── 5. Final balance verification ──

    @Test @Order(999)
    void balance_finalVerification() {
        Double bal = sms.balance();
        assertEquals(expectedBalance, bal, 0.01,
                "Final balance should match tracked expected balance");
        System.out.println("=== Final balance: " + bal + " (expected: " + expectedBalance + ") ===");
    }
}
