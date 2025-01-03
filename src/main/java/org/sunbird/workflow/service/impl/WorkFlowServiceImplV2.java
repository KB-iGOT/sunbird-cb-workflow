package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.ApplicationException;
import org.sunbird.workflow.exception.BadRequestException;
import org.sunbird.workflow.exception.InvalidDataInputException;
import org.sunbird.workflow.models.*;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.producer.Producer;
import org.sunbird.workflow.service.WorkFlowServiceV2;
import org.sunbird.workflow.utils.CassandraOperation;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkFlowServiceImplV2 implements WorkFlowServiceV2 {


    Logger logger = LogManager.getLogger(WorkFlowServiceImplV2.class);

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CassandraOperation cassandraOperation;

    @Autowired
    private Configuration configuration;

    @Autowired
    private RequestServiceImpl requestServiceImpl;

    @Autowired
    private Producer producer;

    @Override
    public Response workflowTransition(String rootOrg, String org, Map<String, Object> requestBody) {
        Response response = new Response();
        List<Object> data = new ArrayList<>();

        try {
            String modifiedBy = "";
            String role = "";

            Object rawRequest = requestBody.get(Constants.REQUEST);
            if (rawRequest instanceof List) {
                List<WfRequest> wfRequestList = mapper.convertValue(rawRequest, new TypeReference<List<WfRequest>>() {
                });
                logger.debug("Received {} workflow requests", wfRequestList.size());

                Map<String, List<WfRequest>> wfRequestsGroupedByUserId = wfRequestList.stream()
                        .filter(wfRequest -> wfRequest.getUserId() != null)
                        .collect(Collectors.groupingBy(WfRequest::getUserId));

                String serviceName = wfRequestList.stream()
                        .map(WfRequest::getServiceName)
                        .filter(wfServiceName -> wfServiceName != null && !wfServiceName.isEmpty())
                        .findFirst()
                        .orElse(null);

                if (!StringUtils.isEmpty(serviceName)) {
                    for (String userId : wfRequestsGroupedByUserId.keySet()) {
                        logger.info("Processing workflow requests for userId: {}", userId);

                        List<Object> wfRequestsForEvent = new ArrayList<>();
                        for (WfRequest wfRequest : wfRequestsGroupedByUserId.get(userId)) {
                            logger.debug("Processing workflow request with wfId: {}", wfRequest.getWfId());
                            Map<String, Object> responseData = new HashMap<>();
                            try {
                                if (!StringUtils.isEmpty(wfRequest.getServiceName())) {
                                    if (StringUtils.isEmpty(wfRequest.getWfId()) &&
                                            wfRequest.getServiceName().equalsIgnoreCase(Constants.PROFILE_SERVICE_NAME)) {

                                        if (handleProfileServiceWorkflow(wfRequest, responseData)) {
                                            logger.warn("Same Profile approval request is already in workflow for request ID: {}", wfRequest.getWfId());
                                            break;
                                        }
                                    }
                                    Map<String, String> changeStatusResponse = changeStatus(rootOrg, org, wfRequest, modifiedBy, role);
                                    String changedStatus = changeStatusResponse.get(Constants.STATUS);
                                    logger.info("Changed status to '{}' for workflow ID: {}", changedStatus, changeStatusResponse.get(Constants.WF_ID_CONSTANT));

                                    responseData.put(Constants.STATUS, changedStatus);
                                    responseData.put(Constants.WF_ID_CONSTANT, changeStatusResponse.get(Constants.WF_ID_CONSTANT));
                                }
                                wfRequestsForEvent.add(wfRequest);
                            } catch (Exception e) {
                                logger.error("Error processing workflow request ID: {}", wfRequest.getWfId(), e);
                                responseData.put(Constants.STATUS, Constants.FAILED);
                                responseData.put(Constants.ERROR_MESSAGE, e.getMessage());
                                responseData.put(Constants.WF_ID_CONSTANT, !StringUtils.isEmpty(wfRequest.getWfId()) ? wfRequest.getWfId() : "");
                            }
                            data.add(responseData);
                        }
                        if (!CollectionUtils.isEmpty(wfRequestsForEvent)) {
                            pushWorkflowEvents(serviceName, userId, wfRequestsForEvent);
                        }
                        logger.info("Completed workflowTransition successfully for userId: {}", userId);
                    }
                    response.put(Constants.MESSAGE, Constants.SUCCESS);
                    response.put(Constants.DATA, data);
                    response.put(Constants.STATUS, HttpStatus.OK);
                } else {
                    handleInvalidDataType(rawRequest, data, response);
                }
            } else {
                handleInvalidDataType(rawRequest, data, response);
            }
        } catch (Exception e) {
            logger.error("Error in workflowTransition for rootOrg: {}, org: {}", rootOrg, org, e);
            handleProcessingError(data, response);
        }
        return response;
    }

    private void handleProcessingError(List<Object> data, Response response) {
        response.put(Constants.MESSAGE, "Failed to process workflow transition");
        response.put(Constants.DATA, data);
        response.put(Constants.STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void handleInvalidDataType(Object rawRequest, List<Object> data, Response response) {
        logger.error("Invalid data type for workflow requests: {}", rawRequest.getClass().getName());
        response.put(Constants.MESSAGE, "Invalid data type for workflow requests");
        response.put(Constants.DATA, data);
        response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
    }

    private void pushWorkflowEvents(String serviceName, String userId, List<Object> wfRequestsForEvent) {
        try {
            Map<String, Object> workflowEvent = new HashMap<>();
            workflowEvent.put(Constants.USER_ID, userId);
            workflowEvent.put(Constants.SERVICE_NAME, serviceName);
            workflowEvent.put(Constants.WORKFLOW_REQUESTS, wfRequestsForEvent);

            logger.debug("Pushing workflow event to notification topic for userId: {}", userId);
            producer.push(configuration.getWorkFlowNotificationTopicV2(), workflowEvent);

            logger.debug("Pushing workflow event to application topic for userId: {}", userId);
            producer.push(configuration.getWorkflowApplicationTopicV2(), workflowEvent);
        } catch (Exception e) {
            logger.error("Error pushing workflow events for userId: {}", userId, e);
        }
    }


    private boolean handleProfileServiceWorkflow(WfRequest wfRequest, Map<String, Object> responseData) {
        try {
            Map<String, Object> wfRequestExistResponse = isWFRequestExist(wfRequest);
            boolean isWFRequestExist = (boolean) wfRequestExistResponse.get(Constants.IS_WF_REQUEST_EXIST);

            if (isWFRequestExist) {
                responseData.put(Constants.STATUS, Constants.SEND_FOR_APPROVAL);
                responseData.put(Constants.WF_ID_CONSTANT, wfRequestExistResponse.get(Constants.WF_ID_CONSTANT));
                return true;
            } else {
                addRequestTypeInProfileWF(wfRequest);
            }
        } catch (IOException e) {
            String errorMessage = String.format("Error while validating WF request for user: %s. Exception message: %s", wfRequest.getUserId(), e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
        return false;
    }

    private Map<String, Object> isWFRequestExist(WfRequest wfRequest) throws IOException {

        if (wfRequest == null || wfRequest.getUpdateFieldValues() == null || wfRequest.getUpdateFieldValues().isEmpty()) {
            throw new IllegalArgumentException("Invalid WfRequest or updateFieldValues");
        }

        Map<String, Object> response = new HashMap<>();
        String newRequestKey = ((Map<String, Object>) wfRequest.getUpdateFieldValues().get(0).get(Constants.TO_VALUE)).keySet().iterator().next();
        List<WfStatusEntity> wfStatusEntities = wfStatusRepo.getPendingRequests(wfRequest.getUserId(), wfRequest.getServiceName(), Constants.SEND_FOR_APPROVAL);

        for (WfStatusEntity wfStatusEntity : wfStatusEntities) {
            String existingRequestKey = getKeyFromUpdateFieldValues(wfStatusEntity.getUpdateFieldValues());
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(existingRequestKey) && existingRequestKey.equalsIgnoreCase(newRequestKey)) {
                response.put(Constants.IS_WF_REQUEST_EXIST, true);
                response.put(Constants.WF_ID_CONSTANT, wfStatusEntity.getWfId());
                return response;
            }
        }
        response.put(Constants.IS_WF_REQUEST_EXIST, false);
        response.put(Constants.WF_ID_CONSTANT, wfRequest.getWfId());
        return response;
    }

    private String getKeyFromUpdateFieldValues(String updateFieldValues) throws IOException {
        List<Map<String, Object>> updatedFieldValues = mapper.readValue(updateFieldValues, new TypeReference<List<Map<String, Object>>>() {
        });
        Map<String, Object> toValue = (Map<String, Object>) updatedFieldValues.get(0).get(Constants.TO_VALUE);

        if (!MapUtils.isEmpty(toValue)) {
            return toValue.keySet().iterator().next();
        } else {
            return null;
        }
    }

    private void addRequestTypeInProfileWF(WfRequest wfRequest) {
        String requestKey = ((Map<String, Object>) wfRequest.getUpdateFieldValues().get(0).get(Constants.TO_VALUE)).keySet().iterator().next();
        switch (requestKey) {
            case "group":
                wfRequest.setRequestType("GROUP_CHANGE");
                break;
            case "designation":
                wfRequest.setRequestType("DESIGNATION_CHANGE");
                break;
            case "name":
                wfRequest.setRequestType("ORG_TRANSFER");
                break;
            default:
                wfRequest.setRequestType(requestKey);
        }
    }

    private Map<String, String> changeStatus(String rootOrg, String org, WfRequest wfRequest, String userId, String role) {
        String wfId = wfRequest.getWfId();
        String nextState = null;
        Map<String, String> data = new HashMap<>();

        try {
            validateWfRequest(wfRequest);

            // Fetch existing workflow record
            WfStatusEntity applicationStatus = wfStatusRepo.findByRootOrgAndOrgAndApplicationIdAndWfId(
                    rootOrg, org, wfRequest.getApplicationId(), wfRequest.getWfId());

            // Resolve service name
            String serviceName = resolveServiceName(wfRequest, applicationStatus);

            // Set createdOn if existing status is found
            if (applicationStatus != null && applicationStatus.getCreatedOn() != null) {
                wfRequest.setCreatedOn(applicationStatus.getCreatedOn().toString());
            }

            // Workflow model and validation
            WorkFlowModel workFlowModel = getWorkFlowConfig(serviceName);
            WfStatus wfStatus = getWfStatus(wfRequest.getState(), workFlowModel);
            validateUserAndWfStatus(wfRequest, wfStatus, applicationStatus);

            // Get next state
            WfAction wfAction = getWfAction(wfRequest.getAction(), wfStatus);
            nextState = wfAction.getNextState();

            // Initialize application status if new
            if (applicationStatus == null) {
                applicationStatus = initializeApplicationStatus(rootOrg, org, wfRequest, serviceName);
                wfId = applicationStatus.getWfId();
            }

            // Update workflow status entity
            updateApplicationStatus(applicationStatus, wfRequest, nextState, userId, role, workFlowModel);

            // Handle specific fields if applicable
            handleSpecialFields(wfRequest);

        } catch (IOException e) {
            throw new ApplicationException(Constants.WORKFLOW_PARSING_ERROR_MESSAGE, e);
        }

        // Prepare response data
        data.put(Constants.STATUS, nextState);
        data.put(Constants.WF_ID_CONSTANT, wfId);
        return data;
    }

    private String resolveServiceName(WfRequest wfRequest, WfStatusEntity applicationStatus) {
        if (Constants.BLENDED_PROGRAM_SERVICE_NAME.equalsIgnoreCase(wfRequest.getServiceName())
                && applicationStatus != null && !StringUtils.isEmpty(applicationStatus.getServiceName())) {
            return applicationStatus.getServiceName();
        }
        return wfRequest.getServiceName();
    }

    private WfStatusEntity initializeApplicationStatus(String rootOrg, String org, WfRequest wfRequest, String serviceName) {
        WfStatusEntity applicationStatus = new WfStatusEntity();
        applicationStatus.setWfId(UUID.randomUUID().toString());
        applicationStatus.setServiceName(serviceName);
        applicationStatus.setUserId(wfRequest.getUserId());
        applicationStatus.setApplicationId(wfRequest.getApplicationId());
        applicationStatus.setRootOrg(rootOrg);
        applicationStatus.setOrg(org);
        applicationStatus.setCreatedOn(new Date());
        applicationStatus.setRequestType(wfRequest.getRequestType());
        wfRequest.setWfId(applicationStatus.getWfId());
        return applicationStatus;
    }

    private void updateApplicationStatus(WfStatusEntity applicationStatus, WfRequest wfRequest, String nextState,
                                         String userId, String role, WorkFlowModel workFlowModel) throws IOException {
        WfStatus nextWfStatus = getWfStatus(nextState, workFlowModel);

        applicationStatus.setLastUpdatedOn(new Date());
        applicationStatus.setCurrentStatus(nextState);
        applicationStatus.setActorUUID(wfRequest.getActorUserId());
        applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
        applicationStatus.setInWorkflow(!nextWfStatus.getIsLastState());
        applicationStatus.setDeptName(wfRequest.getDeptName());
        applicationStatus.setComment(wfRequest.getComment());
        addModificationEntry(applicationStatus, userId, wfRequest.getAction(), role);

        wfStatusRepo.save(applicationStatus);
    }

    private void handleSpecialFields(WfRequest wfRequest) {
        wfRequest.getUpdateFieldValues().stream()
                .filter(updatedValue -> updatedValue.containsKey(Constants.TO_VALUE))
                .map(updatedValue -> (Map<String, Object>) updatedValue.get(Constants.TO_VALUE))
                .findFirst()
                .ifPresent(toValue -> {
                    String fieldKey = toValue.keySet().iterator().next();
                    if (Constants.NAME.equalsIgnoreCase(fieldKey)) {
                        Map<String, Object> propertyMap = new HashMap<>();
                        propertyMap.put(Constants.ID, wfRequest.getUserId());
                        List<Map<String, Object>> userDetails = cassandraOperation.getRecordsByProperties(
                                Constants.KEYSPACE_SUNBIRD, Constants.USER_TABLE, propertyMap,
                                Arrays.asList(Constants.ROOT_ORG_ID));
                        if (!userDetails.isEmpty()) {
                            wfRequest.setPreviousRootOrgId((String) userDetails.get(0).get(Constants.USER_ROOT_ORG_ID));
                        }
                    }
                });
    }


    private void validateWfRequest(WfRequest wfRequest) {

        if (StringUtils.isEmpty(wfRequest.getState())) {
            throw new InvalidDataInputException(Constants.STATE_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getApplicationId())) {
            throw new InvalidDataInputException(Constants.APPLICATION_ID_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getActorUserId())) {
            throw new InvalidDataInputException(Constants.ACTOR_UUID_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getUserId())) {
            throw new InvalidDataInputException(Constants.USER_UUID_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getAction())) {
            throw new InvalidDataInputException(Constants.ACTION_VALIDATION_ERROR);
        }

        if (!Constants.WITHDRAW.equalsIgnoreCase(wfRequest.getAction()) && CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
            throw new InvalidDataInputException(Constants.FIELD_VALUE_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getServiceName())) {
            throw new InvalidDataInputException(Constants.WORKFLOW_SERVICENAME_VALIDATION_ERROR);
        }

    }

    public WorkFlowModel getWorkFlowConfig(String serviceName) {
        try {
            Map<String, Object> wfConfig = new HashMap<>();
            StringBuilder uri = new StringBuilder();
            switch (serviceName) {
                case Constants.PROFILE_SERVICE_NAME:
                    uri.append(configuration.getLmsServiceHost() + configuration.getProfileServiceConfigPath());
                    break;
                case Constants.USER_REGISTRATION_SERVICE_NAME:
                    uri.append(configuration.getLmsServiceHost() + configuration.getUserRegistrationServiceConfigPath());
                    break;
                case Constants.POSITION_SERVICE_NAME:
                    uri.append(configuration.getLmsServiceHost() + configuration.getPositionServiceConfigPath());
                    break;
                case Constants.ORGANISATION_SERVICE_NAME:
                    uri.append(configuration.getLmsServiceHost() + configuration.getOrgServiceConfigPath());
                    break;
                case Constants.DOMAIN_SERVICE_NAME:
                    uri.append(configuration.getLmsServiceHost() + configuration.getDomainServiceConfigPath());
                    break;
                case Constants.BLENDED_PROGRAM_SERVICE_NAME:
                    uri.append(configuration.getLmsServiceHost() + configuration.getBlendedProgramServicePath());
                    break;
                case Constants.ONE_STEP_PC_APPROVAL:
                    uri.append(configuration.getLmsServiceHost()).append(configuration.getMultilevelBPEnrolEndPoint()).append(Constants.ONE_STEP_PC_APPROVAL);
                    break;
                case Constants.ONE_STEP_MDO_APPROVAL:
                    uri.append(configuration.getLmsServiceHost()).append(configuration.getMultilevelBPEnrolEndPoint()).append(Constants.ONE_STEP_MDO_APPROVAL);
                    break;
                case Constants.TWO_STEP_MDO_AND_PC_APPROVAL:
                    uri.append(configuration.getLmsServiceHost()).append(configuration.getMultilevelBPEnrolEndPoint()).append(Constants.TWO_STEP_MDO_AND_PC_APPROVAL);
                    break;
                case Constants.TWO_STEP_PC_AND_MDO_APPROVAL:
                    uri.append(configuration.getLmsServiceHost()).append(configuration.getMultilevelBPEnrolEndPoint()).append(Constants.TWO_STEP_PC_AND_MDO_APPROVAL);
                    break;
                default:
                    break;
            }
            wfConfig = (Map<String, Object>) requestServiceImpl.fetchResultUsingGet(uri);
            Map<String, Object> result = (Map<String, Object>) wfConfig.get(Constants.RESULT);
            Map<String, Object> response = (Map<String, Object>) result.get(Constants.RESPONSE);
            Map<String, Object> wfStates = mapper.readValue((String) response.get(Constants.VALUE), Map.class);
            WorkFlowModel workFlowModel = mapper.convertValue(wfStates, new TypeReference<WorkFlowModel>() {
            });
            return workFlowModel;
        } catch (Exception e) {
            logger.error("Exception occurred while getting work flow config details!");
            throw new ApplicationException(Constants.WORKFLOW_PARSING_ERROR_MESSAGE, e);
        }
    }

    /**
     * Get Workflow Action based on given action
     *
     * @param action
     * @param wfStatus
     * @return Work flow Action
     */
    private WfAction getWfAction(String action, WfStatus wfStatus) {
        WfAction wfAction = null;
        if (ObjectUtils.isEmpty(wfStatus.getActions())) {
            throw new BadRequestException(Constants.WORKFLOW_ACTION_ERROR);
        }
        for (WfAction filterAction : wfStatus.getActions()) {
            if (action.equals(filterAction.getAction())) {
                wfAction = filterAction;
            }
        }
        if (ObjectUtils.isEmpty(wfAction)) {
            throw new BadRequestException(Constants.WORKFLOW_ACTION_ERROR);
        }
        return wfAction;
    }

    /**
     * Get the workflow State based on given state
     *
     * @param state
     * @param workFlowModel
     * @return Workflow State
     */
    private WfStatus getWfStatus(String state, WorkFlowModel workFlowModel) {
        WfStatus wfStatus = null;
        for (WfStatus status : workFlowModel.getWfstates()) {
            if (status.getState().equals(state)) {
                wfStatus = status;
                break;
            }
        }
        if (ObjectUtils.isEmpty(wfStatus)) {
            throw new BadRequestException(Constants.WORKFLOW_STATE_CHECK_ERROR);
        }
        return wfStatus;
    }

    /**
     * Validate application against workflow state
     *
     * @param wfRequest
     * @param wfStatus
     * @param applicationStatus
     */
    private void validateUserAndWfStatus(WfRequest wfRequest, WfStatus wfStatus, WfStatusEntity applicationStatus) {

        if (StringUtils.isEmpty(wfRequest.getWfId()) && !wfStatus.getStartState()) {
            throw new ApplicationException(Constants.WORKFLOW_ID_ERROR_MESSAGE);
        }
        if ((!ObjectUtils.isEmpty(applicationStatus))
                && (!wfRequest.getState().equalsIgnoreCase(applicationStatus.getCurrentStatus()))) {
            throw new BadRequestException("Application is in " + applicationStatus.getCurrentStatus()
                    + " State but trying to be move in " + wfRequest.getState() + " state!");
        }
    }

    private void addModificationEntry(WfStatusEntity applicationStatus, String userId, String action, String role) throws IOException {
        if (!StringUtils.isEmpty(userId) &&
                Arrays.asList(configuration.getModificationRecordAllowActions().split(Constants.COMMA)).contains(action)) {
            List<Map<String, Object>> historyMap = null;
            String history = applicationStatus.getModificationHistory();
            if (!StringUtils.isEmpty(history))
                historyMap = mapper.readValue(history, List.class);
            else
                historyMap = new ArrayList<>();
            Map<String, Object> newModification = new HashMap<>();
            newModification.put("modifiedDate", new Date());
            newModification.put("modifiedBy", userId);
            newModification.put("action", action);
            newModification.put("role", role);
            historyMap.add(newModification);
            applicationStatus.setModificationHistory(mapper.writeValueAsString(historyMap));
        }

    }
}
