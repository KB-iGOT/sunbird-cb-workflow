package org.sunbird.workflow.postgres.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional

public interface WfStatusRepoV2 extends JpaRepository<WfStatusEntityV2, Long> {

    @Query(value = "SELECT * FROM sunbird.wf_status_v2 WHERE user_id = ?1 AND request_type = ?2 AND status NOT IN (?3)", nativeQuery = true)
    List<WfStatusEntityV2> countPendingRequests(String userId, String requestType, List<String> excludedStatuses);
}
