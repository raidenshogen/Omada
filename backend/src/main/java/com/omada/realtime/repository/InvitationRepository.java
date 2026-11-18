package com.omada.realtime.repository;

import com.omada.realtime.entity.Invitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends MongoRepository<Invitation, UUID> {
    List<Invitation> findByWorkspaceId(String workspaceId);
    List<Invitation> findByWorkspaceIdAndStatus(String workspaceId, String status);
    Optional<Invitation> findByToken(UUID token);
    boolean existsByToken(UUID token);
    void deleteByToken(UUID token);
}
