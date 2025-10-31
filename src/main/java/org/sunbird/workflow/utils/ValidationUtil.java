package org.sunbird.workflow.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.workflow.config.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;


public class ValidationUtil {


	public static final String DEFAULT_BULK_UPLOAD_VERIFICATION_REGEX = "^[a-zA-Z\\s,]+$";
    private static final Pattern FULL_NAME_PATTERN =
            Pattern.compile(Constants.FULL_NAME_REGEX);

    private static final Pattern EXTERNAL_SYSTEM_ID_PATTERN =
            Pattern.compile(Constants.EXTERNAL_SYSTEM_ID_REGEX);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(Constants.EMAIL_REGEX);
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtil.class);


    public static boolean isStringNullOREmpty(String value) {
		return (value == null || "".equals(value.trim()));
	}

    public static boolean validateEmailPattern(String email) {
        if (email == null || email.length() > 320) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

	public static Boolean validateContactPattern(String contactNumber) {
		String contactNumberRegex = "^\\d{10}$";
		Pattern pat = Pattern.compile(contactNumberRegex);
		if (pat.matcher(contactNumber).matches()) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	public static Boolean validateDate(String dateString){
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		dateFormat.setLenient(false);
		try {
			Date todaysDate = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.YEAR, -65);
			Date pastDate = calendar.getTime();
			Date date = dateFormat.parse(dateString);
			return date.after(pastDate) && (date.before(todaysDate) || date.equals(todaysDate));
		} catch (ParseException e) {
            logger.warn("Invalid date format for input '{}': {}", dateString, e.getMessage(), e);
		}
		return false;
	}

    public static boolean validateExternalSystemId(String externalSystemId) {
        if (externalSystemId == null) {
            return false;
        }
        return EXTERNAL_SYSTEM_ID_PATTERN.matcher(externalSystemId).matches();
    }

	public static Boolean validateExternalSystem(String externalSystem) {
		return externalSystem.matches("^(?=.*[a-zA-Z .-])[a-zA-Z0-9 .-]{1,255}$"); // Allow only alphanumeric, alphabets and restrict if only numeric character
	}

    public static boolean validateFullName(String firstName) {
        if (firstName == null) {
            return false;
        }
        return FULL_NAME_PATTERN.matcher(firstName).matches();
    }

	public static Boolean validateTag(List<String> tags) {
		String regEx = DEFAULT_BULK_UPLOAD_VERIFICATION_REGEX;
		for (String tag : tags) {
			if (!tag.matches(regEx)) {
				return false;
			}
		}
		return true;
	}

	public static Boolean validateEmployeeId(String employeeId) {
		return employeeId.matches("^[a-zA-Z0-9]{1,30}$"); // Allow alphabetic, alphanumeric, and numeric character(s).
	}

	public static Boolean validateRegexPatternWithNoSpecialCharacter(String regex) {
		return regex.matches("^[a-zA-Z0-9 \\-()]*$");
	}

	public static Boolean validatePinCode(String regex) {
		return regex.matches("^[0-9]{6}$");
	}
}