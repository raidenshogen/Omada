package com.omada.realtime.repository;

import com.omada.realtime.entity.BoardMembership;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardMembershipRepository extends MongoRepository<BoardMembership, UUID> {
    List<BoardMembership> findByBoardId(String boardId);
    Optional<BoardMembership> findByBoardIdAndUserId(String boardId, String userId);
    boolean existsByBoardIdAndUserId(String boardId, String userId);
    void deleteByBoardIdAndUserId(String boardId, String userId);
}
