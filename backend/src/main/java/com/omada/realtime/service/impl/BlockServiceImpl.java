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

    public BlockServiceImpl(BlockRepository repo, DocumentRepository documentRepo, BlockMapper mapper,
                            ActivityProducer producer, ApplicationEventPublisher events) {
        this.repo = repo;
        this.documentRepo = documentRepo;
        this.mapper = mapper;
        this.producer = producer;
        this.events = events;
    }

    @Override
    public List<BlockResponse> findByDocument(String documentId) {
        return repo.findByDocumentIdOrderByPositionAsc(documentId).stream()
                .map(mapper::toResponse).collect(Collectors.toList());
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
        block.setCreatedAt(Instant.now());
        block.setUpdatedAt(Instant.now());
        block = repo.save(block);
        emit(doc, block, "BLOCK_CREATED");
        return mapper.toResponse(block);
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
        block.setVersion(block.getVersion() + 1);
        block = repo.save(block);
        Document doc = documentRepo.findById(UUID.fromString(block.getDocumentId())).orElse(null);
        emit(doc, block, "BLOCK_UPDATED");
        return mapper.toResponse(block);
    }

    @Override
    public void delete(String id) {
        Block block = repo.findById(UUID.fromString(id)).orElseThrow();
        Document doc = documentRepo.findById(UUID.fromString(block.getDocumentId())).orElse(null);
        repo.delete(block);
        emit(doc, block, "BLOCK_DELETED");
    }

    /** Broadcasts a block delta to editors and records a DOCUMENT_EDITED feed item. */
    private void emit(Document doc, Block block, String type) {
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
        events.publishEvent(new DocumentChangedEvent(type, documentId, workspaceId, mapper.toResponse(block)));
    }
}
