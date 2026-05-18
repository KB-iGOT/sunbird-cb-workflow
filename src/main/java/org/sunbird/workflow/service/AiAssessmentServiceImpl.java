package org.sunbird.workflow.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.ApplicationException;
import org.sunbird.workflow.exception.BadRequestException;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.impl.WorkflowServiceImpl;
import org.sunbird.workflow.utils.AccessTokenValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datastax.dse.driver.internal.core.graph.SearchPredicate.token;

@Service
public class AiAssessmentServiceImpl extends WorkflowServiceImpl {

    private static final Logger log = LogManager.getLogger(AiAssessmentServiceImpl.class);

    private final AccessTokenValidator accessTokenValidator;
    private final Configuration configuration;
    private final WfStatusRepo wfStatusRepo;
    private final UserProfileWfService userProfileWfService;

    public AiAssessmentServiceImpl(AccessTokenValidator accessTokenValidator, Configuration configuration, WfStatusRepo wfStatusRepo, UserProfileWfService userProfileWfService) {
        this.accessTokenValidator = accessTokenValidator;
        this.configuration = configuration;
        this.wfStatusRepo = wfStatusRepo;
        this.userProfileWfService = userProfileWfService;
    }

    public Response aiAssessmentWorkflowTransition(String rootOrg, String org, WfRequest wfRequest, String token) {
        java.util.List<String> actorRoles = accessTokenValidator.fetchUserRolesFromToken(token);
        log.info("Actor roles: {}", actorRoles);
        validateRoles(wfRequest.getAction(), actorRoles);
        return workflowTransition(rootOrg, org, wfRequest);
    }

    private void validateRoles(String action, List<String> actorRoles) {
        List<String> initiateRoles = configuration.getAiAssessmentInitiateRoles();
        List<String> approveRejectRoles = configuration.getAiAssessmentApproveRejectRoles();

        if (Constants.INITIATE.equalsIgnoreCase(action)) {
            boolean hasRole = actorRoles.stream()
                    .anyMatch(initiateRoles::contains);
            if (!hasRole) {
                log.error("Unauthorized INITIATE by roles: {}", actorRoles);
                throw new BadRequestException(
                        "Roles " + initiateRoles +
                                " are allowed to initiate AI Assessment requests");
            }

        } else if (Constants.APPROVE.equalsIgnoreCase(action)
                || Constants.REJECT.equalsIgnoreCase(action)) {
            boolean hasRole = actorRoles.stream()
                    .anyMatch(approveRejectRoles::contains);
            if (!hasRole) {
                log.error("Unauthorized {} by roles: {}", action, actorRoles);
                throw new BadRequestException(
                        "Roles " + approveRejectRoles +
                                " are allowed to approve or reject " +
                                "AI Assessment requests");
            }

        } else {
            throw new BadRequestException("Invalid action: " + action);
        }
    }

    public Response fetchAiAssessement(String token, SearchCriteria criteria) {
        String actorUserId = accessTokenValidator.fetchUserIdFromAccessToken(token);
        if (StringUtils.isEmpty(actorUserId)) {
            throw new BadRequestException("Invalid or expired token");
        }

        List<String> actorRoles = accessTokenValidator.fetchUserRolesFromToken(token);
        log.info("Actor: {} roles: {}", actorUserId, actorRoles);

        List<String> approveRejectRoles = configuration.getAiAssessmentApproveRejectRoles();
        boolean hasRole = actorRoles.stream().anyMatch(approveRejectRoles::contains);
        if (!hasRole) {
            throw new BadRequestException("Only SPV Publisher can access AI Assessment requests");
        }
        return getAiAssessmentRequests(criteria);
    }

    public Response getAiAssessmentRequestByUserId(String token) {
        try {
            validateTokenAndRoles(token, configuration.getAiAssessmentInitiateRoles());
            String userId = accessTokenValidator.fetchUserIdFromAccessToken(token);
            if (StringUtils.isEmpty(userId)) {
                throw new BadRequestException("Invalid or expired token");
            }
            List<WfStatusEntity> entities = wfStatusRepo
                    .findByUserIdAndServiceNameAndInWorkflow(
                            userId,
                            Constants.AI_ASSESSMENT_SERVICE_NAME,
                            true);

            if (CollectionUtils.isEmpty(entities)) {
                Response response = new Response();
                response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
                response.put(Constants.DATA, new ArrayList<>());
                response.put(Constants.COUNT, 0);
                response.put(Constants.STATUS, HttpStatus.OK);
                return response;
            }

            Map<String, List<WfStatusEntity>> groupedEntities = entities
                    .stream()
                    .collect(Collectors.groupingBy(
                            WfStatusEntity::getApplicationId));

            List<Map<String, Object>> userProfiles = userProfileWfService.enrichUserData(groupedEntities, null);

            Response response = new Response();
            response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
            response.put(Constants.DATA, userProfiles);
            response.put(Constants.COUNT, userProfiles.size());
            response.put(Constants.STATUS, HttpStatus.OK);
            return response;

        } catch (Exception e) {
            log.error("Error fetching AI Assessment request for userId: {}", e);
            throw new ApplicationException(
                    "Error fetching AI Assessment request for user", e);
        }
    }

    private String validateTokenAndRoles(String token, List<String> allowedRoles) {
        String actorUserId = accessTokenValidator
                .fetchUserIdFromAccessToken(token);
        if (StringUtils.isEmpty(actorUserId)) {
            throw new BadRequestException("Invalid or expired token");
        }

        List<String> actorRoles = accessTokenValidator
                .fetchUserRolesFromToken(token);
        log.info("Actor: {} roles: {}", actorUserId, actorRoles);

        boolean hasRole = actorRoles.stream()
                .anyMatch(allowedRoles::contains);
        if (!hasRole) {
            throw new BadRequestException(
                    "Not authorized to perform this action");
        }

        return actorUserId;
    }
}
