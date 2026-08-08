package com.bzcom.crm.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bzcom.crm.auth.jwt.JwtProvider;
import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.repository.MemberRepository;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.request.RequestCreateRequest;
import com.bzcom.crm.workflow.dto.request.AssignRequest;
import com.bzcom.crm.workflow.dto.request.StatusUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestWorkflowIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private Member adminMember;
    private Member dev1Member;
    private Member dev2Member;
    private Member client1Member;
    private Member client2Member;

    private String adminToken;
    private String dev1Token;
    private String dev2Token;
    private String client1Token;
    private String client2Token;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM request_histories");
        jdbcTemplate.update("DELETE FROM alerts");
        jdbcTemplate.update("DELETE FROM requests");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM members");

        String pwd = passwordEncoder.encode("1234");
        adminMember = memberRepository.save(new Member("admin@bzcom.com", pwd, "Admin", MemberRole.ADMIN));
        dev1Member = memberRepository.save(new Member("dev1@bzcom.com", pwd, "Dev One", MemberRole.DEVELOPER));
        dev2Member = memberRepository.save(new Member("dev2@bzcom.com", pwd, "Dev Two", MemberRole.DEVELOPER));
        client1Member = memberRepository.save(new Member("client1@bzcom.com", pwd, "Client One", MemberRole.CLIENT));
        client2Member = memberRepository.save(new Member("client2@bzcom.com", pwd, "Client Two", MemberRole.CLIENT));

        adminToken = jwtProvider.generateAccessToken(adminMember.getId(), adminMember.getRole());
        dev1Token = jwtProvider.generateAccessToken(dev1Member.getId(), dev1Member.getRole());
        dev2Token = jwtProvider.generateAccessToken(dev2Member.getId(), dev2Member.getRole());
        client1Token = jwtProvider.generateAccessToken(client1Member.getId(), client1Member.getRole());
        client2Token = jwtProvider.generateAccessToken(client2Member.getId(), client2Member.getRole());
    }

    @Test
    @DisplayName(
            "End-to-End Workflow: Create request -> Auto Assign -> Status PENDING->IN_PROGRESS->DONE -> History & Alert")
    void testEndToEndWorkflow() throws Exception {
        // 1. Client 1 creates a HIGH priority request
        RequestCreateRequest createReq =
                new RequestCreateRequest("Login bug", "Returns 500 error", RequestCategory.BUG, RequestPriority.HIGH);
        MvcResult createRes = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.clientId").value(client1Member.getId()))
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn();

        JsonNode reqData = objectMapper
                .readTree(createRes.getResponse().getContentAsString())
                .path("data");
        long requestId = reqData.path("id").asLong();

        // Verify alert created for Admin due to HIGH priority
        Integer adminAlertCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM alerts WHERE target_member_id = ?", Integer.class, adminMember.getId());
        assertThat(adminAlertCount).isEqualTo(1);

        // 2. Admin auto-assigns developer
        AssignRequest autoAssignReq = new AssignRequest(true, null, 0);
        MvcResult assignRes = mockMvc.perform(patch("/api/requests/" + requestId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autoAssignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedDeveloperId").value(dev1Member.getId()))
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn();

        // Verify alert created for Dev 1
        Integer dev1AlertCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM alerts WHERE target_member_id = ? AND alert_type = 'ASSIGNED'",
                Integer.class,
                dev1Member.getId());
        assertThat(dev1AlertCount).isEqualTo(1);

        // 3. Dev 1 updates status: PENDING -> IN_PROGRESS
        StatusUpdateRequest statusReq1 = new StatusUpdateRequest(RequestStatus.IN_PROGRESS, "Started working", 1);
        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + dev1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.version").value(2));

        // 4. Dev 1 updates status: IN_PROGRESS -> DONE
        StatusUpdateRequest statusReq2 = new StatusUpdateRequest(RequestStatus.DONE, "Fixed bug", 2);
        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + dev1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.version").value(3));

        // Verify dev 1 last_completed_at updated
        Member updatedDev1 = memberRepository.findById(dev1Member.getId()).orElseThrow();
        assertThat(updatedDev1.getLastCompletedAt()).isNotNull();

        // 5. Retrieve history
        mockMvc.perform(get("/api/requests/" + requestId + "/history")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].memo").value("auto-assigned to Dev One"))
                .andExpect(jsonPath("$.data[1].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[1].toStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[2].fromStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[2].toStatus").value("DONE"));
    }

    @Test
    @DisplayName("Role Access Matrix & Error Scenarios")
    void testAccessMatrixAndErrors() throws Exception {
        // Create request by Client 1
        RequestCreateRequest createReq = new RequestCreateRequest(
                "Feature request", "Add dark mode", RequestCategory.FEATURE, RequestPriority.LOW);
        MvcResult createRes = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + client1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long requestId = objectMapper
                .readTree(createRes.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        // Client 2 trying to view Client 1's request detail -> 403
        mockMvc.perform(get("/api/requests/" + requestId).header("Authorization", "Bearer " + client2Token))
                .andExpect(status().isForbidden());

        // Dev 1 trying to update status BEFORE request is assigned -> 409
        StatusUpdateRequest updateUnassignedReq = new StatusUpdateRequest(RequestStatus.IN_PROGRESS, "Start", 0);
        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + dev1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUnassignedReq)))
                .andExpect(status().isForbidden()); // Dev 1 not assigned yet -> 403 ownership check

        // Admin updates status of unassigned request -> 409
        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUnassignedReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Request is not assigned to any developer"));

        // Admin assigns to Dev 1
        AssignRequest assignReq = new AssignRequest(false, dev1Member.getId(), 0);
        mockMvc.perform(patch("/api/requests/" + requestId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk());

        // Dev 1 tries invalid status transition: PENDING -> DONE -> 409
        StatusUpdateRequest invalidTransitionReq = new StatusUpdateRequest(RequestStatus.DONE, "Jump step", 1);
        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + dev1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransitionReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid status transition: PENDING -> DONE"));

        // Dev 1 tries stale expectedVersion -> 409
        StatusUpdateRequest staleVersionReq = new StatusUpdateRequest(RequestStatus.IN_PROGRESS, "Stale", 0);
        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                        .header("Authorization", "Bearer " + dev1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleVersionReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Request version conflict: expected 0 but found 1"));
    }
}
