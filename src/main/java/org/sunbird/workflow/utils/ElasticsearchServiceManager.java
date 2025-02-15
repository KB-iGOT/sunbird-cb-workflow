package org.sunbird.workflow.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Constants;

import io.micrometer.core.instrument.util.StringUtils;

@Service
public class ElasticsearchServiceManager {
    Logger logger = LogManager.getLogger(ElasticsearchServiceManager.class);

    @Value("${sunbird_user_index}")
    private String sbUserIndex;

    @Autowired
    private RestHighLevelClient client;

    public static final String _DOC = "_doc";

    public boolean upsertWfRequest(String userId, List<String> wfRequests) {
        try {
            // Create or update the document
            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.WF_REQUESTS_KEY, wfRequests);

            UpdateRequest updateRequest = new UpdateRequest(sbUserIndex, _DOC, userId)
                    .doc(updates)
                    .upsert(new IndexRequest(sbUserIndex).id(userId).source(updates));

            client.update(updateRequest, RequestOptions.DEFAULT);
            logger.info("WfRequests Upsert successful for userId: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to upsert wfRequests for userId: {}", userId, e);
            return false;
        }
        return true;
    }

    /**
     * Removes a specific wfRequest from the wfRequests list in user_alias index.
     */
    public boolean removeWfRequest(String userId, String wfRequestToRemove) {
        try {
            // Fetch the existing document
            GetResponse getResponse = client.get(new org.elasticsearch.action.get.GetRequest(sbUserIndex, _DOC, userId),
                    RequestOptions.DEFAULT);

            if (!getResponse.isExists()) {
                logger.error("Failed to find user information from ES for Id: {}", userId);
                return false;
            }

            Map<String, Object> source = getResponse.getSourceAsMap();
            List<String> wfRequests = (List<String>) source.get(Constants.WF_REQUESTS_KEY);

            if (wfRequests == null || !wfRequests.contains(wfRequestToRemove)) {
                logger.error("WFRequests object is empty or doesn't contain wf_id: {} for user: {}", wfRequestToRemove,
                        userId);
                return false;
            }

            // Remove the request
            wfRequests.remove(wfRequestToRemove);
            source.put(Constants.WF_REQUESTS_KEY, wfRequests);

            // Update document
            UpdateRequest updateRequest = new UpdateRequest(sbUserIndex, _DOC, userId).doc(source);
            client.update(updateRequest, RequestOptions.DEFAULT);

            logger.info("wfRequest with id: {} is removed successfully from user: {}", wfRequestToRemove, userId);
        } catch (IOException e) {
            logger.error("Failed to remove wf_id: {} from WFRequests for user: {}", wfRequestToRemove, userId, e);
            return false;
        }
        return true;
    }

    public long searchUsers(String queryString, String rootOrgId, int from, int size, List<String> userInfo,
            String deptName, List<String> requestTypes) {
        long totalHits = 0;
        try {
            // Construct the search request
            SearchRequest searchRequest = new SearchRequest(sbUserIndex);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.from(from);
            sourceBuilder.size(size);

            // Build the bool query
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // if query is email address then check against
            // profileDetails.personalDetails.primaryEmail
            // if query is valid mobile number then check against
            // profileDetails.personalDetails.mobile
            // if query is none of above use SimpleQueryStringBuilder to get data
            if (ProjectUtil.isValidEmail(queryString)) {
                boolQuery.must(QueryBuilders.termQuery(Constants.PROFILE_PRIMARY_EMAIL_FIELD, queryString));
            } else if (ProjectUtil.isValidMobileNumber(queryString)) {
                boolQuery.must(QueryBuilders.termQuery(Constants.PROFILE_PHONE_NUMBER_FILED, queryString));
            } else {
                SimpleQueryStringBuilder simpleQuery = QueryBuilders.simpleQueryStringQuery(queryString)
                        .defaultOperator(org.elasticsearch.index.query.Operator.OR)
                        .analyzeWildcard(false)
                        .autoGenerateSynonymsPhraseQuery(true)
                        .fuzzyPrefixLength(0)
                        .fuzzyMaxExpansions(50)
                        .fuzzyTranspositions(true)
                        .boost(1.0f);

                for (String field : Constants.USER_DEFAULT_SEARCH_FIELDS) {
                    simpleQuery.field(field);
                }

                boolQuery.must(simpleQuery);
            }

            if (requestTypes.contains(Constants.ORG_TRANSFER_REQUEST)) {
                boolQuery.must(QueryBuilders.termQuery(Constants.WF_TRANSFER_REQUEST_DEPTNAME_KEY, deptName));
            } else {
                boolQuery.must(QueryBuilders.termQuery(Constants.WF_PROFILE_REQUEST_DEPTNAME_KEY, deptName));
            }

            sourceBuilder.query(boolQuery);

            sourceBuilder.fetchSource(
                    new String[] { Constants.ID },
                    new String[] {});

            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            for (SearchHit hit : searchResponse.getHits().getHits()) {
                Map<String, Object> sourceMap = (Map<String, Object>) hit.getSourceAsMap();
                userInfo.add((String) sourceMap.get(Constants.ID));
            }
            totalHits = searchResponse.getHits().getTotalHits();
        } catch (IOException e) {
            logger.error("Failed to query ES to find records for given query.", e);
        }
        return totalHits;
    }

    public boolean updateWfRequest(String userId, String uuid, boolean append) {
        try {
            // Prepare parameters for the script
            Map<String, Object> params = new HashMap<>();
            params.put("uuid", uuid);

            // Choose the script source based on the operation type.
            // If 'append' is true, then add the UUID if not already present.
            // If 'append' is false, then remove the UUID if it exists.
            String scriptSource;
            if (append) {
                scriptSource = "if (ctx._source.wfRequests == null) { " +
                        "  ctx._source.wfRequests = new ArrayList(); " +
                        "} " +
                        "if (!ctx._source.wfRequests.contains(params.uuid)) { " +
                        "  ctx._source.wfRequests.add(params.uuid); " +
                        "}";
            } else {
                scriptSource = "if (ctx._source.wfRequests != null && ctx._source.wfRequests.contains(params.uuid)) { "
                        +
                        "  ctx._source.wfRequests.remove(ctx._source.wfRequests.indexOf(params.uuid)); " +
                        "}";
            }

            // Create an inline Painless script with the chosen source
            Script script = new Script(ScriptType.INLINE, "painless", scriptSource, params);

            // Define upsert content: if the document does not exist,
            // create it with wfRequests initialized to a list containing the UUID.
            // (For a remove operation, the upsert is less important since there’s nothing
            // to remove.)
            Map<String, Object> upsertContent = new HashMap<>();
            upsertContent.put(Constants.WF_REQUESTS_KEY, Collections.singletonList(uuid));

            // Create the UpdateRequest with a retry_on_conflict setting to handle rapid
            // concurrent updates
            UpdateRequest updateRequest = new UpdateRequest(sbUserIndex, _DOC, userId)
                    .script(script)
                    .upsert(new IndexRequest(sbUserIndex).id(userId).source(upsertContent))
                    .retryOnConflict(5);

            // Execute the update
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
            DocWriteResponse.Result result = updateResponse.getResult();

            if (result == DocWriteResponse.Result.CREATED) {
                logger.info("WfRequests created successfully for userId: {}", userId);
            } else if (result == DocWriteResponse.Result.UPDATED) {
                logger.info("WfRequests updated successfully for userId: {}", userId);
            } else if (result == DocWriteResponse.Result.NOOP) {
                logger.info("WfRequests update was a noop; no changes were made for userId: {}", userId);
            } else {
                logger.warn("WfRequests update:: Unexpected result: {}, for userId: {}", result, userId);
            }
        } catch (Exception e) {
            logger.error("Failed to update wfRequests for userId: {}", userId, e);
            return false;
        }
        return true;
    }

    public boolean updateWfTransferRequest(String userId, String deptName, String wfId, boolean append) {
        try {
            // Prepare parameters for the script
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.DEPARTMENT_NAME, deptName);
            params.put(Constants.WF_ID_CONSTANT, wfId);

            // Script logic for add or remove
            String scriptSource;
            if (append) {
                scriptSource = "ctx._source.wfTransferRequest = new HashMap(); " +
                        "ctx._source.wfTransferRequest.departmentName = params.departmentName; " +
                        "ctx._source.wfTransferRequest.wfId = params.wfId;";
            } else {
                scriptSource = "ctx._source.wfTransferRequest = new HashMap();";
            }

            // Create script object
            Script script = new Script(ScriptType.INLINE, "painless", scriptSource, params);

            // Upsert logic: If document doesn’t exist, initialize it with `wfTransferRequest`
            Map<String, Object> upsertContent = new HashMap<>();
            if (append) {
                Map<String, Object> wfTransferRequest = new HashMap<>();
                wfTransferRequest.put(Constants.DEPARTMENT_NAME, deptName);
                wfTransferRequest.put(Constants.WF_ID_CONSTANT, wfId);
                upsertContent.put(Constants.WF_TRANSFER_REQUEST_STRING, wfTransferRequest);
            } else {
                upsertContent.put("wfTransferRequest", new HashMap<>()); // Set as empty map
            }

            // Create UpdateRequest
            UpdateRequest updateRequest = new UpdateRequest(sbUserIndex, _DOC, userId)
                    .script(script)
                    .upsert(upsertContent) // Creates the document if missing
                    .retryOnConflict(5); // Handle concurrent updates

            // Execute the update
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);

            // Log success
            switch (updateResponse.getResult()) {
                case CREATED:
                    logger.info("WfTransferRequest created successfully for userId: {}", userId);
                    break;
                case UPDATED:
                    logger.info("WfTransferRequest updated successfully for userId: {}", userId);
                    break;
                case NOOP:
                    logger.info("WfTransferRequest update was a noop; no changes were made for userId: {}", userId);
                    break;
                default:
                    logger.warn("WfTransferRequest update:: Unexpected result: {}, for userId: {}",
                            updateResponse.getResult(), userId);
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to update wfTransferRequest for userId: {}", userId, e);
            return false;
        }
    }

    public boolean updateWfProfileRequest(String userId, String uuid, String departmentName, boolean append) {
        try {
            // Prepare parameters for the script
            Map<String, Object> params = new HashMap<>();
            params.put("uuid", uuid);
            params.put("departmentName", departmentName);
    
            // Script logic for adding or removing the entry
            String scriptSource;
            if (append) {
                scriptSource = "if (ctx._source.wfProfileRequests == null) { " +
                        "  ctx._source.wfProfileRequests = new ArrayList(); " +
                        "} " +
                        "boolean exists = false; " +
                        "for (entry in ctx._source.wfProfileRequests) { " +
                        "  if (entry.wfId == params.uuid && entry.departmentName == params.departmentName) { " +
                        "    exists = true; " +
                        "    break; " +
                        "  } " +
                        "} " +
                        "if (!exists) { " +
                        "  ctx._source.wfProfileRequests.add(['wfId': params.uuid, 'departmentName': params.departmentName]); " +
                        "}";
            } else {
                scriptSource = "if (ctx._source.wfProfileRequests != null) { " +
                    "  List newList = new ArrayList(); " +
                    "  for (entry in ctx._source.wfProfileRequests) { " +
                    "    if (!(entry.wfId == params.uuid && entry.departmentName == params.departmentName)) { " +
                    "      newList.add(entry); " +
                    "    } " +
                    "  } " +
                    "  ctx._source.wfProfileRequests = newList; " +
                    "  if (ctx._source.wfProfileRequests.isEmpty()) { " +  // Remove field if empty
                    "    ctx._source.remove('wfProfileRequests'); " +
                    "  } " +
                    "}";
            }
    
            // Create the script
            Script script = new Script(ScriptType.INLINE, "painless", scriptSource, params);
    
            // Define upsert content only if append = true
            UpdateRequest updateRequest = new UpdateRequest(sbUserIndex, _DOC, userId)
                    .script(script)
                    .retryOnConflict(5);
    
            if (append) {
                // Create upsert content for insert operations
                Map<String, Object> upsertContent = new HashMap<>();
                List<Map<String, Object>> initialList = new ArrayList<>();
                Map<String, Object> entry = new HashMap<>();
                entry.put("wfId", uuid);
                entry.put("departmentName", departmentName);
                initialList.add(entry);
                upsertContent.put("wfProfileRequests", initialList);
    
                updateRequest.upsert(new IndexRequest(sbUserIndex).id(userId).source(upsertContent));
            }
    
            // Execute the update
            UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
            DocWriteResponse.Result result = updateResponse.getResult();
    
            if (result == DocWriteResponse.Result.CREATED) {
                logger.info("wfProfileRequests created successfully for userId: {}", userId);
            } else if (result == DocWriteResponse.Result.UPDATED) {
                logger.info("wfProfileRequests updated successfully for userId: {}", userId);
            } else if (result == DocWriteResponse.Result.NOOP) {
                logger.info("wfProfileRequests update was a noop; no changes were made for userId: {}", userId);
            } else {
                logger.warn("wfProfileRequests update:: Unexpected result: {}, for userId: {}", result, userId);
            }
        } catch (Exception e) {
            logger.error("Failed to update wfProfileRequests for userId: {}", userId, e);
            return false;
        }
        return true;
    }    
}
