package org.sunbird.workflow.postgres.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "wf_status_v2", schema = "sunbird")
public class WfStatusEntityV2 {

    @Id
    @Column(name = "wf_id", length = 64) 
    private String wfId;

    @Column(name = "user_id", nullable = false, length = 64) 
    private String userId; 

    @Column(name = "organization_id", nullable = false, length = 64) 
    private String organizationId; 

    @Column(name = "request_type", nullable = false, length = 64) 
    private String requestType;

    @Column(name = "from_value", nullable = false, length = 64) 
    private String fromValue;

    @Column(name = "to_value", nullable = false, length = 64) 
    private String toValue;

    @Column(name = "status", nullable = false, length = 64) 
    private String status;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "created_by", nullable = false, length = 64) 
    private String createdBy; 

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "updated_by", nullable = false, length = 64) 
    private String updatedBy;

    @Column(name = "service_name")
    private String serviceName;

    public String getWfId() {
        return wfId; 
    }

    public void setWfId(String wfId) {
        this.wfId = wfId; 
    }

    public String getUserId() {
        return userId; 
    }

    public void setUserId(String userId) {
        this.userId = userId; 
    }

    public String getOrganizationId() {
        return organizationId; 
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId; 
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getFromValue() {
        return fromValue;
    }

    public void setFromValue(String fromValue) {
        this.fromValue = fromValue;
    }

    public String getToValue() {
        return toValue;
    }

    public void setToValue(String toValue) {
        this.toValue = toValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy; 
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy; 
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy; 
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy; 
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
