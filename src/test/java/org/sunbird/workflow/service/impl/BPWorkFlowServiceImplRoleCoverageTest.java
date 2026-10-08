package org.sunbird.workflow.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.sunbird.workflow.config.Constants;

/**
 * Covers extractUserRole / resolveRoleFromOrg and the empty-file branch of
 * validateAndExtractApprovalData in BPWorkFlowServiceImpl.
 */
class BPWorkFlowServiceImplRoleCoverageTest {

    private BPWorkFlowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BPWorkFlowServiceImpl();
    }

    private String extractUserRole(Map<String, Object> profile) throws Exception {
        Method method = BPWorkFlowServiceImpl.class.getDeclaredMethod("extractUserRole", Map.class);
        method.setAccessible(true);
        return (String) method.invoke(service, profile);
    }

    private String resolveRoleFromOrg(List<String> roles) throws Exception {
        Method method = BPWorkFlowServiceImpl.class.getDeclaredMethod("resolveRoleFromOrg", List.class);
        method.setAccessible(true);
        return (String) method.invoke(service, roles);
    }

    private Map<String, Object> org(Object roles) {
        Map<String, Object> org = new HashMap<>();
        org.put(Constants.ROLES, roles);
        return org;
    }

    private Map<String, Object> profileWithOrgs(Object... orgs) {
        Map<String, Object> profile = new HashMap<>();
        profile.put(Constants.ORGANISATIONS, new ArrayList<>(List.of(orgs)));
        return profile;
    }

    // ---------- resolveRoleFromOrg ----------

    @Test
    void resolveRoleFromOrg_nullRoles_returnsNull() throws Exception {
        assertNull(resolveRoleFromOrg(null));
    }

    @Test
    void resolveRoleFromOrg_programCoordinator_returnsPC() throws Exception {
        assertEquals(Constants.PC, resolveRoleFromOrg(List.of(Constants.PROGRAM_COORDINATOR)));
    }

    @Test
    void resolveRoleFromOrg_bpProgramTrainer_returnsPC() throws Exception {
        assertEquals(Constants.PC, resolveRoleFromOrg(List.of(Constants.BP_PROGRAM_TRAINER)));
    }

    @Test
    void resolveRoleFromOrg_mdoAdmin_returnsMDO() throws Exception {
        assertEquals(Constants.MDO, resolveRoleFromOrg(List.of(Constants.MDO_ADMIN)));
    }

    @Test
    void resolveRoleFromOrg_mdoLeader_returnsMDO() throws Exception {
        assertEquals(Constants.MDO, resolveRoleFromOrg(List.of(Constants.MDO_LEADER)));
    }

    @Test
    void resolveRoleFromOrg_pcTakesPrecedenceOverMdo() throws Exception {
        assertEquals(Constants.PC, resolveRoleFromOrg(List.of(Constants.MDO_ADMIN, Constants.PROGRAM_COORDINATOR)));
    }

    @Test
    void resolveRoleFromOrg_noMatchingRole_returnsNull() throws Exception {
        assertNull(resolveRoleFromOrg(List.of("PUBLIC")));
        assertNull(resolveRoleFromOrg(new ArrayList<>()));
    }

    // ---------- extractUserRole ----------

    @Test
    void extractUserRole_noOrganisations_returnsSelf() throws Exception {
        assertEquals(Constants.SELF, extractUserRole(new HashMap<>()));
    }

    @Test
    void extractUserRole_emptyOrganisations_returnsSelf() throws Exception {
        assertEquals(Constants.SELF, extractUserRole(profileWithOrgs()));
    }

    @Test
    void extractUserRole_orgWithoutRoles_returnsSelf() throws Exception {
        assertEquals(Constants.SELF, extractUserRole(profileWithOrgs(org(null))));
    }

    @Test
    void extractUserRole_orgWithNonMatchingRoles_returnsSelf() throws Exception {
        assertEquals(Constants.SELF, extractUserRole(profileWithOrgs(org(List.of("PUBLIC")))));
    }

    @Test
    void extractUserRole_programCoordinator_returnsPC() throws Exception {
        assertEquals(Constants.PC, extractUserRole(profileWithOrgs(org(List.of(Constants.PROGRAM_COORDINATOR)))));
    }

    @Test
    void extractUserRole_bpProgramTrainer_returnsPC() throws Exception {
        assertEquals(Constants.PC, extractUserRole(profileWithOrgs(org(List.of(Constants.BP_PROGRAM_TRAINER)))));
    }

    @Test
    void extractUserRole_mdoAdminOrLeader_returnsMDO() throws Exception {
        assertEquals(Constants.MDO, extractUserRole(profileWithOrgs(org(List.of(Constants.MDO_ADMIN)))));
        assertEquals(Constants.MDO, extractUserRole(profileWithOrgs(org(List.of(Constants.MDO_LEADER)))));
    }

    @Test
    void extractUserRole_firstOrgWithoutMatch_thenSecondOrgMatches() throws Exception {
        Map<String, Object> profile = profileWithOrgs(
                org(List.of("PUBLIC")),
                org(null),
                org(List.of(Constants.MDO_ADMIN)));
        assertEquals(Constants.MDO, extractUserRole(profile));
    }

    @Test
    void extractUserRole_firstMatchingOrgWins() throws Exception {
        Map<String, Object> profile = profileWithOrgs(
                org(List.of(Constants.MDO_ADMIN)),
                org(List.of(Constants.PROGRAM_COORDINATOR)));
        assertEquals(Constants.MDO, extractUserRole(profile));
    }

    @Test
    void extractUserRole_malformedOrganisations_fallsBackToSelf() throws Exception {
        Map<String, Object> profile = new HashMap<>();
        profile.put(Constants.ORGANISATIONS, "not-a-list");
        assertEquals(Constants.SELF, extractUserRole(profile));
    }

    @Test
    void extractUserRole_malformedRoles_fallsBackToSelf() throws Exception {
        assertEquals(Constants.SELF, extractUserRole(profileWithOrgs(org("not-a-list"))));
    }

    @Test
    void extractUserRole_nullProfile_fallsBackToSelf() throws Exception {
        assertEquals(Constants.SELF, extractUserRole(null));
    }

    // ---------- validateAndExtractApprovalData: empty file ----------

    @Test
    void validateAndExtractApprovalData_emptyFile_returnsEmptyList() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "approval.csv", "text/csv",
                "".getBytes(StandardCharsets.UTF_8));
        List<String> errors = new ArrayList<>();

        Method method = BPWorkFlowServiceImpl.class.getDeclaredMethod(
                "validateAndExtractApprovalData", org.springframework.web.multipart.MultipartFile.class, List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) method.invoke(service, file, errors);

        assertTrue(rows.isEmpty());
        assertTrue(errors.isEmpty());
    }
}
