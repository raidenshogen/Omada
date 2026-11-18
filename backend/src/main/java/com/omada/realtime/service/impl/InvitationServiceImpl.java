package com.omada.realtime.service.impl;

import com.omada.realtime.entity.Invitation;
import com.omada.realtime.entity.User;
import com.omada.realtime.repository.InvitationRepository;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.repository.WorkspaceMembershipRepository;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.InvitationService;
import com.omada.realtime.service.WorkspaceMembershipService;
import com.omada.realtime.service.BoardMembershipService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Invitation business logic.
 * - token is UUIDv4, expiresAt is 24h from creation
 * - acceptance creates a WorkspaceMembership exactly once (idempotent)
 * - role is validated server-side against the allowed role set
 */
@Service
public class InvitationServiceImpl implements InvitationService {
    private static final Logger log = LoggerFactory.getLogger(InvitationServiceImpl.class);

    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String DECLINED = "DECLINED";
    private static final String EXPIRED = "EXPIRED";
    private static final String REVOKED = "REVOKED";

    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "MEMBER", "VIEWER");
    private static final Duration TTL = Duration.ofHours(24);

    private final InvitationRepository invitations;
    private final WorkspaceMembershipService memberships;
    private final WorkspaceMembershipRepository membershipRepository;
    private final BoardMembershipService boardMemberships;
    private final UserRepository users;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String frontendUrl;
    private final String mailFrom;

    public InvitationServiceImpl(InvitationRepository invitations,
                                 WorkspaceMembershipService memberships,
                                 WorkspaceMembershipRepository membershipRepository,
                                 BoardMembershipService boardMemberships,
                                 UserRepository users,
                                 ObjectProvider<JavaMailSender> mailSenders,
                                 @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                                 @Value("${app.mail.from:omada@localhost}") String mailFrom) {
        this.invitations = invitations;
        this.memberships = memberships;
        this.membershipRepository = membershipRepository;
        this.boardMemberships = boardMemberships;
        this.users = users;
        this.mailSenders = mailSenders;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
    }

    @Override
    public Invitation create(String workspaceId, String email, String role, String boardId) {
        String normalized = normalizeRole(role);
        Invitation inv = Invitation.builder()
                .id(IdGenerator.uuid())
                .token(IdGenerator.uuid())
                .email(email)
                .workspaceId(workspaceId)
                .boardId(boardId)
                .role(normalized)
                .status(PENDING)
                .expiresAt(Instant.now().plus(TTL))
                .build();
        Invitation saved = invitations.save(inv);
        sendInvitationEmail(saved, currentUserId());
        return saved;
    }

    private void sendInvitationEmail(Invitation invitation, String inviterUserId) {
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailSender == null) {
            log.warn("INVITATION_EMAIL_SKIPPED no mail sender configured invitationId={}",
                    invitation.getId());
            return;
        }
        User inviter = null;
        if (inviterUserId != null) {
            try {
                inviter = users.findById(UUID.fromString(inviterUserId)).orElse(null);
            } catch (IllegalArgumentException ignored) {
                log.warn("INVITATION_INVITER_LOOKUP_FAILED invitationId={}", invitation.getId());
            }
        }
        String inviterName = inviter != null && inviter.getName() != null
                ? inviter.getName()
                : inviter != null ? inviter.getEmail() : "A teammate";
        String link = frontendUrl.replaceAll("/$", "") + "/invite/" + invitation.getToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(invitation.getEmail());
        message.setSubject(inviterName + " invited you to collaborate in Omada");
        message.setText("""
                %s invited you to collaborate in Omada.

                You have been invited as: %s
                Accept the invitation here:
                %s

                This invitation expires in 24 hours.
                """.formatted(inviterName, invitation.getRole(), link));
        try {
            mailSender.send(message);
            log.info("INVITATION_EMAIL_SENT recipient={} invitationId={}",
                    invitation.getEmail(), invitation.getId());
        } catch (RuntimeException e) {
            log.warn("INVITATION_EMAIL_FAILED recipient={} invitationId={} reason={}",
                    invitation.getEmail(), invitation.getId(), e.getMessage());
        }
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    @Override
    public List<Invitation> listByWorkspace(String workspaceId) {
        return invitations.findByWorkspaceId(workspaceId);
    }

    @Override
    public Invitation getByToken(UUID token) {
        return invitations.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
    }

    @Override
    public Invitation accept(UUID token) {
        Invitation inv = invitations.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (ACCEPTED.equals(inv.getStatus())) {
            // Idempotent: a second acceptance does not duplicate the membership.
            return inv;
        }
        if (!PENDING.equals(inv.getStatus())) {
            throw new IllegalArgumentException("Invitation is not pending (status: " + inv.getStatus() + ")");
        }
        if (Instant.now().isAfter(inv.getExpiresAt())) {
            inv.setStatus(EXPIRED);
            invitations.save(inv);
            throw new IllegalArgumentException("Invitation has expired");
        }

        User user = users.findByEmail(inv.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("No user with email " + inv.getEmail());
        }
        String userId = user.getId().toString();
        if (membershipRepository.existsByWorkspaceIdAndUserId(inv.getWorkspaceId(), userId)) {
            // Already a member through another path: mark accepted without creating a duplicate.
            addToBoardIfRequested(inv, userId);
            inv.setStatus(ACCEPTED);
            return invitations.save(inv);
        }
        memberships.create(inv.getWorkspaceId(), userId, inv.getRole());
        addToBoardIfRequested(inv, userId);
        inv.setStatus(ACCEPTED);
        return invitations.save(inv);
    }

    private void addToBoardIfRequested(Invitation invitation, String userId) {
        if (invitation.getBoardId() != null && !invitation.getBoardId().isBlank()) {
            try {
                boardMemberships.add(invitation.getBoardId(), userId);
            } catch (IllegalArgumentException alreadyMember) {
                // Acceptance is idempotent when the board membership already exists.
                if (!alreadyMember.getMessage().contains("already a member")) {
                    throw alreadyMember;
                }
            }
        }
    }

    @Override
    public Invitation decline(UUID token) {
        Invitation inv = invitations.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        if (!PENDING.equals(inv.getStatus())) {
            throw new IllegalArgumentException("Invitation is not pending (status: " + inv.getStatus() + ")");
        }
        inv.setStatus(DECLINED);
        return invitations.save(inv);
    }

    @Override
    public void revoke(UUID token) {
        Invitation inv = invitations.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        if (REVOKED.equals(inv.getStatus())) {
            return;
        }
        if (ACCEPTED.equals(inv.getStatus())) {
            User user = users.findByEmail(inv.getEmail());
            if (user != null) {
                String userId = user.getId().toString();
                membershipRepository.deleteByWorkspaceIdAndUserId(inv.getWorkspaceId(), userId);
                if (inv.getBoardId() != null && !inv.getBoardId().isBlank()) {
                    try {
                        boardMemberships.remove(inv.getBoardId(), userId);
                    } catch (IllegalArgumentException ignored) {
                        log.debug("INVITATION_BOARD_MEMBERSHIP_ALREADY_ABSENT invitationId={}", inv.getId());
                    }
                }
            }
        }
        inv.setStatus(REVOKED);
        invitations.save(inv);
        sendRevocationEmail(inv);
    }

    private void sendRevocationEmail(Invitation invitation) {
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailSender == null) {
            log.warn("INVITATION_REVOCATION_EMAIL_SKIPPED no mail sender configured invitationId={}",
                    invitation.getId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(invitation.getEmail());
        message.setSubject("Your Omada invitation is no longer available");
        message.setText("""
                The invitation to join an Omada workspace is no longer available.

                The workspace owner or administrator revoked the invitation. The invitation link can no longer be used.
                If you believe this was a mistake, contact the person who invited you.
                """);
        try {
            mailSender.send(message);
            log.info("INVITATION_REVOCATION_EMAIL_SENT recipient={} invitationId={}",
                    invitation.getEmail(), invitation.getId());
        } catch (RuntimeException e) {
            log.warn("INVITATION_REVOCATION_EMAIL_FAILED recipient={} invitationId={} reason={}",
                    invitation.getEmail(), invitation.getId(), e.getMessage());
        }
    }

    /** Role must be one of OWNER, ADMIN, MEMBER, VIEWER (case-insensitive). */
    private String normalizeRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        String upper = role.trim().toUpperCase();
        if (!VALID_ROLES.contains(upper)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        return upper;
    }
}
