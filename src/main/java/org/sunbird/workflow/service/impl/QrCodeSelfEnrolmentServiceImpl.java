package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.BadRequestException;
import org.sunbird.workflow.exception.InvalidDataInputException;
import org.sunbird.workflow.models.QrSelfEnrolRequest;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.ContentReadService;
import org.sunbird.workflow.service.QrCodeSelfEnrolmentService;
import org.sunbird.workflow.utils.AccessTokenValidator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QrCodeSelfEnrolmentServiceImpl implements QrCodeSelfEnrolmentService {

    private static final Logger logger = LogManager.getLogger(QrCodeSelfEnrolmentServiceImpl.class);

    private final BPWorkFlowServiceImpl bpWorkFlowService;
    private final WfStatusRepo wfStatusRepo;
    private final Configuration configuration;
    private final AccessTokenValidator accessTokenValidator;
    private final ContentReadService contentReadService;
    private final ObjectMapper mapper;

    /**
     * Orchestrate QR Code self-enrolment workflow with direct approval
     *
     * @param rootOrg       - Root organization from header
     * @param org           - Organization from header
     * @param userAuthToken - JWT authentication token
     * @param qrRequest     - QR self-enrolment request with courseId and batchId
     * @return Response object with enrolment result (success 200 OK or error 400 BAD_REQUEST)
     */
    @Override
    public Response enrolQrCodeBPWorkFlow(String rootOrg, String org, String userAuthToken, QrSelfEnrolRequest qrRequest) {
        logger.info("QR Code enrolment initiated for course: {}, batch: {}", qrRequest.getCourseId(), qrRequest.getBatchId());

        String userId = accessTokenValidator.fetchUserIdFromAccessToken(userAuthToken);
        if (StringUtils.isEmpty(userId)) {
            logger.warn("QR enrolment failed: Invalid or expired access token provided");
            Response response = new Response();
            response.put(Constants.ERROR_MESSAGE, Constants.INVALID_ACCESS_TOKEN_ERROR);
            response.put(Constants.STATUS, HttpStatus.UNAUTHORIZED);
            return response;
        }
        logger.debug("User ID extracted from token: {}", userId);

        validateQrRequest(qrRequest, rootOrg, org);

        String courseId = qrRequest.getCourseId();
        String batchId = qrRequest.getBatchId();

        // Course validation
        Map<String, Object> courseDetails = contentReadService.getServiceNameDetails(courseId);
        validateCourseExists(courseDetails, courseId);
        validateSelfEnrolmentEnabled(courseDetails);
        logger.debug("Course validation completed for courseId: {}", courseId);

        // Batch validation
        Map<String, Object> courseBatchDetails = bpWorkFlowService.getCurrentBatchAttributes(batchId, courseId);
        validateBatchActive(courseBatchDetails, batchId, courseId);
        validateQrEnrollmentWindow(courseBatchDetails);
        logger.debug("Batch validation completed for batchId: {}", batchId);

        // Enrollment rules validation
        validateUniqueBatchEnrollment(userId, courseId, batchId);
        logger.debug("Single batch enrollment validation passed for userId: {}", userId);

        WfRequest wfRequest = buildQrEnrolRequest(userId, courseId, batchId, courseBatchDetails);

        int totalApprovedUserCount = bpWorkFlowService.getTotalApprovedUserCount(wfRequest);
        int totalUserEnrolCount = bpWorkFlowService.getTotalUserEnrolCountForBatch(wfRequest.getApplicationId());
        logger.debug("Batch capacity check: approved={}, total enrolled={}", totalApprovedUserCount, totalUserEnrolCount);

        boolean enrolAccess = bpWorkFlowService.validateBatchEnrolment(courseBatchDetails, totalApprovedUserCount, totalUserEnrolCount,
                Constants.BP_ENROLL_STATE);
        if (!enrolAccess) {
            logger.warn("QR enrolment failed: Batch capacity exceeded for batchId: {}", batchId);
            Response response = new Response();
            response.put(Constants.ERROR_MESSAGE, configuration.getBatchFullMesg());
            response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }
        validateNoActiveWorkflow(userId, batchId);

        // QR Direct Approval: Save wf_status with APPROVED status directly
        WfStatusEntity wfStatusEntity = saveQrEnrollmentDirect(rootOrg, org, wfRequest);
        logger.info("QR enrolment record created with wfId: {}, userId: {}, courseId: {}",
                wfStatusEntity.getWfId(), userId, courseId);

        // Call enrollment logic directly (no Kafka, no workflow processing)
        bpWorkFlowService.updateEnrolmentDetails(wfRequest);
        logger.info("User enrolled successfully in blended program. UserId: {}, CourseId: {}, BatchId: {}",
                userId, courseId, batchId);

        // Build and return success response
        Response response = new Response();
        HashMap<String, Object> data = new HashMap<>();
        data.put(Constants.STATUS, Constants.APPROVED);
        data.put(Constants.WF_IDS_CONSTANT, wfStatusEntity.getWfId());
        response.put(Constants.MESSAGE, Constants.STATUS_CHANGE_MESSAGE + Constants.APPROVED);
        response.put(Constants.DATA, data);
        response.put(Constants.STATUS, HttpStatus.OK);
        logger.debug("QR enrolment response built successfully for wfId: {}", wfStatusEntity.getWfId());
        return response;
    }

    /**
     * Validate QR self-enrolment request - check required fields and headers
     *
     * @param qrRequest - QR self-enrolment request
     * @param rootOrg   - Root organization header
     * @param org       - Organization header
     * @throws InvalidDataInputException if any required field is missing
     */
    private void validateQrRequest(QrSelfEnrolRequest qrRequest, String rootOrg, String org) {
        logger.debug("Validating QR enrolment request headers and body");

        if (qrRequest == null) {
            logger.warn("QR enrolment request validation failed: Request body is null");
            throw new InvalidDataInputException(Constants.QR_REQUEST_NULL_ERROR);
        }

        if (StringUtils.isBlank(qrRequest.getCourseId())) {
            logger.warn("QR enrolment request validation failed: Course ID is missing");
            throw new InvalidDataInputException(Constants.COURSE_ID_REQUIRED_ERROR);
        }

        if (StringUtils.isBlank(qrRequest.getBatchId())) {
            logger.warn("QR enrolment request validation failed: Batch ID is missing");
            throw new InvalidDataInputException(Constants.BATCH_ID_REQUIRED_ERROR);
        }

        if (StringUtils.isBlank(rootOrg)) {
            logger.warn("QR enrolment request validation failed: Root Organization header is missing");
            throw new InvalidDataInputException(Constants.ROOT_ORG_REQUIRED_ERROR);
        }

        if (StringUtils.isBlank(org)) {
            logger.warn("QR enrolment request validation failed: Organization header is missing");
            throw new InvalidDataInputException(Constants.ORG_REQUIRED_ERROR);
        }

        logger.debug("QR request validation successful. CourseId: {}, BatchId: {}, RootOrg: {}, Org: {}",
                qrRequest.getCourseId(), qrRequest.getBatchId(), rootOrg, org);
    }

    /**
     * Validate course exists and has required details
     *
     * @param courseDetails - Course details map
     * @param courseId      - Course ID
     * @throws BadRequestException if course not found
     */
    private void validateCourseExists(Map<String, Object> courseDetails, String courseId) {
        if (MapUtils.isEmpty(courseDetails)) {
            throw new BadRequestException(String.format(Constants.COURSE_NOT_FOUND_ERROR, courseId));
        }
    }

    /**
     * Validate batch is active and enrollment period is open
     *
     * @param courseBatchDetails - Batch details map
     * @param batchId            - Batch ID
     * @param courseId           - Course ID
     * @throws BadRequestException if batch not found or enrollment period closed
     */
    private void validateBatchActive(Map<String, Object> courseBatchDetails, String batchId, String courseId) {
        if (MapUtils.isEmpty(courseBatchDetails)) {
            throw new BadRequestException(
                    String.format(Constants.BATCH_NOT_FOUND_ERROR, courseId, batchId)
            );
        }
        Date enrollmentEndDate = (Date) courseBatchDetails.get(Constants.ENROLMENT_END_DATE);
        if (enrollmentEndDate != null && enrollmentEndDate.before(new Date())) {
            throw new BadRequestException(Constants.BATCH_ENROLLMENT_PERIOD_ENDED_ERROR);
        }
    }

    /**
     * Validate QR Code enrollment is only allowed on batch start date
     *
     * @param courseBatchDetails - Batch attributes containing start date
     * @throws BadRequestException if enrollment is not on the batch start date
     */
    private void validateQrEnrollmentWindow(Map<String, Object> courseBatchDetails) {
        Date batchStartDate = (Date) courseBatchDetails.get(Constants.START_DATE);
        if (batchStartDate == null) {
            throw new BadRequestException(Constants.BATCH_START_DATE_UNAVAILABLE_ERROR);
        }

        LocalDate batchStartLocalDate = batchStartDate.toInstant()
                .atZone(ZoneId.of(configuration.getSunbirdTimeZone()))
                .toLocalDate();
        LocalDate currentLocalDate = LocalDate.now(ZoneId.of(configuration.getSunbirdTimeZone()));

        if (!batchStartLocalDate.isEqual(currentLocalDate)) {
            throw new BadRequestException(
                    String.format(Constants.QR_ENROLLMENT_DATE_ERROR, batchStartLocalDate, currentLocalDate)
            );
        }
    }

    /**
     * Validate user is not already enrolled in this batch or another active batch for the same course
     *
     * @param userId   - User ID
     * @param courseId - Course ID
     * @param batchId  - Batch ID
     * @throws BadRequestException if user already enrolled
     */
    private void validateUniqueBatchEnrollment(String userId, String courseId, String batchId) {
        logger.debug("Validating single batch enrollment - userId: {}, courseId: {}, batchId: {}", userId, courseId, batchId);

        List<Map<String, Object>> activeEnrollments = bpWorkFlowService.getActiveEnrollmentForUserAndCourse(userId, courseId);
        if (CollectionUtils.isEmpty(activeEnrollments)) {
            logger.debug("No existing enrollments found for userId: {}, courseId: {}", userId, courseId);
            return;
        }

        Map<String, Object> enrollment = activeEnrollments.get(0);
        String existingBatchId = (String) enrollment.get(Constants.BATCH_ID);

        if (batchId.equals(existingBatchId)) {
            logger.warn("User already enrolled in same batch - userId: {}, batchId: {}", userId, batchId);
            throw new BadRequestException(Constants.USER_ALREADY_ENROLLED_SAME_BATCH_ERROR);
        }

        logger.warn("User already enrolled in different batch - userId: {}, currentBatchId: {}, newBatchId: {}",
                userId, existingBatchId, batchId);
        throw new BadRequestException(
                String.format(Constants.USER_ALREADY_ENROLLED_DIFFERENT_BATCH_ERROR, existingBatchId));
    }

    /**
     * Validate no active workflow exists for user and batch
     *
     * @param userId  - User ID
     * @param batchId - Batch ID
     * @throws BadRequestException if active non-terminal workflow found
     */
    private void validateNoActiveWorkflow(String userId, String batchId) {
        logger.debug("Checking for active workflows - userId: {}, batchId: {}", userId, batchId);

        List<WfStatusEntity> existingWorkflows = wfStatusRepo.findByServiceNameAndUserIdAndApplicationId(
                Constants.BLENDED_PROGRAM_SERVICE_NAME, userId, batchId
        );

        if (!CollectionUtils.isEmpty(existingWorkflows)) {
            for (WfStatusEntity workflow : existingWorkflows) {
                if (!isTerminalStatus(workflow.getCurrentStatus())) {
                    logger.warn("Active workflow found for userId: {}, batchId: {}, status: {}",
                            userId, batchId, workflow.getCurrentStatus());
                    throw new BadRequestException(
                            String.format(Constants.ACTIVE_WORKFLOW_EXISTS_ERROR, workflow.getCurrentStatus())
                    );
                } else {
                    logger.debug("Found terminal status workflow for userId: {}, status: {}", userId, workflow.getCurrentStatus());
                }
            }
        }
        logger.debug("No active workflows found for userId: {}, batchId: {}", userId, batchId);
    }

    /**
     * Check if a workflow status is terminal (no further transitions possible)
     *
     * @param status - Workflow status
     * @return true if status is terminal
     */
    private boolean isTerminalStatus(String status) {
        return Constants.APPROVED.equalsIgnoreCase(status) ||
                Constants.REJECTED.equalsIgnoreCase(status) ||
                Constants.WITHDRAWN.equalsIgnoreCase(status) ||
                Constants.REMOVED.equalsIgnoreCase(status);
    }

    /**
     * Validate self-enrollment is enabled for the course
     *
     * @param courseDetails - Course details map
     * @throws BadRequestException if self-enrollment not enabled
     */
    private void validateSelfEnrolmentEnabled(Map<String, Object> courseDetails) {
        if (MapUtils.isEmpty(courseDetails)) {
            throw new BadRequestException(Constants.COURSE_DETAILS_UNAVAILABLE_ERROR);
        }

        Object selfEnrollment = courseDetails.get(Constants.SELF_ENROLLMENT);

        if (selfEnrollment == null || !"Yes".equalsIgnoreCase(String.valueOf(selfEnrollment))) {
            throw new BadRequestException(Constants.SELF_ENROLLMENT_NOT_ENABLED_ERROR);
        }
    }

    /**
     * Build WfRequest for QR enrollment with direct approval
     *
     * @param userId             - User ID
     * @param courseId           - Course ID
     * @param batchId            - Batch ID
     * @param courseBatchDetails - Batch details
     * @return WfRequest configured for QR enrollment
     */
    private WfRequest buildQrEnrolRequest(String userId, String courseId, String batchId,
                                          Map<String, Object> courseBatchDetails) {
        WfRequest wfRequest = new WfRequest();
        wfRequest.setUserId(userId);
        wfRequest.setCourseId(courseId);
        wfRequest.setApplicationId(batchId);
        wfRequest.setBatchName((String) courseBatchDetails.get(Constants.BATCH_NAME));
        wfRequest.setBatchStartDate((Date) courseBatchDetails.get(Constants.START_DATE));
        wfRequest.setServiceName(Constants.SELF_ENROLL_BY_QR);
        wfRequest.setState(Constants.APPROVED);  // QR direct approval, no workflow transition
        wfRequest.setAction(Constants.INITIATE);
        wfRequest.setActorUserId(userId);

        List<HashMap<String, Object>> updateFieldValues = new ArrayList<>();
        wfRequest.setUpdateFieldValues(updateFieldValues);

        return wfRequest;
    }

    /**
     * Save QR enrollment directly with APPROVED status.
     * QR self-enrollment is not a workflow, only an audit/history record.
     *
     * @param rootOrg   - Root organization
     * @param org       - Organization
     * @param wfRequest - Workflow request containing enrollment details
     * @return WfStatusEntity - Saved workflow status entity
     */
    private WfStatusEntity saveQrEnrollmentDirect(String rootOrg, String org, WfRequest wfRequest) {
        WfStatusEntity applicationStatus = new WfStatusEntity();
        String wfId = UUID.randomUUID().toString();
        logger.debug("Creating QR enrollment record with wfId: {} for user: {}", wfId, wfRequest.getUserId());

        applicationStatus.setWfId(wfId);
        applicationStatus.setApplicationId(wfRequest.getApplicationId());
        applicationStatus.setUserId(wfRequest.getUserId());
        applicationStatus.setInWorkflow(false);  // Terminal state, not in active workflow
        applicationStatus.setActorUUID(wfRequest.getActorUserId());  // User self-approves
        applicationStatus.setCreatedOn(new Date());
        applicationStatus.setCurrentStatus(Constants.APPROVED);  // Direct approval, no intermediate state
        applicationStatus.setLastUpdatedOn(new Date());
        applicationStatus.setOrg(org);
        applicationStatus.setRootOrg(rootOrg);
        applicationStatus.setServiceName("selfEnrollByQRCode");  // QR-specific service name for audit

        try {
            applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing update field values for wfId: {}", wfId, e);
        }

        applicationStatus.setDeptName(wfRequest.getDeptName());
        applicationStatus.setComment(wfRequest.getComment());
        wfRequest.setWfId(wfId);

        wfStatusRepo.save(applicationStatus);
        logger.debug("QR enrollment record persisted to database with status: APPROVED, wfId: {}", wfId);

        return applicationStatus;
    }

}
