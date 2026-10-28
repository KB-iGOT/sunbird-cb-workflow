package org.sunbird.workflow.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.models.WorkflowApprovalEvent;
import org.sunbird.workflow.producer.Producer;
import org.sunbird.workflow.service.ServiceHistoryApprovalEventService;

/**
 * Publishes the {@link WorkflowApprovalEvent} previous/current pair that lets
 * cb-ext-userprofile-service update a learner's service history on a designation change or org
 * transfer. One {@code producer.push()} per approved batch; org-transfer wins over
 * designation-change when both are present.
 */
@Service
public class ServiceHistoryApprovalEventServiceImpl implements ServiceHistoryApprovalEventService {

	private final Logger logger = LoggerFactory.getLogger(ServiceHistoryApprovalEventServiceImpl.class);

	private final Configuration configuration;

	private final Producer producer;

	private final RequestServiceImpl requestServiceImpl;

	public ServiceHistoryApprovalEventServiceImpl(Configuration configuration, Producer producer,
			RequestServiceImpl requestServiceImpl) {
		this.configuration = configuration;
		this.producer = producer;
		this.requestServiceImpl = requestServiceImpl;
	}

	/**
	 * Entry point called once per batch, after the profile merge for that batch has finished.
	 *
	 * @param wfRequests the workflow requests processed in this batch
	 * @param existingUserResponse the user's profile response as read before this update
	 * @param profileDetails the user's {@code profileDetails} as read before this update
	 */
	@Override
	public void publishServiceHistoryApprovalEvent(List<WfRequest> wfRequests, Map<String, Object> existingUserResponse,
			Map<String, Object> profileDetails) {
		String previousRootOrgId = getPreviousRootOrgId(existingUserResponse);
		String previousDesignation = getPreviousDesignation(profileDetails);
		logger.debug("Resolving service-history approval event for {} workflow request(s), previousRootOrgId: {}, previousDesignation: {}",
				wfRequests.size(), previousRootOrgId, previousDesignation);
		WfRequest orgTransferRequest = findOrgTransferRequest(wfRequests);
		if (orgTransferRequest != null) {
			publishOrgTransferApprovalEventIfOrgChanged(orgTransferRequest, previousRootOrgId, previousDesignation);
			return;
		}
		WfRequest designationChangeRequest = findDesignationChangeRequest(wfRequests);
		if (designationChangeRequest != null) {
			publishDesignationApprovalEvent(designationChangeRequest, previousRootOrgId);
		}
	}

	/**
	 * Publishes the previous/current pair for an org-transfer approval, skipping it when the
	 * resolved new root org id matches what the user already had (a resubmission that didn't
	 * actually change org). Designation is carried through unchanged on both entries.
	 *
	 * @param orgTransferRequest the batch's org-transfer request
	 * @param previousRootOrgId the user's root org id before this update
	 * @param previousDesignation the user's designation before this update
	 */
	private void publishOrgTransferApprovalEventIfOrgChanged(WfRequest orgTransferRequest, String previousRootOrgId,
			String previousDesignation) {
		try {
			String newDeptName = getNewDeptName(orgTransferRequest);
			Map<String, Object> newOrgDetails = fetchOrgDetailsByName(newDeptName);
			if (MapUtils.isEmpty(newOrgDetails)) {
				logger.warn("Could not resolve org details for deptName: {}, skipping org-transfer service-history event for userId: {}",
						newDeptName, orgTransferRequest.getUserId());
				return;
			}
			String newRootOrgId = String.valueOf(newOrgDetails.get(Constants.ROOT_ORG_ID));
			if (StringUtils.isEmpty(previousRootOrgId) || previousRootOrgId.equals(newRootOrgId)) {
				logger.info("Root org id unchanged for userId: {}, skipping org-transfer service-history event", orgTransferRequest.getUserId());
				return;
			}
			logger.debug("Org transfer resolved for userId: {}: rootOrgId {} -> {}", orgTransferRequest.getUserId(), previousRootOrgId, newRootOrgId);
			String previousOrgName = resolvePreviousOrgName(previousRootOrgId);
			WorkflowApprovalEvent previousEntry = new WorkflowApprovalEvent(orgTransferRequest.getUserId(), orgTransferRequest.getWfId(),
					Constants.APPROVAL_TYPE_ORGANIZATION, Constants.ORG_TRANSFER_REQUEST,
					new WorkflowApprovalEvent.EntryDetails(Constants.ENTRY_TYPE_PREVIOUS, previousDesignation, previousOrgName, previousRootOrgId));
			WorkflowApprovalEvent currentEntry = new WorkflowApprovalEvent(orgTransferRequest.getUserId(), orgTransferRequest.getWfId(),
					Constants.APPROVAL_TYPE_ORGANIZATION, Constants.ORG_TRANSFER_REQUEST,
					new WorkflowApprovalEvent.EntryDetails(Constants.ENTRY_TYPE_CURRENT, previousDesignation, newDeptName, newRootOrgId));
			producer.push(configuration.getWorkflowServiceHistoryTopic(), List.of(previousEntry, currentEntry));
			logger.info("Published org-transfer approval event for userId: {}, wfId: {}", orgTransferRequest.getUserId(), orgTransferRequest.getWfId());
		} catch (Exception e) {
			logger.error("Error publishing org-transfer approval event for userId: {}", orgTransferRequest.getUserId(), e);
		}
	}

	/**
	 * Resolves the previous org's name by its root org id. A lookup failure is logged and
	 * swallowed rather than aborting the publish - {@code orgId} is still correct and is the
	 * consumer's primary key, so a missing name is a display gap, not a reason to skip the event.
	 *
	 * @param previousRootOrgId the previous root org id to resolve a name for
	 * @return the previous org's name, or {@code null} if it could not be resolved
	 */
	private String resolvePreviousOrgName(String previousRootOrgId) {
		Map<String, Object> previousOrgDetails = fetchOrgDetailsByRootOrgId(previousRootOrgId);
		if (MapUtils.isEmpty(previousOrgDetails)) {
			logger.warn("Could not resolve org name for previous rootOrgId: {}", previousRootOrgId);
			return null;
		}
		String previousOrgName = (String) previousOrgDetails.get(Constants.ORG_NAME);
		logger.debug("Resolved previous org name: {} for rootOrgId: {}", previousOrgName, previousRootOrgId);
		return previousOrgName;
	}

	/**
	 * @param wfRequests the workflow requests processed in this batch
	 * @return the batch's org-transfer request (by {@code requestType}), or {@code null}
	 */
	private WfRequest findOrgTransferRequest(List<WfRequest> wfRequests) {
		for (WfRequest wfRequest : wfRequests) {
			if (Constants.ORG_TRANSFER_REQUEST.equalsIgnoreCase(wfRequest.getRequestType())) {
				return wfRequest;
			}
		}
		return null;
	}

	/**
	 * @param wfRequest an org-transfer request
	 * @return the new department name carried in {@code toValue.name}
	 */
	private String getNewDeptName(WfRequest wfRequest) {
		HashMap<String, Object> toValue = (HashMap<String, Object>) wfRequest.getUpdateFieldValues().get(0).get(Constants.TO_VALUE);
		return (String) toValue.get(Constants.NAME);
	}

	/**
	 * Publishes the previous/current pair for a DESIGNATION_CHANGE approval. The previous entry
	 * carries the {@code fromValue} designation and the current entry the {@code toValue}
	 * designation; both carry the same org, since this request type does not move the org.
	 *
	 * @param designationChangeRequest the batch's designation-change request
	 * @param previousRootOrgId the user's root org id before this update, unchanged by this
	 *            request type and carried on both entries
	 */
	private void publishDesignationApprovalEvent(WfRequest designationChangeRequest, String previousRootOrgId) {
		try {
			String previousDesignation = getFromValueDesignation(designationChangeRequest);
			String newDesignation = getToValueDesignation(designationChangeRequest);
			String deptName = designationChangeRequest.getDeptName();
			logger.debug("Designation change resolved for userId: {}: {} -> {}", designationChangeRequest.getUserId(),
					previousDesignation, newDesignation);
			WorkflowApprovalEvent previousEntry = new WorkflowApprovalEvent(designationChangeRequest.getUserId(), designationChangeRequest.getWfId(),
					Constants.APPROVAL_TYPE_DESIGNATION, Constants.DESIGNATION_CHANGE,
					new WorkflowApprovalEvent.EntryDetails(Constants.ENTRY_TYPE_PREVIOUS, previousDesignation, deptName, previousRootOrgId));
			WorkflowApprovalEvent currentEntry = new WorkflowApprovalEvent(designationChangeRequest.getUserId(), designationChangeRequest.getWfId(),
					Constants.APPROVAL_TYPE_DESIGNATION, Constants.DESIGNATION_CHANGE,
					new WorkflowApprovalEvent.EntryDetails(Constants.ENTRY_TYPE_CURRENT, newDesignation, deptName, previousRootOrgId));
			producer.push(configuration.getWorkflowServiceHistoryTopic(), List.of(previousEntry, currentEntry));
			logger.info("Published designation approval event for userId: {}, wfId: {}",
					designationChangeRequest.getUserId(), designationChangeRequest.getWfId());
		} catch (Exception e) {
			logger.error("Error publishing designation approval event for userId: {}", designationChangeRequest.getUserId(), e);
		}
	}

	/**
	 * @param wfRequests the workflow requests processed in this batch
	 * @return the batch's designation-change request (by {@code requestType}), or {@code null}
	 */
	private WfRequest findDesignationChangeRequest(List<WfRequest> wfRequests) {
		for (WfRequest wfRequest : wfRequests) {
			if (Constants.DESIGNATION_CHANGE.equalsIgnoreCase(wfRequest.getRequestType())) {
				return wfRequest;
			}
		}
		return null;
	}

	/**
	 * @param wfRequest a designation-change request
	 * @return the prior ({@code fromValue}) designation, or {@code null} if missing
	 */
	private String getFromValueDesignation(WfRequest wfRequest) {
		if (CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
			return null;
		}
		HashMap<String, Object> fromValue = (HashMap<String, Object>) wfRequest.getUpdateFieldValues().get(0).get(Constants.FROM_VALUE);
		return MapUtils.isEmpty(fromValue) ? null : (String) fromValue.get(Constants.DESIGNATION);
	}

	/**
	 * @param wfRequest a designation-change request
	 * @return the newly approved ({@code toValue}) designation, or {@code null} if missing
	 */
	private String getToValueDesignation(WfRequest wfRequest) {
		if (CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
			return null;
		}
		HashMap<String, Object> toValue = (HashMap<String, Object>) wfRequest.getUpdateFieldValues().get(0).get(Constants.TO_VALUE);
		return MapUtils.isEmpty(toValue) ? null : (String) toValue.get(Constants.DESIGNATION);
	}

	/**
	 * @param existingUserResponse the user's profile response as read before this update
	 * @return the previous root org id, or {@code null} if not present
	 */
	private String getPreviousRootOrgId(Map<String, Object> existingUserResponse) {
		Map<String, Object> rootOrg = (Map<String, Object>) existingUserResponse.get(Constants.ROOT_ORG_CONSTANT);
		return MapUtils.isEmpty(rootOrg) ? null : (String) rootOrg.get(Constants.ROOT_ORG_ID);
	}

	/**
	 * @param profileDetails the user's {@code profileDetails} as read before this update
	 * @return the previous designation, or {@code null} if not present
	 */
	private String getPreviousDesignation(Map<String, Object> profileDetails) {
		if (MapUtils.isEmpty(profileDetails)) {
			return null;
		}
		List<Map<String, Object>> professionalDetailsList = (List<Map<String, Object>>) profileDetails.get(Constants.PROFESSIONAL_DETAILS);
		if (CollectionUtils.isEmpty(professionalDetailsList)) {
			return null;
		}
		return (String) professionalDetailsList.get(0).get(Constants.DESIGNATION);
	}

	/**
	 * Looks up the newly approved org by name, for an org-transfer request.
	 *
	 * @param deptName the department/org name to search for
	 * @return the first matching org record, or an empty map if unresolved
	 */
	private Map<String, Object> fetchOrgDetailsByName(String deptName) {
		return searchOrgDetails(Constants.CHANNEL, deptName);
	}

	/**
	 * Looks up the previous org by root org id - the org-transfer payload only ever carries the
	 * user's previous root org id, never its name.
	 *
	 * @param rootOrgId the root org id to search for
	 * @return the first matching org record, or an empty map if unresolved
	 */
	private Map<String, Object> fetchOrgDetailsByRootOrgId(String rootOrgId) {
		return searchOrgDetails(Constants.ROOT_ORG_ID, rootOrgId);
	}

	/**
	 * Shared org-search call behind {@link #fetchOrgDetailsByName(String)} and
	 * {@link #fetchOrgDetailsByRootOrgId(String)}. Deliberately not shared with the
	 * identically-shaped lookup in {@link UserProfileWfServiceImpl} - kept local to this class
	 * per explicit instruction.
	 *
	 * @param filterKey the org-search filter field, e.g. {@link Constants#CHANNEL} or
	 *            {@link Constants#ROOT_ORG_ID}
	 * @param filterValue the value to filter on
	 * @return the first matching org record, or an empty map if unresolved
	 */
	private Map<String, Object> searchOrgDetails(String filterKey, String filterValue) {
		logger.debug("Fetching org details for {}: {}", filterKey, filterValue);
		if (StringUtils.isEmpty(filterValue)) {
			return Map.of();
		}
		Map<String, Object> reqMap = new HashMap<>();
		reqMap.put(Constants.FILTERS, Collections.singletonMap(filterKey, filterValue));
		Map<String, Object> requestObj = new HashMap<>();
		requestObj.put(Constants.REQUEST, reqMap);
		HashMap<String, String> headersValue = new HashMap<>();
		headersValue.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
		try {
			StringBuilder builder = new StringBuilder(configuration.getLmsServiceHost());
			builder.append(configuration.getLmsOrgSearchEndPoint());
			Map<String, Object> orgDetails = (Map<String, Object>) requestServiceImpl
					.fetchResultUsingPost(builder, requestObj, Map.class, headersValue);
			if (MapUtils.isEmpty(orgDetails) || !Constants.OK.equalsIgnoreCase((String) orgDetails.get(Constants.RESPONSE_CODE))) {
				logger.warn("Org search returned no result for {}: {}", filterKey, filterValue);
				return Map.of();
			}
			Map<String, Object> result = (Map<String, Object>) orgDetails.get(Constants.RESULT);
			Map<String, Object> response = MapUtils.isNotEmpty(result) && result.get(Constants.RESPONSE) instanceof Map
					? (Map<String, Object>) result.get(Constants.RESPONSE) : Map.of();
			List<Map<String, Object>> contentList = response.get(Constants.CONTENT) instanceof List
					? (List<Map<String, Object>>) response.get(Constants.CONTENT) : List.of();
			return CollectionUtils.isNotEmpty(contentList) ? contentList.get(0) : Map.of();
		} catch (Exception e) {
			logger.error("Error searching for org details with {}: {}", filterKey, filterValue, e);
		}
		return Map.of();
	}
}
