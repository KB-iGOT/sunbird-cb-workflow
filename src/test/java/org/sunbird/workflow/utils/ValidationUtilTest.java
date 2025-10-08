package org.sunbird.workflow.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ValidationUtilTest {

    @Test
    void testIsStringNullOREmpty() {
        assertTrue(ValidationUtil.isStringNullOREmpty(null));
        assertTrue(ValidationUtil.isStringNullOREmpty(""));
        assertTrue(ValidationUtil.isStringNullOREmpty("   "));
        assertFalse(ValidationUtil.isStringNullOREmpty("test"));
    }

    @Test
    void testValidateEmailPattern_validAndInvalid() {
        assertTrue(ValidationUtil.validateEmailPattern("user@example.com"));
        assertFalse(ValidationUtil.validateEmailPattern("invalid@com"));
        assertFalse(ValidationUtil.validateEmailPattern("plainaddress"));
        assertFalse(ValidationUtil.validateEmailPattern("user@@example.com"));
    }

    @Test
    void testValidateContactPattern_validAndInvalid() {
        assertTrue(ValidationUtil.validateContactPattern("9876543210"));
        assertFalse(ValidationUtil.validateContactPattern("12345"));
        assertFalse(ValidationUtil.validateContactPattern("abcdefghij"));
        assertFalse(ValidationUtil.validateContactPattern("12345678901"));
    }

    @Test
    void testValidateDate_validPastAndFuture() {
        String validDate = new java.text.SimpleDateFormat("dd-MM-yyyy").format(new Date());
        assertTrue(ValidationUtil.validateDate(validDate));

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -70);
        String oldDate = new java.text.SimpleDateFormat("dd-MM-yyyy").format(calendar.getTime());
        assertFalse(ValidationUtil.validateDate(oldDate));

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        String futureDate = new java.text.SimpleDateFormat("dd-MM-yyyy").format(calendar.getTime());
        assertFalse(ValidationUtil.validateDate(futureDate));

        assertFalse(ValidationUtil.validateDate("31/12/2020"));
        assertFalse(ValidationUtil.validateDate("invalid-date"));
    }

    @Test
    void testValidateExternalSystemId_validAndInvalid() {
        assertTrue(ValidationUtil.validateExternalSystemId("ABC-123"));
        assertTrue(ValidationUtil.validateExternalSystemId("abcDEF"));
        assertFalse(ValidationUtil.validateExternalSystemId("abc_123"));
        assertFalse(ValidationUtil.validateExternalSystemId("a".repeat(31))); // too long
    }

    @Test
    void testValidateExternalSystem_validAndInvalid() {
        assertTrue(ValidationUtil.validateExternalSystem("External-System 1"));
        assertTrue(ValidationUtil.validateExternalSystem("External.System"));
        assertFalse(ValidationUtil.validateExternalSystem("123456"));
        assertFalse(ValidationUtil.validateExternalSystem("Invalid_Char!"));
    }

    @Test
    void testValidateFullName_validAndInvalid() {
        assertTrue(ValidationUtil.validateFullName("John"));
        assertTrue(ValidationUtil.validateFullName("John Doe"));
        assertTrue(ValidationUtil.validateFullName("O'Connor"));
        assertFalse(ValidationUtil.validateFullName("John."));
        assertFalse(ValidationUtil.validateFullName("John\nDoe"));
        assertFalse(ValidationUtil.validateFullName("John123"));
    }

    @Test
    void testValidateTag_validAndInvalidTags() {
        List<String> validTags = Arrays.asList("Education", "Skill, Tag");
        assertTrue(ValidationUtil.validateTag(validTags));

        List<String> invalidTags = Arrays.asList("ValidTag", "Invalid@Tag");
        assertFalse(ValidationUtil.validateTag(invalidTags));
    }

    @Test
    void testValidateEmployeeId_validAndInvalid() {
        assertTrue(ValidationUtil.validateEmployeeId("EMP123"));
        assertTrue(ValidationUtil.validateEmployeeId("12345"));
        assertFalse(ValidationUtil.validateEmployeeId("emp-001"));
        assertFalse(ValidationUtil.validateEmployeeId("a".repeat(31)));
    }

    @Test
    void testValidateRegexPatternWithNoSpecialCharacter_validAndInvalid() {
        assertTrue(ValidationUtil.validateRegexPatternWithNoSpecialCharacter("Valid 123 (A)"));
        assertTrue(ValidationUtil.validateRegexPatternWithNoSpecialCharacter("Name-Value"));
        assertFalse(ValidationUtil.validateRegexPatternWithNoSpecialCharacter("Invalid@Value"));
        assertFalse(ValidationUtil.validateRegexPatternWithNoSpecialCharacter("Hello#"));
    }

    @Test
    void testValidatePinCode_validAndInvalid() {
        assertTrue(ValidationUtil.validatePinCode("560001"));
        assertFalse(ValidationUtil.validatePinCode("5600"));
        assertFalse(ValidationUtil.validatePinCode("5600012"));
        assertFalse(ValidationUtil.validatePinCode("ABCDE1"));
    }
}
