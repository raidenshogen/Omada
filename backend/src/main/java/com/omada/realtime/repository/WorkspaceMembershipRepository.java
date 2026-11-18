package com.omada.realtime.repository;

import com.omada.realtime.entity.WorkspaceMembership;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMembershipRepository extends MongoRepository<WorkspaceMembership, UUID> {
    List<WorkspaceMembership> findByWorkspaceId(String workspaceId);
    List<WorkspaceMembership> findByUserId(String userId);
    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(String workspaceId, String userId);
    boolean existsByWorkspaceIdAndUserId(String workspaceId, String userId);
    void deleteByWorkspaceIdAndUserId(String workspaceId, String userId);
}
