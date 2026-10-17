package com.omada.realtime.repository; import com.omada.realtime.entity.Workspace; import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends MongoRepository<Workspace, UUID>{ List<Workspace> findByOrganizationId(String orgId); }