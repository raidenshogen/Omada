package com.omada.realtime.repository;
import com.omada.realtime.entity.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends MongoRepository<Document, UUID> {
    List<Document> findByWorkspaceIdOrderByUpdatedAtDesc(String workspaceId);
}
