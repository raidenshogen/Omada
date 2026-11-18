package com.omada.realtime.repository; import com.omada.realtime.entity.Card; import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends MongoRepository<Card, UUID>{
    List<Card> findByBoardId(String boardId);
    void deleteByBoardId(String boardId);
    List<Card> findByListId(String listId);
    // ordered reads for the Kanban view and in-column reordering
    List<Card> findByListIdOrderByPositionAsc(String listId);
    List<Card> findByBoardIdOrderByPositionAsc(String boardId); }