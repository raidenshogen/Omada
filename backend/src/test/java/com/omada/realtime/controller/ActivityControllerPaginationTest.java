package com.omada.realtime.controller;

import com.omada.realtime.config.SecurityConfig;
import com.omada.realtime.dto.response.ActivityResponse;
import com.omada.realtime.dto.response.PageResponse;
import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.repository.BoardMembershipRepository;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.InvitationRepository;
import com.omada.realtime.security.JwtAuthenticationFilter;
import com.omada.realtime.security.JwtUtil;
import com.omada.realtime.security.WorkspaceSecurity;
import com.omada.realtime.service.ActivityService;
import com.omada.realtime.service.MembershipService;
import com.omada.realtime.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pagination contract for the activity feed endpoint: default + explicit
 * paging, size clamping, and the PageResponse JSON envelope.
 */
@WebMvcTest(controllers = ActivityController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkspaceSecurity.class})
class ActivityControllerPaginationTest {

    @Autowired MockMvc mvc;
    @MockBean ActivityService activityService;
    @MockBean MembershipService membershipService;
    @MockBean WorkspaceService workspaceService;
    @MockBean BoardRepository boardRepository;
    @MockBean CardRepository cardRepository;
    @MockBean StringRedisTemplate redis;
    @MockBean BoardMembershipRepository boardMembershipRepository;
    @MockBean InvitationRepository invitationRepository;

    static final String WS = "33333333-3333-3333-3333-333333333333";
    static final String ORG = "org-1";
    static final String USER = "user-1";

    private String token;

    @BeforeEach
    void setUp() {
        token = JwtUtil.generateAccess(USER);
        when(workspaceService.findById(WS)).thenReturn(new WorkspaceResponse(WS, ORG, "ws", ""));
        var ops = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
        when(membershipService.hasRole(ORG, USER, "VIEWER")).thenReturn(true);
        when(membershipService.hasRole(ORG, USER, "MEMBER")).thenReturn(true);
    }

    @Test
    void returnsPageResponseEnvelopeWithDefaults() throws Exception {
        when(activityService.findByWorkspace(WS, 0, 20)).thenReturn(
                new PageResponse<>(List.of(new ActivityResponse("a1", "CARD_CREATED", "c1", USER, WS, "b1", null)),
                        0, 20, 1, 1));

        mvc.perform(get("/api/activities").param("workspaceId", WS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("CARD_CREATED"))
                .andExpect(jsonPath("$.content[0].workspaceId").value(WS));
    }

    @Test
    void passesExplicitPageAndSize() throws Exception {
        when(activityService.findByWorkspace(WS, 2, 5)).thenReturn(
                new PageResponse<>(List.of(), 2, 5, 0, 0));

        mvc.perform(get("/api/activities").param("workspaceId", WS)
                        .param("page", "2").param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void clampsOversizedPageSizeToMax() throws Exception {
        when(activityService.findByWorkspace(WS, 0, 100)).thenReturn(
                new PageResponse<>(List.of(), 0, 100, 0, 0));

        mvc.perform(get("/api/activities").param("workspaceId", WS)
                        .param("size", "9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void clampsNegativePageToZero() throws Exception {
        when(activityService.findByWorkspace(WS, 0, 20)).thenReturn(
                new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/activities").param("workspaceId", WS)
                        .param("page", "-3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void boardScopedFeedRoutesToBoardQuery() throws Exception {
        when(boardRepository.findById(java.util.UUID.fromString(WS)))
                .thenReturn(java.util.Optional.of(com.omada.realtime.entity.Board.builder()
                        .id(java.util.UUID.fromString(WS)).workspaceId(WS).build()));
        when(activityService.findByBoard(WS, 0, 20)).thenReturn(
                new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/activities").param("boardId", WS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }
}
