package org.sunbird.workflow.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.BadRequestException;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.impl.WorkflowServiceImpl;
import org.sunbird.workflow.utils.AccessTokenValidator;

import java.util.List;
@Service
public class AiAssessmentServiceImpl extends WorkflowServiceImpl {

    private static final Logger log = LogManager.getLogger(AiAssessmentServiceImpl.class);

    private final AccessTokenValidator accessTokenValidator;
    private final Configuration configuration;

    public AiAssessmentServiceImpl(AccessTokenValidator accessTokenValidator, Configuration configuration) {
        this.accessTokenValidator = accessTokenValidator;
        this.configuration = configuration;
    }

    public Response validate(String rootOrg, String org, WfRequest wfRequest, String token) {
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

    public Response validateSearchAccess(String token, SearchCriteria criteria) {
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
}
