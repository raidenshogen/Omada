package com.omada.realtime.controller;

import com.omada.realtime.config.SecurityConfig;
import com.omada.realtime.dto.response.CardResponse;
import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Card;
import com.omada.realtime.repository.BoardMembershipRepository;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.InvitationRepository;
import com.omada.realtime.security.JwtAuthenticationFilter;
import com.omada.realtime.security.JwtUtil;
import com.omada.realtime.security.WorkspaceSecurity;
import com.omada.realtime.service.CardService;
import com.omada.realtime.service.MembershipService;
import com.omada.realtime.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC wiring for the card endpoints, exercised through MockMvc with the real
 * security filter chain and the real WorkspaceSecurity bean. No Mongo/Redis/
 * Kafka containers are needed: the repositories and membership lookup are
 * mocked, so this runs anywhere (including CI without Docker).
 *
 * Regression guard for the "any authenticated user can edit any card" hole:
 * before the guards were added these requests succeeded (200) for a VIEWER and
 * a non-member; they must now be 403.
 */
@WebMvcTest(controllers = CardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkspaceSecurity.class})
class CardControllerSecurityTest {

    @Autowired MockMvc mvc;
    @MockBean CardService cardService;
    @MockBean MembershipService membershipService;
    @MockBean WorkspaceService workspaceService;
    @MockBean BoardRepository boardRepository;
    @MockBean CardRepository cardRepository;
    @MockBean StringRedisTemplate redis;
    @MockBean BoardMembershipRepository boardMembershipRepository;
    @MockBean InvitationRepository invitationRepository;

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
                .thenReturn(Optional.of(Card.builder().id(CARD_ID).boardId(BOARD_ID.toString()).listId("list-1").build()));
        when(workspaceService.findById(WS))
                .thenReturn(new WorkspaceResponse(WS, ORG, "ws", ""));
        var ops = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
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

    @Test
    void viewerCannotCreateCard() throws Exception {
        givenRole("VIEWER");
        mvc.perform(post("/api/boards/{b}/lists/{l}/cards", BOARD_ID, "list-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanCreateCard() throws Exception {
        givenRole("MEMBER");
        when(cardService.create(any())).thenReturn(new CardResponse("c1", "x", "", 0, 1));
        mvc.perform(post("/api/boards/{b}/lists/{l}/cards", BOARD_ID, "list-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nonMemberCannotReadCards() throws Exception {
        givenRole("NONE");
        mvc.perform(get("/api/boards/{b}/lists/{l}/cards", BOARD_ID, "list-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanReadCards() throws Exception {
        givenRole("MEMBER");
        when(cardService.findByList("list-1")).thenReturn(java.util.List.of());
        mvc.perform(get("/api/boards/{b}/lists/{l}/cards", BOARD_ID, "list-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void noTokenIsRejected() throws Exception {
        givenRole("OWNER");
        mvc.perform(post("/api/boards/{b}/lists/{l}/cards", BOARD_ID, "list-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotUpdateCard() throws Exception {
        givenRole("VIEWER");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/boards/{b}/lists/{l}/cards/{id}", BOARD_ID, "list-1", CARD_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
