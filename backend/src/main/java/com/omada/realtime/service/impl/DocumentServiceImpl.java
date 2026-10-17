package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.CreateDocumentRequest;
import com.omada.realtime.dto.request.UpdateDocumentRequest;
import com.omada.realtime.dto.response.DocumentResponse;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.entity.Document;
import com.omada.realtime.event.DocumentChangedEvent;
import com.omada.realtime.mapper.DocumentMapper;
import com.omada.realtime.repository.BlockRepository;
import com.omada.realtime.repository.DocumentRepository;
import com.omada.realtime.security.ActorContext;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.ActivityProducer;
import com.omada.realtime.service.DocumentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repo;
    private final BlockRepository blockRepo;
    private final DocumentMapper mapper;
    private final ActivityProducer producer;
    private final ApplicationEventPublisher events;

    public DocumentServiceImpl(DocumentRepository repo, BlockRepository blockRepo, DocumentMapper mapper,
                               ActivityProducer producer, ApplicationEventPublisher events) {
        this.repo = repo;
        this.blockRepo = blockRepo;
        this.mapper = mapper;
        this.producer = producer;
        this.events = events;
    }

    @Override
    public DocumentResponse create(String workspaceId, CreateDocumentRequest req) {
        Document doc = mapper.toEntity(req);
        doc.setId(IdGenerator.uuid());
        doc.setWorkspaceId(workspaceId);
        doc.setCreatedBy(ActorContext.id());
        doc.setVersion(1);
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        doc = repo.save(doc);
        emit(doc, "DOCUMENT_CREATED");
        return mapper.toResponse(doc);
    }

    @Override
    public List<DocumentResponse> findByWorkspace(String workspaceId) {
        return repo.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).stream()
                .map(mapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public DocumentResponse getById(String id) {
        Document doc = repo.findById(UUID.fromString(id)).orElseThrow();
        return mapper.toResponse(doc);
    }

    @Override
    public DocumentResponse update(String id, UpdateDocumentRequest req) {
        Document doc = repo.findById(UUID.fromString(id)).orElseThrow();
        if (req.title != null) doc.setTitle(req.title);
        doc.setUpdatedAt(Instant.now());
        doc.setVersion(doc.getVersion() + 1);
        doc = repo.save(doc);
        emit(doc, "DOCUMENT_UPDATED");
        return mapper.toResponse(doc);
    }

    @Override
    public void delete(String id) {
        Document doc = repo.findById(UUID.fromString(id)).orElseThrow();
        blockRepo.deleteByDocumentId(id);
        repo.delete(doc);
    }

    /** Publishes DOCUMENT_CREATED/UPDATED to Kafka + the live feed and notifies open editors. */
    private void emit(Document doc, String type) {
        producer.publish(Activity.builder()
                .id(IdGenerator.uuid())
                .workspaceId(doc.getWorkspaceId())
                .boardId(doc.getBoardId())
                .actorId(ActorContext.idOrSystem())
                .type(type)
                .entityId(doc.getId().toString())
                .createdAt(Instant.now())
                .build());
        events.publishEvent(new DocumentChangedEvent(type, doc.getId().toString(),
                doc.getWorkspaceId(), mapper.toResponse(doc)));
    }
}
