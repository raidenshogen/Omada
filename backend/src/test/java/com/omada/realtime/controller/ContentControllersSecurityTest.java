package com.omada.realtime.controller;

import com.omada.realtime.config.SecurityConfig;
import com.omada.realtime.dto.response.CommentResponse;
import com.omada.realtime.dto.response.ListResponse;
import com.omada.realtime.dto.response.PageResponse;
import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.entity.Board;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.security.JwtAuthenticationFilter;
import com.omada.realtime.security.JwtUtil;
import com.omada.realtime.security.WorkspaceSecurity;
import com.omada.realtime.service.ActivityService;
import com.omada.realtime.service.CommentService;
import com.omada.realtime.service.MembershipService;
import com.omada.realtime.service.TaskListService;
import com.omada.realtime.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC wiring for the list, comment, workspace and activity endpoints.
 * Container-free (@WebMvcTest + mocked persistence) so it runs without Docker.
 */
@WebMvcTest(controllers = {ListController.class, CommentController.class,
        WorkspaceController.class, ActivityController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkspaceSecurity.class})
class ContentControllersSecurityTest {

    @Autowired MockMvc mvc;
    @MockBean TaskListService taskListService;
    @MockBean CommentService commentService;
    @MockBean WorkspaceService workspaceService;
    @MockBean ActivityService activityService;
    @MockBean MembershipService membershipService;
    @MockBean BoardRepository boardRepository;
    @MockBean CardRepository cardRepository;

    static final UUID BOARD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID CARD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final String WS = "33333333-3333-3333-3333-333333333333";
    static final String ORG = "org-1";
    static final String USER = "user-1";

    private String token;

    @BeforeEach
    void setUp() {
        token = JwtUtil.generateAccess(USER);
        when(boardRepository.findById(BOARD_ID))
                .thenReturn(Optional.of(Board.builder().id(BOARD_ID).workspaceId(WS).build()));
        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(com.omada.realtime.entity.Card.builder()
                        .id(CARD_ID).boardId(BOARD_ID.toString()).listId("list-1").build()));
        when(workspaceService.findById(WS)).thenReturn(new WorkspaceResponse(WS, ORG, "ws", ""));
    }

    private void givenRole(String role) {
        when(membershipService.hasRole(eq(ORG), eq(USER), anyString()))
                .thenAnswer(inv -> rank(role) >= rank(inv.getArgument(2, String.class)));
    }

    private static int rank(String role) {
        return switch (role) {
            case "VIEWER" -> 0;
            case "MEMBER" -> 1;
            case "ADMIN" -> 2;
            case "OWNER" -> 3;
            default -> -1;
        };
    }

    // ---- Lists ----
    @Test
    void viewerCannotCreateList() throws Exception {
        givenRole("VIEWER");
        mvc.perform(post("/api/boards/{b}/lists", BOARD_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"L\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanCreateList() throws Exception {
        givenRole("MEMBER");
        when(taskListService.create(any())).thenReturn(new ListResponse("l1", "L", 0));
        mvc.perform(post("/api/boards/{b}/lists", BOARD_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"L\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nonMemberCannotReadLists() throws Exception {
        givenRole("NONE");
        mvc.perform(get("/api/boards/{b}/lists", BOARD_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ---- Comments ----
    @Test
    void viewerCannotComment() throws Exception {
        givenRole("VIEWER");
        mvc.perform(post("/api/cards/{c}/comments", CARD_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"hi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanComment() throws Exception {
        givenRole("MEMBER");
        when(commentService.create(eq(CARD_ID.toString()), any())).thenReturn(new CommentResponse("cm1", "hi", USER));
        mvc.perform(post("/api/cards/{c}/comments", CARD_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"hi\"}"))
                .andExpect(status().isOk());
    }

    // ---- Workspaces ----
    @Test
    void nonMemberCannotCreateWorkspace() throws Exception {
        givenRole("NONE");
        mvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":\"" + ORG + "\",\"name\":\"W\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanCreateWorkspace() throws Exception {
        givenRole("MEMBER");
        when(workspaceService.create(any())).thenReturn(new WorkspaceResponse(WS, ORG, "W", ""));
        mvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":\"" + ORG + "\",\"name\":\"W\"}"))
                .andExpect(status().isOk());
    }

    // ---- Activity ----
    @Test
    void activityWithoutScopeIsForbidden() throws Exception {
        givenRole("OWNER");
        mvc.perform(get("/api/activities").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void activityForForeignWorkspaceIsForbidden() throws Exception {
        givenRole("NONE");
        when(activityService.findByWorkspace(eq(WS), anyInt(), anyInt())).thenReturn(PageResponse.empty(0, 20));
        mvc.perform(get("/api/activities").param("workspaceId", WS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void activityForOwnWorkspaceIsAllowed() throws Exception {
        givenRole("MEMBER");
        when(activityService.findByWorkspace(eq(WS), anyInt(), anyInt())).thenReturn(PageResponse.empty(0, 20));
        mvc.perform(get("/api/activities").param("workspaceId", WS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
