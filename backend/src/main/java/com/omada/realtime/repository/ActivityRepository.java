package com.omada.realtime.repository;
import com.omada.realtime.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, UUID>{
    // Full (unpaged) reads kept for internal use and existing tests.
    List<Activity> findByWorkspaceIdOrderByCreatedAtDesc(String wsId);
    List<Activity> findByBoardIdOrderByCreatedAtDesc(String boardId);

    // Paginated reads for the feed API.
    Page<Activity> findByWorkspaceId(String wsId, Pageable pageable);
    Page<Activity> findByBoardId(String boardId, Pageable pageable);
}
