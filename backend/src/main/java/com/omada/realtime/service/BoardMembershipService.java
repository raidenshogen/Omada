package com.omada.realtime.service;

import com.omada.realtime.entity.BoardMembership;

import java.util.List;

/**
 * Board membership management.
 */
public interface BoardMembershipService {

    /** Adds a user to a board. Rejects if already a member. */
    BoardMembership add(String boardId, String userId);

    /** Lists all members of a board. */
    List<BoardMembership> findByBoard(String boardId);

    /** Removes a member from a board. */
    void remove(String boardId, String userId);
}
