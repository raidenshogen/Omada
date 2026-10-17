package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateBlockRequest;
import com.omada.realtime.dto.request.UpdateBlockRequest;
import com.omada.realtime.dto.response.BlockResponse;
import com.omada.realtime.service.BlockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Block REST CRUD, nested under a document. Reads require VIEWER+ on the owning
 * workspace; writes require MEMBER+.
 */
@RestController
@RequestMapping("/api/documents/{documentId}/blocks")
public class BlockController {

    private final BlockService service;

    public BlockController(BlockService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@documentSecurity.canViewDocument(#documentId, authentication.name)")
    public List<BlockResponse> list(@PathVariable String documentId) {
        return service.findByDocument(documentId);
    }

    @PostMapping
    @PreAuthorize("@documentSecurity.canEditDocument(#documentId, authentication.name)")
    public BlockResponse create(@PathVariable String documentId, @RequestBody CreateBlockRequest req) {
        return service.create(documentId, req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@documentSecurity.canEditDocument(#documentId, authentication.name)")
    public BlockResponse update(@PathVariable String documentId, @PathVariable String id,
                                @RequestBody UpdateBlockRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@documentSecurity.canEditDocument(#documentId, authentication.name)")
    public void delete(@PathVariable String documentId, @PathVariable String id) {
        service.delete(id);
    }
}
