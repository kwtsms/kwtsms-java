/*
 * FILE:      src/test/java/com/kwtsms/TestNumber.java
 * CREATED:   2026-11-05 08:59 +03
 * TASK:      int-testnumber
 * AUTHOR:    M Alhassoun
 * WHAT:      Shared test helper: the test phone number and its format variants.
 * WHY:       Tests must not hardcode a phone number. The number comes from the
 *            KWTSMS_TEST_NUMBER environment variable (an international Kuwait
 *            mobile, digits only, 965XXXXXXXX) and every test that needs one
 *            is skipped when it is unset or malformed.
 * USAGE:     KWTSMS_TEST_NUMBER=965XXXXXXXX ./gradlew test
 *            CI reads it from the repository secret of the same name.
 *            Tests call TestNumber.get() or TestNumber.variant("kind"), then
 *            compare with TestNumber.assertSame so a failure never prints the
 *            number. Never log the value.
 * DEPENDS:   JUnit 5 (Assumptions, Assertions), already a test dependency.
 */
package com.kwtsms;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

final class TestNumber {

    /** For values only passed through unchanged (for example a mocked response echo). */
    static final String PLACEHOLDER = "965XXXXXXXX";

    private static final String SKIP_REASON =
            "KWTSMS_TEST_NUMBER not set (digits only, 965 plus 8 digits)";

    // [0-9], not \d: keeps the check to Latin digits only.
    private static final String VALUE = read();

    private TestNumber() {
    }

    private static String read() {
        String n = System.getenv("KWTSMS_TEST_NUMBER");
        n = n == null ? "" : n.trim();
        return n.matches("^965[0-9]{8}$") ? n : null;
    }

    /** KWTSMS_TEST_NUMBER, or skips the running test when it is unset or malformed. */
    static String get() {
        assumeTrue(VALUE != null, SKIP_REASON);
        return VALUE;
    }

    /** Latin digits to Arabic-Indic (U+0660-0669). */
    static String toArabicIndic(String s) {
        return shift(s, '٠');
    }

    /** Latin digits to Extended Arabic-Indic / Persian (U+06F0-06F9). */
    static String toPersian(String s) {
        return shift(s, '۰');
    }

    private static String shift(String s, char zero) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(c >= '0' && c <= '9' ? (char) (zero + (c - '0')) : c);
        }
        return sb.toString();
    }

    /** The test number written in the format named by kind. Skips when unset. */
    static String variant(String kind) {
        String n = get();
        String cc = n.substring(0, 3);
        String a = n.substring(3, 7);
        String b = n.substring(7);
        switch (kind) {
            case "plain": return n;
            case "plus": return "+" + n;
            case "00": return "00" + n;
            case "spaces": return cc + " " + a + " " + b;
            case "dashes": return cc + "-" + a + "-" + b;
            case "dots": return cc + "." + a + "." + b;
            case "parens": return "(" + cc + ") " + a + b;
            case "mixed":
                return "  +" + cc + " (" + n.substring(3, 5) + ") "
                        + n.substring(5, 8) + "-" + n.substring(8) + "  ";
            case "arabic": return toArabicIndic(n);
            case "arabic-plus": return "+" + toArabicIndic(n);
            case "arabic-00": return toArabicIndic("00" + n);
            case "arabic-spaces": return toArabicIndic(cc + " " + a + " " + b);
            case "arabic-dashes": return toArabicIndic(cc + "-" + a + "-" + b);
            case "arabic-cc-latin": return cc + toArabicIndic(a + b);
            case "persian": return toPersian(n);
            case "persian-plus": return "+" + toPersian(n);
            case "persian-00": return toPersian("00" + n);
            default: throw new IllegalArgumentException("unknown variant kind: " + kind);
        }
    }

    /** Assert equality without printing either value (they may hold the number). */
    static void assertSame(String expected, String actual) {
        assertTrue(expected.equals(actual),
                "phone number does not match the expected value (values hidden)");
    }

    /** Assert the list holds the number without printing the list. */
    static void assertContains(String expected, List<String> actual) {
        assertTrue(actual.contains(expected), "phone number not found in the list (values hidden)");
    }
}
