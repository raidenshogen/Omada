package com.omada.realtime.controller;

import com.omada.realtime.dto.request.AddBoardMemberRequest;
import com.omada.realtime.dto.response.BoardMembershipResponse;
import com.omada.realtime.entity.BoardMembership;
import com.omada.realtime.service.BoardMembershipService;
import com.omada.realtime.entity.User;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.WorkspaceMembershipRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Board membership API.
 * <ul>
 *   <li>Adding/removing members is restricted to the board's workspace OWNER/ADMIN.</li>
 *   <li>Listing members is open to any member of the board's workspace.</li>
 * </ul>
 * Authorization checks run through {@code workspaceSecurity} so RBAC lives in one place.
 */
@RestController
@RequestMapping("/api/boards/{id}/members")
public class BoardMembershipController {

    private final BoardMembershipService service;
    private final UserRepository users;
    private final BoardRepository boards;
    private final WorkspaceMembershipRepository workspaceMemberships;

    public BoardMembershipController(BoardMembershipService service, UserRepository users,
                                     BoardRepository boards, WorkspaceMembershipRepository workspaceMemberships) {
        this.service = service;
        this.users = users;
        this.boards = boards;
        this.workspaceMemberships = workspaceMemberships;
    }

    /** Adds a member to the board (OWNER/ADMIN only). */
    @PostMapping
    @PreAuthorize("@workspaceSecurity.isBoardAdmin(#id, authentication.name)")
    public BoardMembershipResponse add(@PathVariable String id,
                                       @Valid @RequestBody AddBoardMemberRequest req) {
        BoardMembership m = service.add(id, req.getUserId());
        return toResponse(m);
    }

    /** Lists the board's members (any workspace member may view). */
    @GetMapping
    @PreAuthorize("@workspaceSecurity.isBoardMember(#id, authentication.name)")
    public List<BoardMembershipResponse> list(@PathVariable String id) {
        List<BoardMembershipResponse> result = new ArrayList<>();
        Set<String> memberIds = new HashSet<>();
        service.findByBoard(id).forEach(member -> {
            result.add(toResponse(member));
            memberIds.add(member.getUserId());
        });
        boards.findById(java.util.UUID.fromString(id)).ifPresent(board ->
                workspaceMemberships.findByWorkspaceId(board.getWorkspaceId()).stream()
                        .filter(member -> memberIds.add(member.getUserId()))
                        .forEach(member -> result.add(toResponse(id, member))));
        return result;
    }

    /** Removes a member from the board (OWNER/ADMIN only). */
    @DeleteMapping("/{userId}")
    @PreAuthorize("@workspaceSecurity.isBoardAdmin(#id, authentication.name)")
    public void remove(@PathVariable String id, @PathVariable String userId) {
        service.remove(id, userId);
    }

    private BoardMembershipResponse toResponse(BoardMembership m) {
        User user = findUser(m.getUserId());
        return new BoardMembershipResponse(
                m.getId().toString(), m.getBoardId(), m.getUserId(), m.getJoinedAt().toString(),
                user != null ? user.getEmail() : null,
                user != null ? user.getName() : null);
    }

    private BoardMembershipResponse toResponse(String boardId, com.omada.realtime.entity.WorkspaceMembership membership) {
        User user = findUser(membership.getUserId());
        return new BoardMembershipResponse(
                membership.getId().toString(), boardId, membership.getUserId(),
                membership.getJoinedAt().toString(),
                user != null ? user.getEmail() : null,
                user != null ? user.getName() : null);
    }

    private User findUser(String userId) {
        try {
            return users.findById(java.util.UUID.fromString(userId)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
