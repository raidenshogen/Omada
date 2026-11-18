package com.omada.realtime.repository;
import com.omada.realtime.entity.TaskList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskListRepository extends MongoRepository<TaskList, UUID>{ 
    // boardId is stored as String in the list document — query it as String
    List<TaskList> findByBoardId(String boardId); 
    void deleteByBoardId(String boardId);
    // ordered read for board columns (Trello-like left-to-right order)
    List<TaskList> findByBoardIdOrderByPositionAsc(String boardId);
}