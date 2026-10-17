package com.omada.realtime.repository; import com.omada.realtime.entity.Board; import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardRepository extends MongoRepository<Board, UUID> {
    // workspaceId is stored as String in the board document — query it as String,
    // otherwise the BSON type (String vs UUID) never matches.
    List<Board> findByWorkspaceId(String workspaceId);
}