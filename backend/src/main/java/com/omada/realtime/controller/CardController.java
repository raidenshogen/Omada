package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateCardRequest;
import com.omada.realtime.dto.request.UpdateCardRequest;
import com.omada.realtime.dto.response.CardResponse;
import com.omada.realtime.service.CardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/lists/{listId}/cards")
public class CardController {
    private final CardService service;
    public CardController(CardService service){ this.service = service; }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.canEditBoard(#boardId, authentication.name)")
    public CardResponse create(@PathVariable String boardId, @PathVariable String listId, @Valid @RequestBody CreateCardRequest req){
        req.boardId = boardId;
        req.listId = listId;
        return service.create(req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canEditCard(#id, authentication.name)")
    public CardResponse update(@PathVariable String boardId, @PathVariable String listId, @PathVariable String id, @Valid @RequestBody UpdateCardRequest req){
        return service.update(id, req);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.canViewBoard(#boardId, authentication.name)")
    public List<CardResponse> list(@PathVariable String boardId, @PathVariable String listId){
        return service.findByList(listId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canViewCard(#id, authentication.name)")
    public CardResponse get(@PathVariable String boardId, @PathVariable String listId, @PathVariable String id){
        return service.getById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canEditCard(#id, authentication.name)")
    public void delete(@PathVariable String boardId, @PathVariable String listId, @PathVariable String id){
        service.delete(id);
    }
}
