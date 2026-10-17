package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateDocumentRequest;
import com.omada.realtime.dto.request.UpdateDocumentRequest;
import com.omada.realtime.dto.response.DocumentResponse;
import com.omada.realtime.service.DocumentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Document REST API. Creation/listing are workspace-scoped (MEMBER+ to create,
 * VIEWER+ to read); item operations resolve the owning workspace via
 * @documentSecurity. Matches the roadmap's "documents" collection CRUD.
 */
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/workspaces/{workspaceId}/documents")
    @PreAuthorize("@workspaceSecurity.canWriteWorkspaceContent(#workspaceId, authentication.name)")
    public DocumentResponse create(@PathVariable String workspaceId, @RequestBody CreateDocumentRequest req) {
        return service.create(workspaceId, req);
    }

    @GetMapping("/workspaces/{workspaceId}/documents")
    @PreAuthorize("@workspaceSecurity.canViewWorkspace(#workspaceId, authentication.name)")
    public List<DocumentResponse> byWorkspace(@PathVariable String workspaceId) {
        return service.findByWorkspace(workspaceId);
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("@documentSecurity.canViewDocument(#id, authentication.name)")
    public DocumentResponse get(@PathVariable String id) {
        return service.getById(id);
    }

    @PatchMapping("/documents/{id}")
    @PreAuthorize("@documentSecurity.canEditDocument(#id, authentication.name)")
    public DocumentResponse update(@PathVariable String id, @RequestBody UpdateDocumentRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("@documentSecurity.canEditDocument(#id, authentication.name)")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
