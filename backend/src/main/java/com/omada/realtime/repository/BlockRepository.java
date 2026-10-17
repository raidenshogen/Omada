package com.omada.realtime.repository;
import com.omada.realtime.entity.Block;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Blocks are queried by their owning document, ordered by position. */
@Repository
public interface BlockRepository extends MongoRepository<Block, UUID> {
    List<Block> findByDocumentIdOrderByPositionAsc(String documentId);
    void deleteByDocumentId(String documentId);
}
