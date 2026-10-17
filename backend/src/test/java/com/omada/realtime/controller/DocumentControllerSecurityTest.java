package com.omada.realtime.controller;

import com.omada.realtime.config.SecurityConfig;
import com.omada.realtime.dto.response.BlockResponse;
import com.omada.realtime.dto.response.DocumentResponse;
import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.entity.Document;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.DocumentRepository;
import com.omada.realtime.security.DocumentSecurity;
import com.omada.realtime.security.JwtAuthenticationFilter;
import com.omada.realtime.security.JwtUtil;
import com.omada.realtime.security.WorkspaceSecurity;
import com.omada.realtime.service.BlockService;
import com.omada.realtime.service.DocumentService;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC wiring for documents and blocks. Reads require VIEWER+, writes MEMBER+.
 * Container-free (@WebMvcTest + mocked persistence).
 */
@WebMvcTest(controllers = {DocumentController.class, BlockController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkspaceSecurity.class, DocumentSecurity.class})
class DocumentControllerSecurityTest {

    @Autowired MockMvc mvc;
    @MockBean DocumentService documentService;
    @MockBean BlockService blockService;
    @MockBean WorkspaceService workspaceService;
    @MockBean MembershipService membershipService;
    @MockBean BoardRepository boardRepository;
    @MockBean CardRepository cardRepository;
    @MockBean DocumentRepository documentRepository;

    static final UUID DOC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    static final String WS = "33333333-3333-3333-3333-333333333333";
    static final String ORG = "org-1";
    static final String USER = "user-1";

    private String token;

    @BeforeEach
    void setUp() {
        token = JwtUtil.generateAccess(USER);
        when(workspaceService.findById(WS)).thenReturn(new WorkspaceResponse(WS, ORG, "ws", ""));
        when(documentRepository.findById(DOC_ID))
                .thenReturn(Optional.of(Document.builder().id(DOC_ID).workspaceId(WS).title("Doc").build()));
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
    void viewerCannotCreateDocument() throws Exception {
        givenRole("VIEWER");
        mvc.perform(post("/api/workspaces/{ws}/documents", WS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Doc\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanCreateDocument() throws Exception {
        givenRole("MEMBER");
        when(documentService.create(eq(WS), any()))
                .thenReturn(new DocumentResponse(DOC_ID.toString(), WS, null, "Doc", USER, 1, null, null));
        mvc.perform(post("/api/workspaces/{ws}/documents", WS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Doc\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nonMemberCannotReadDocument() throws Exception {
        givenRole("NONE");
        mvc.perform(get("/api/documents/{id}", DOC_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCanReadDocument() throws Exception {
        givenRole("VIEWER");
        when(documentService.getById(DOC_ID.toString()))
                .thenReturn(new DocumentResponse(DOC_ID.toString(), WS, null, "Doc", USER, 1, null, null));
        mvc.perform(get("/api/documents/{id}", DOC_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void viewerCannotEditDocument() throws Exception {
        givenRole("VIEWER");
        mvc.perform(patch("/api/documents/{id}", DOC_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"New\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanAddBlock() throws Exception {
        givenRole("MEMBER");
        when(blockService.create(eq(DOC_ID.toString()), any()))
                .thenReturn(new BlockResponse("b1", DOC_ID.toString(), "paragraph", "hi", false, 0, 1));
        mvc.perform(post("/api/documents/{id}/blocks", DOC_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"paragraph\",\"text\":\"hi\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nonMemberCannotListBlocks() throws Exception {
        givenRole("NONE");
        mvc.perform(get("/api/documents/{id}/blocks", DOC_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
