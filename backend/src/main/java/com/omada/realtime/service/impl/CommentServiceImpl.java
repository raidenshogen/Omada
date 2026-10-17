package com.omada.realtime.service.impl;
import com.omada.realtime.dto.request.CreateCommentRequest;
import com.omada.realtime.dto.response.CommentResponse;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Card;
import com.omada.realtime.entity.Comment;
import com.omada.realtime.mapper.CommentMapper;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.CommentRepository;
import com.omada.realtime.service.ActivityProducer;
import com.omada.realtime.service.CommentService;
import com.omada.realtime.security.IdGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository repo;
    private final CardRepository cardRepo;
    private final BoardRepository boardRepo;
    private final CommentMapper mapper;
    private final ActivityProducer producer;
    public CommentServiceImpl(CommentRepository repo, CardRepository cardRepo, BoardRepository boardRepo,
                              CommentMapper mapper, ActivityProducer producer){
        this.repo=repo; this.cardRepo=cardRepo; this.boardRepo=boardRepo; this.mapper=mapper; this.producer=producer; }
    @Override public CommentResponse create(String cardId, CreateCommentRequest req){
        Comment c = mapper.toEntity(req);
        c.setId(IdGenerator.uuid());
        c.setCardId(UUID.fromString(cardId));
        c.setAuthorId(currentActorId());
        c.setCreatedAt(Instant.now());
        c = repo.save(c);
        emitCommentActivity(c);
        return mapper.toResponse(c);
    }
    @Override public List<CommentResponse> findByCard(String cardId){
        return repo.findByCardId(UUID.fromString(cardId)).stream().map(mapper::toResponse).collect(Collectors.toList());
    }
    @Override public CommentResponse getById(String id){
        Comment c = repo.findById(UUID.fromString(id)).orElseThrow();
        return mapper.toResponse(c);
    }
    @Override public CommentResponse update(String id, CreateCommentRequest req){
        Comment c = repo.findById(UUID.fromString(id)).orElseThrow();
        if (req.body != null) c.setBody(req.body);
        c.setUpdatedAt(Instant.now());
        c = repo.save(c);
        return mapper.toResponse(c);
    }
    @Override public void delete(String id){
        repo.deleteById(UUID.fromString(id));
    }

    /** Publishes a COMMENT_ADDED activity, resolving the comment's card -> board -> workspace. */
    private void emitCommentActivity(Comment c){
        String boardId = cardRepo.findById(c.getCardId()).map(Card::getBoardId).orElse(null);
        String workspaceId = boardId == null ? null : boardRepo.findById(UUID.fromString(boardId))
                .map(Board::getWorkspaceId).orElse(null);
        producer.publish(Activity.builder()
            .id(IdGenerator.uuid())
            .workspaceId(workspaceId)
            .boardId(boardId)
            .actorId(String.valueOf(c.getAuthorId()))
            .type("COMMENT_ADDED")
            .entityId(c.getId().toString())
            .createdAt(Instant.now())
            .build());
    }

    /** Current user id as UUID, or null when unauthenticated / not a UUID. */
    private static UUID currentActorId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
