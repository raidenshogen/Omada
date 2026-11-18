package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.CreateBlockRequest;
import com.omada.realtime.dto.request.UpdateBlockRequest;
import com.omada.realtime.dto.response.BlockResponse;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.entity.Block;
import com.omada.realtime.entity.BlockType;
import com.omada.realtime.entity.Document;
import com.omada.realtime.event.DocumentChangedEvent;
import com.omada.realtime.mapper.BlockMapper;
import com.omada.realtime.repository.BlockRepository;
import com.omada.realtime.repository.DocumentRepository;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.security.ActorContext;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.ActivityProducer;
import com.omada.realtime.service.BlockService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BlockServiceImpl implements BlockService {
    private final BlockRepository repo;
    private final DocumentRepository documentRepo;
    private final BlockMapper mapper;
    private final ActivityProducer producer;
    private final ApplicationEventPublisher events;
    private final UserRepository userRepository;

    public BlockServiceImpl(BlockRepository repo, DocumentRepository documentRepo, BlockMapper mapper,
                            ActivityProducer producer, ApplicationEventPublisher events, UserRepository userRepository) {
        this.repo = repo;
        this.documentRepo = documentRepo;
        this.mapper = mapper;
        this.producer = producer;
        this.events = events;
        this.userRepository = userRepository;
    }

    private String userDisplayName(String id) {
        if (id == null) return null;
        if ("system".equals(id)) return "System";
        try {
            return userRepository.findById(UUID.fromString(id))
                    .map(u -> {
                        if (u.getName() != null && !u.getName().isBlank()) return u.getName();
                        return u.getEmail() != null ? u.getEmail() : id;
                    })
                    .orElse(id);
        } catch (Exception e) {
            return id;
        }
    }

    private void enrich(BlockResponse r, Document doc) {
        if (r.createdBy == null && doc != null) r.createdBy = doc.getCreatedBy();
        if (r.updatedBy == null && doc != null) r.updatedBy = doc.getCreatedBy();
        if (r.createdBy != null) r.createdByName = userDisplayName(r.createdBy);
        if (r.updatedBy != null) r.updatedByName = userDisplayName(r.updatedBy);
    }

    @Override
    public List<BlockResponse> findByDocument(String documentId) {
        Document doc = documentRepo.findById(UUID.fromString(documentId)).orElse(null);
        List<Block> blocks = repo.findByDocumentIdOrderByPositionAsc(documentId);
        List<BlockResponse> res = blocks.stream().map(mapper::toResponse).collect(Collectors.toList());
        res.forEach(r -> enrich(r, doc));
        return res;
    }

    @Override
    public BlockResponse create(String documentId, CreateBlockRequest req) {
        Document doc = documentRepo.findById(UUID.fromString(documentId)).orElseThrow();
        Block block = mapper.toEntity(req);
        block.setId(IdGenerator.uuid());
        block.setDocumentId(documentId);
        block.setType(BlockType.normalize(req.type));
        if (req.checked != null) block.setChecked(req.checked);
        if (req.position != null) block.setPosition(req.position);
        block.setVersion(1);
        Instant now = Instant.now();
        block.setCreatedAt(now);
        block.setUpdatedAt(now);
        String actor = ActorContext.idOrSystem();
        block.setCreatedBy(actor);
        block.setUpdatedBy(actor);
        block = repo.save(block);
        BlockResponse r = mapper.toResponse(block);
        enrich(r, doc);
        emit(doc, block, "BLOCK_CREATED", r);
        return r;
    }

    @Override
    public BlockResponse update(String id, UpdateBlockRequest req) {
        Block block = repo.findById(UUID.fromString(id)).orElseThrow();
        if (req.version != null && block.getVersion() != req.version) {
            throw new IllegalStateException("Version conflict");
        }
        if (req.type != null) block.setType(BlockType.normalize(req.type));
        if (req.text != null) block.setText(req.text);
        if (req.checked != null) block.setChecked(req.checked);
        if (req.position != null) block.setPosition(req.position);
        block.setUpdatedAt(Instant.now());
        block.setUpdatedBy(ActorContext.idOrSystem());
        block.setVersion(block.getVersion() + 1);
        block = repo.save(block);
        Document doc = documentRepo.findById(UUID.fromString(block.getDocumentId())).orElse(null);
        BlockResponse r = mapper.toResponse(block);
        enrich(r, doc);
        emit(doc, block, "BLOCK_UPDATED", r);
        return r;
    }

    @Override
    public void delete(String id) {
        Block block = repo.findById(UUID.fromString(id)).orElseThrow();
        Document doc = documentRepo.findById(UUID.fromString(block.getDocumentId())).orElse(null);
        repo.delete(block);
        BlockResponse r = mapper.toResponse(block);
        enrich(r, doc);
        emit(doc, block, "BLOCK_DELETED", r);
    }

    private void emit(Document doc, Block block, String type, BlockResponse enriched) {
        String workspaceId = doc != null ? doc.getWorkspaceId() : null;
        String boardId = doc != null ? doc.getBoardId() : null;
        String documentId = block.getDocumentId();
        producer.publish(Activity.builder()
                .id(IdGenerator.uuid())
                .workspaceId(workspaceId)
                .boardId(boardId)
                .actorId(ActorContext.idOrSystem())
                .type("DOCUMENT_EDITED")
                .entityId(documentId)
                .createdAt(Instant.now())
                .build());
        events.publishEvent(new DocumentChangedEvent(type, documentId, workspaceId, enriched));
    }
}
