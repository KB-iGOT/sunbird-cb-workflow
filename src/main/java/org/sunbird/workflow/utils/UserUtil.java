package org.sunbird.workflow.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.service.impl.RequestServiceImpl;
import org.sunbird.workflow.service.impl.WorkflowServiceImpl;

import java.util.Map;
@Component
public class UserUtil {

    Logger log = LogManager.getLogger(UserUtil.class);

    @Autowired
    private Configuration configuration;

    @Autowired
    private RequestServiceImpl requestServiceImpl;

    @Autowired
    private ObjectMapper mapper;

    public Map<String, Object> userProfileRead(String userId) {
        log.info("WorkflowServiceImpl: userProfileRead for userId: {}", userId);
        String url = configuration.getLmsServiceHost() +
                configuration.getUserProfileReadEndPoint().replace(Constants.USER_ID_VALUE, userId);
        Object response = requestServiceImpl.fetchResultUsingGet(new StringBuilder(url));
        if (response == null) {
            return null;
        }
        Map<String, Object> readResponse = mapper.convertValue(response, Map.class);
        Object resultObj = readResponse.get(Constants.RESULT);
        if (!(resultObj instanceof Map)) {
            return null;
        }
        Map<String, Object> resultMap = (Map<String, Object>) resultObj;
        if (MapUtils.isEmpty(resultMap)) {
            log.error("User profile read response is empty for userId: {}", userId);
            return null;
        }
        Object userResponseObj = resultMap.get(Constants.RESPONSE);
        if (!(userResponseObj instanceof Map)) {
            log.error("User profile read response is empty for userId: {}", userId);
            return null;
        }
        return (Map<String, Object>) userResponseObj;
    }
}
