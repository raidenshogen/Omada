package com.omada.realtime.service.impl;
import com.omada.realtime.dto.request.CreateCardRequest;
import com.omada.realtime.dto.request.UpdateCardRequest;
import com.omada.realtime.dto.response.CardResponse;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Card;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.event.CardChangedEvent;
import com.omada.realtime.mapper.CardMapper;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.service.CardService;
import com.omada.realtime.service.ActivityProducer;
import com.omada.realtime.security.IdGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class CardServiceImpl implements CardService {
    private final CardRepository repo;
    private final BoardRepository boardRepo;
    private final CardMapper mapper;
    private final ActivityProducer producer;
    private final ApplicationEventPublisher events;
    public CardServiceImpl(CardRepository repo, BoardRepository boardRepo, CardMapper mapper, ActivityProducer producer,
                           ApplicationEventPublisher events){ this.repo=repo; this.boardRepo=boardRepo; this.mapper=mapper; this.producer=producer; this.events=events; }
    @Override public CardResponse create(CreateCardRequest req){
        Card card = mapper.toEntity(req);
        card.setId(IdGenerator.uuid());
        card.setCreatedAt(Instant.now());
        card.setUpdatedAt(Instant.now());
        card.setVersion(1);
        card = repo.save(card);
        emit(card, "CARD_CREATED");
        events.publishEvent(new CardChangedEvent(
                "CARD_CREATED", card.getBoardId(), card.getListId(), null, mapper.toResponse(card)));
        return mapper.toResponse(card);
    }
    @Override public CardResponse update(String id, UpdateCardRequest req){
        Card card = repo.findById(UUID.fromString(id)).orElseThrow();
        if (req.version != null && card.getVersion() != req.version) {
            throw new IllegalStateException("Version conflict");
        }
        String previousListId = card.getListId();
        if(req.title!=null) card.setTitle(req.title);
        if(req.description!=null) card.setDescription(req.description);
        if(req.position!=null) card.setPosition(req.position);
        if(req.listId!=null) card.setListId(req.listId);
        if(req.assignees!=null) card.setAssignees(req.assignees);
        card.setUpdatedAt(java.time.Instant.now());
        card.setVersion(card.getVersion() + 1);
        card = repo.save(card);
        // Emit activity event for card update
        emit(card, "CARD_UPDATED");
        boolean moved = req.listId != null && !req.listId.equals(previousListId);
        events.publishEvent(new CardChangedEvent(
                "CARD_UPDATED", card.getBoardId(), card.getListId(),
                moved ? previousListId : null, mapper.toResponse(card)));
        return mapper.toResponse(card);
    }
    @Override public List<CardResponse> findByBoard(String boardId){
        return repo.findByBoardIdOrderByPositionAsc(boardId).stream().map(mapper::toResponse).collect(Collectors.toList());
    }
    @Override public List<CardResponse> findByList(String listId){
        return repo.findByListIdOrderByPositionAsc(listId).stream().map(mapper::toResponse).collect(Collectors.toList());
    }
    @Override public CardResponse getById(String id){
        Card card = repo.findById(UUID.fromString(id)).orElseThrow();
        return mapper.toResponse(card);
    }
    @Override public void delete(String id){
        Card card = repo.findById(UUID.fromString(id)).orElseThrow();
        repo.delete(card);
        emit(card, "CARD_DELETED");
        events.publishEvent(new CardChangedEvent(
                "CARD_DELETED", card.getBoardId(), card.getListId(), null,
                Map.of("id", card.getId().toString())));
    }

    /** Builds and publishes an Activity for a card change, resolving the owning workspace and actor. */
    private void emit(Card card, String type){
        producer.publish(Activity.builder()
            .id(IdGenerator.uuid())
            .workspaceId(workspaceIdFor(card.getBoardId()))
            .boardId(card.getBoardId())
            .actorId(currentActor())
            .type(type)
            .entityId(card.getId().toString())
            .createdAt(Instant.now())
            .build());
    }

    /** Resolves the workspace a board belongs to; null if the board is unknown/malformed. */
    private String workspaceIdFor(String boardId){
        try {
            return boardRepo.findById(UUID.fromString(boardId)).map(Board::getWorkspaceId).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Authenticated user id, or "system" when there is no security context (e.g. scheduled work). */
    private static String currentActor(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : "system";
    }
}
