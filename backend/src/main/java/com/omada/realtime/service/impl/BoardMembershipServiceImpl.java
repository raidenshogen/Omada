package com.omada.realtime.service.impl;

import com.omada.realtime.entity.BoardMembership;
import com.omada.realtime.repository.BoardMembershipRepository;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.BoardMembershipService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Board membership business logic.
 * - adding a member that already exists is rejected
 */
@Service
public class BoardMembershipServiceImpl implements BoardMembershipService {

    private final BoardMembershipRepository memberships;

    public BoardMembershipServiceImpl(BoardMembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Override
    public BoardMembership add(String boardId, String userId) {
        if (memberships.existsByBoardIdAndUserId(boardId, userId)) {
            throw new IllegalArgumentException("User is already a member of this board");
        }
        BoardMembership m = BoardMembership.builder()
                .id(IdGenerator.uuid())
                .boardId(boardId)
                .userId(userId)
                .joinedAt(Instant.now())
                .build();
        return memberships.save(m);
    }

    @Override
    public List<BoardMembership> findByBoard(String boardId) {
        return memberships.findByBoardId(boardId);
    }

    @Override
    public void remove(String boardId, String userId) {
        BoardMembership m = memberships.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member of this board"));
        memberships.delete(m);
    }
}
