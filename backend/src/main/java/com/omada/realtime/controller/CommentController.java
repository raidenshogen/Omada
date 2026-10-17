package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateCommentRequest;
import com.omada.realtime.dto.response.CommentResponse;
import com.omada.realtime.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards/{cardId}/comments")
public class CommentController {
    private final CommentService service;
    public CommentController(CommentService service){ this.service = service; }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.canEditCard(#cardId, authentication.name)")
    public CommentResponse create(@PathVariable String cardId, @Valid @RequestBody CreateCommentRequest req){
        return service.create(cardId, req);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.canViewCard(#cardId, authentication.name)")
    public List<CommentResponse> byCard(@PathVariable String cardId){
        return service.findByCard(cardId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canViewCard(#cardId, authentication.name)")
    public CommentResponse get(@PathVariable String cardId, @PathVariable String id){
        return service.getById(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canEditCard(#cardId, authentication.name)")
    public CommentResponse update(@PathVariable String cardId, @PathVariable String id, @Valid @RequestBody CreateCommentRequest req){
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canEditCard(#cardId, authentication.name)")
    public void delete(@PathVariable String cardId, @PathVariable String id){
        service.delete(id);
    }
}
