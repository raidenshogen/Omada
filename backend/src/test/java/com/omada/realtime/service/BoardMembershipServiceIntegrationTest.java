package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.entity.BoardMembership;
import com.omada.realtime.repository.BoardMembershipRepository;
import com.omada.realtime.security.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for board membership: add/remove/find-by-board,
 * duplicate-member rejection, and unknown-member removal.
 */
public class BoardMembershipServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private BoardMembershipService boardMembershipService;

    @Autowired
    private BoardMembershipRepository boardMembershipRepository;

    private String newBoardId() {
        return "board-" + UUID.randomUUID();
    }

    private String newUserId() {
        return "user-" + UUID.randomUUID();
    }

    @Test
    void add_storesMembershipAndListsMembers() {
        String boardId = newBoardId();
        String userId = newUserId();
        BoardMembership m = boardMembershipService.add(boardId, userId);

        assertThat(m.getId()).isNotNull();
        assertThat(m.getBoardId()).isEqualTo(boardId);
        assertThat(m.getUserId()).isEqualTo(userId);
        assertThat(m.getJoinedAt()).isNotNull();

        List<BoardMembership> members = boardMembershipService.findByBoard(boardId);
        assertThat(members).extracting(BoardMembership::getUserId)
                .containsExactly(userId);
    }

    @Test
    void add_multipleMembers_listedTogether() {
        String boardId = newBoardId();
        String otherBoardId = newBoardId();
        String userA = newUserId();
        String userB = newUserId();
        String userC = newUserId();

        boardMembershipService.add(boardId, userA);
        boardMembershipService.add(boardId, userB);
        boardMembershipService.add(otherBoardId, userC);

        List<BoardMembership> members = boardMembershipService.findByBoard(boardId);
        assertThat(members).extracting(BoardMembership::getUserId)
                .containsExactlyInAnyOrder(userA, userB);
        assertThat(boardMembershipService.findByBoard(otherBoardId))
                .extracting(BoardMembership::getUserId)
                .containsExactly(userC);
    }

    @Test
    void remove_deletesMembership() {
        String boardId = newBoardId();
        String userId = newUserId();
        boardMembershipService.add(boardId, userId);
        boardMembershipService.remove(boardId, userId);

        assertThat(boardMembershipService.findByBoard(boardId)).isEmpty();
        assertThat(boardMembershipRepository.findByBoardIdAndUserId(boardId, userId))
                .isEmpty();
    }

    @Test
    void addDuplicate_throws() {
        String boardId = newBoardId();
        String userId = newUserId();
        boardMembershipService.add(boardId, userId);

        assertThatThrownBy(() -> boardMembershipService.add(boardId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void removeUnknown_throws() {
        String boardId = newBoardId();
        assertThatThrownBy(() -> boardMembershipService.remove(boardId, "ghost-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a member of this board");
    }
}
