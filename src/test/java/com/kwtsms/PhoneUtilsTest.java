package com.kwtsms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

// 2026-11-05 09:00 +03: Kuwait numbers come from KWTSMS_TEST_NUMBER via TestNumber (skips when unset);
// pass-through values use the 965XXXXXXXX placeholder; India sample changed so it no longer embeds the old Kuwait sample.
// Why: the old hardcoded sample belongs to a real person. Task: int-testnumber
class PhoneUtilsTest {

    // ── normalizePhone ──

    @Test
    void normalizePhone_stripsPlus() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("plus")));
    }

    @Test
    void normalizePhone_strips00Prefix() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("00")));
    }

    @Test
    void normalizePhone_stripsSpaces() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("spaces")));
    }

    @Test
    void normalizePhone_stripsDashes() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("dashes")));
    }

    @Test
    void normalizePhone_stripsDots() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("dots")));
    }

    @Test
    void normalizePhone_stripsParentheses() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("parens")));
    }

    @Test
    void normalizePhone_convertsArabicIndicDigits() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("arabic")));
    }

    @Test
    void normalizePhone_convertsExtendedArabicIndicDigits() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("persian")));
    }

    @Test
    void normalizePhone_stripsLeadingZeros() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("00")));
    }

    @Test
    void normalizePhone_emptyString() {
        assertEquals("", PhoneUtils.normalizePhone(""));
    }

    @Test
    void normalizePhone_onlyZeros() {
        assertEquals("", PhoneUtils.normalizePhone("000"));
    }

    @Test
    void normalizePhone_nullInput() {
        assertEquals("", PhoneUtils.normalizePhone(null));
    }

    @Test
    void normalizePhone_lettersOnly() {
        assertEquals("", PhoneUtils.normalizePhone("abcdef"));
    }

    @Test
    void normalizePhone_mixedContent() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("mixed")));
    }

    // ── Trunk prefix stripping ──

    @Test
    void normalizePhone_saudiTrunkPrefix() {
        // 9660559123456 -> 966559123456 (strip leading 0 after 966)
        assertEquals("966559123456", PhoneUtils.normalizePhone("9660559123456"));
    }

    @Test
    void normalizePhone_saudiWithPlus00TrunkPrefix() {
        // +9660559123456 -> 966559123456
        assertEquals("966559123456", PhoneUtils.normalizePhone("+9660559123456"));
    }

    @Test
    void normalizePhone_saudiWith00PlusTrunkPrefix() {
        // 009660559123456 -> 966559123456
        assertEquals("966559123456", PhoneUtils.normalizePhone("009660559123456"));
    }

    @Test
    void normalizePhone_uaeTrunkPrefix() {
        // 97105x... -> 9715x...
        assertEquals("971501234567", PhoneUtils.normalizePhone("9710501234567"));
    }

    @Test
    void normalizePhone_egyptTrunkPrefix() {
        // 20010... -> 2010...
        assertEquals("201012345678", PhoneUtils.normalizePhone("200101234567" + "8"));
    }

    @Test
    void normalizePhone_noTrunkPrefix_unaffected() {
        // Already correct: no trunk 0
        assertEquals("966559123456", PhoneUtils.normalizePhone("966559123456"));
    }

    @Test
    void normalizePhone_kuwaitNoTrunkPrefix() {
        // Kuwait numbers don't have trunk prefix issue
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("plain")));
    }

    // ── Arabic/Hindi digit edge cases ──

    @Test
    void normalizePhone_arabicIndicWithPlus() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("arabic-plus")));
    }

    @Test
    void normalizePhone_arabicIndicWith00Prefix() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("arabic-00")));
    }

    @Test
    void normalizePhone_arabicIndicWithSpaces() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("arabic-spaces")));
    }

    @Test
    void normalizePhone_arabicIndicWithDashes() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("arabic-dashes")));
    }

    @Test
    void normalizePhone_mixedArabicAndLatinDigits() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("arabic-cc-latin")));
    }

    @Test
    void normalizePhone_extendedArabicIndicWithPlus() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("persian-plus")));
    }

    @Test
    void normalizePhone_extendedArabicIndicWith00() {
        TestNumber.assertSame(TestNumber.get(), PhoneUtils.normalizePhone(TestNumber.variant("persian-00")));
    }

    // ── findCountryCode ──

    @Test
    void findCountryCode_threeDigit() {
        assertEquals("965", PhoneUtils.findCountryCode(TestNumber.PLACEHOLDER));
    }

    @Test
    void findCountryCode_twoDigit() {
        assertEquals("91", PhoneUtils.findCountryCode("919123456789"));
    }

    @Test
    void findCountryCode_oneDigit() {
        assertEquals("1", PhoneUtils.findCountryCode("12025551234"));
    }

    @Test
    void findCountryCode_unknown() {
        assertNull(PhoneUtils.findCountryCode("999123456"));
    }

    @Test
    void findCountryCode_null() {
        assertNull(PhoneUtils.findCountryCode(null));
    }

    @Test
    void findCountryCode_empty() {
        assertNull(PhoneUtils.findCountryCode(""));
    }

    // ── validatePhoneFormat (country-specific) ──

    @Test
    void validatePhoneFormat_validKuwait() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat(TestNumber.get());
        assertTrue(r.isValid());
    }

    @Test
    void validatePhoneFormat_kuwaitWrongLength() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("9659876543");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("Kuwait"));
        assertTrue(r.getError().contains("8 digits"));
    }

    @Test
    void validatePhoneFormat_kuwaitWrongPrefix() {
        // Kuwait mobile must start with 4,5,6,9 after 965
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("96512345678");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("Kuwait"));
        assertTrue(r.getError().contains("must start with"));
    }

    @Test
    void validatePhoneFormat_validSaudi() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("966559123456");
        assertTrue(r.isValid());
    }

    @Test
    void validatePhoneFormat_saudiWrongPrefix() {
        // Saudi mobile must start with 5 after 966
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("966712345678");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("Saudi Arabia"));
    }

    @Test
    void validatePhoneFormat_validUAE() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("971501234567");
        assertTrue(r.isValid());
    }

    @Test
    void validatePhoneFormat_validUSA() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("12025551234");
        assertTrue(r.isValid());
    }

    @Test
    void validatePhoneFormat_usaWrongLength() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("1202555");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("USA/Canada"));
    }

    @Test
    void validatePhoneFormat_unknownCountryPassesThrough() {
        // Unknown country code: passes generic validation only
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("999123456789");
        assertTrue(r.isValid());
    }

    @Test
    void validatePhoneFormat_validEgypt() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("201012345678");
        assertTrue(r.isValid());
    }

    @Test
    void validatePhoneFormat_validIndia() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneFormat("919123456789");
        assertTrue(r.isValid());
    }

    // ── validatePhoneInput (full pipeline) ──

    @Test
    void validatePhoneInput_validKuwaitNumber() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput(TestNumber.variant("plain"));
        assertTrue(r.isValid());
        assertNull(r.getError());
        TestNumber.assertSame(TestNumber.get(), r.getNormalized());
    }

    @Test
    void validatePhoneInput_validWithPlus() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput(TestNumber.variant("plus"));
        assertTrue(r.isValid());
        TestNumber.assertSame(TestNumber.get(), r.getNormalized());
    }

    @Test
    void validatePhoneInput_validMinimum7Digits() {
        // Use a number that doesn't match any known country code (999...)
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("9991234");
        assertTrue(r.isValid());
        assertEquals("9991234", r.getNormalized());
    }

    @Test
    void validatePhoneInput_validMaximum15Digits() {
        // Use a number that doesn't match any known country code
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("999123456789012");
        assertTrue(r.isValid());
        assertEquals("999123456789012", r.getNormalized());
    }

    @Test
    void validatePhoneInput_empty() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("");
        assertFalse(r.isValid());
        assertEquals("Phone number is required", r.getError());
    }

    @Test
    void validatePhoneInput_blank() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("   ");
        assertFalse(r.isValid());
        assertEquals("Phone number is required", r.getError());
    }

    @Test
    void validatePhoneInput_email() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("user@example.com");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("email address"));
    }

    @Test
    void validatePhoneInput_noDigits() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("abcdef");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("no digits found"));
    }

    @Test
    void validatePhoneInput_tooShort() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("123");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("too short"));
        assertTrue(r.getError().contains("3 digits"));
    }

    @Test
    void validatePhoneInput_tooLong() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("1234567890123456");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("too long"));
        assertTrue(r.getError().contains("16 digits"));
    }

    @Test
    void validatePhoneInput_arabicDigits() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput(TestNumber.variant("arabic"));
        assertTrue(r.isValid());
        TestNumber.assertSame(TestNumber.get(), r.getNormalized());
    }

    @Test
    void validatePhoneInput_extendedArabicIndicDigits() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput(TestNumber.variant("persian"));
        assertTrue(r.isValid());
        TestNumber.assertSame(TestNumber.get(), r.getNormalized());
    }

    @Test
    void validatePhoneInput_arabicDigitsWithPlus() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput(TestNumber.variant("arabic-plus"));
        assertTrue(r.isValid());
        TestNumber.assertSame(TestNumber.get(), r.getNormalized());
    }

    @Test
    void validatePhoneInput_arabicDigitsTooShort() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("\u0661\u0662\u0663");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("too short"));
    }

    @Test
    void validatePhoneInput_mixedArabicLatinDigits() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput(TestNumber.variant("arabic-cc-latin"));
        assertTrue(r.isValid());
        TestNumber.assertSame(TestNumber.get(), r.getNormalized());
    }

    // ── Saudi trunk prefix through full validation pipeline ──

    @Test
    void validatePhoneInput_saudiWithTrunkZero() {
        // 9660559123456 normalizes to 966559123456, which is valid Saudi
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("9660559123456");
        assertTrue(r.isValid());
        assertEquals("966559123456", r.getNormalized());
    }

    @Test
    void validatePhoneInput_saudiWithPlusTrunkZero() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("+9660559123456");
        assertTrue(r.isValid());
        assertEquals("966559123456", r.getNormalized());
    }

    @Test
    void validatePhoneInput_saudiWith00TrunkZero() {
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("009660559123456");
        assertTrue(r.isValid());
        assertEquals("966559123456", r.getNormalized());
    }

    @Test
    void validatePhoneInput_countrySpecificRejectsInvalidKuwaitPrefix() {
        // 96512345678: Kuwait number starting with 1 (not 4,5,6,9)
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("96512345678");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("Kuwait"));
    }

    @Test
    void validatePhoneInput_countrySpecificRejectsInvalidSaudiPrefix() {
        // 966712345678: Saudi number starting with 7 (not 5)
        PhoneUtils.ValidationResult r = PhoneUtils.validatePhoneInput("966712345678");
        assertFalse(r.isValid());
        assertTrue(r.getError().contains("Saudi Arabia"));
    }

    // ── deduplicatePhones ──

    @Test
    void deduplicatePhones_removesDuplicates() {
        List<String> result = PhoneUtils.deduplicatePhones(
                Arrays.asList(TestNumber.PLACEHOLDER, "96512345678", TestNumber.PLACEHOLDER));
        assertEquals(2, result.size());
        assertEquals(TestNumber.PLACEHOLDER, result.get(0));
        assertEquals("96512345678", result.get(1));
    }

    @Test
    void deduplicatePhones_preservesOrder() {
        List<String> result = PhoneUtils.deduplicatePhones(
                Arrays.asList("96512345678", TestNumber.PLACEHOLDER, "96512345678"));
        assertEquals(2, result.size());
        assertEquals("96512345678", result.get(0));
        assertEquals(TestNumber.PLACEHOLDER, result.get(1));
    }

    @Test
    void deduplicatePhones_emptyList() {
        List<String> result = PhoneUtils.deduplicatePhones(Arrays.asList());
        assertTrue(result.isEmpty());
    }
}
