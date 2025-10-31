package org.sunbird.workflow.utils;

import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.models.SunbirdApiRespParam;

/**
 * This class will contains all the common utility methods.
 *
 * @author Manzarul
 */
public class ProjectUtil {

	static Logger logger = LogManager.getLogger(ProjectUtil.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(Constants.EMAIL_REGEX);

	public static PropertiesCache propertiesCache;

	static {
		propertiesCache = PropertiesCache.getInstance();
	}

	public static String getConfigValue(String key) {
		if (StringUtils.isNotBlank(System.getenv(key))) {
			return System.getenv(key);
		}
		return propertiesCache.readProperty(key);
	}

	public static SBApiResponse createDefaultResponse(String api) {
		SBApiResponse response = new SBApiResponse();
		response.setId(api);
		response.setVer(Constants.API_VERSION_1);
		response.setParams(new SunbirdApiRespParam(UUID.randomUUID().toString()));
		response.getParams().setStatus(Constants.SUCCESS);
		response.setResponseCode(HttpStatus.OK);
		response.setTs(DateTime.now().toString());
		return response;
	}

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

	public static Boolean isValidMobileNumber(String mobileNumber) {
		String contactNumberRegex = "^\\d{10}$";
		Pattern pat = Pattern.compile(contactNumberRegex);
		return pat.matcher(mobileNumber).matches();
	}
}