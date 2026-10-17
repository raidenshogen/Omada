package com.omada.realtime.service.impl;
import com.omada.realtime.dto.request.CreateListRequest;
import com.omada.realtime.dto.response.ListResponse;
import com.omada.realtime.entity.TaskList;
import com.omada.realtime.mapper.ListMapper;

import com.omada.realtime.repository.TaskListRepository;
import com.omada.realtime.service.TaskListService;
import com.omada.realtime.security.IdGenerator;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class TaskTaskListServiceImpl implements TaskListService {
    private final TaskListRepository repo;
    private final ListMapper mapper;
    public TaskTaskListServiceImpl(TaskListRepository repo, ListMapper mapper){ this.repo=repo; this.mapper=mapper; }
    @Override public ListResponse create(CreateListRequest req){
        TaskList list = mapper.toEntity(req);
        list.setId(IdGenerator.uuid());
        list.setBoardId(req.boardId);
        list.setCreatedAt(Instant.now());
        list = repo.save(list);
        return mapper.toResponse(list);
    }
    @Override public List<ListResponse> findByBoard(String boardId){
        return repo.findByBoardIdOrderByPositionAsc(boardId).stream().map(mapper::toResponse).collect(Collectors.toList());
    }
    @Override public ListResponse getById(String id){
        TaskList list = repo.findById(UUID.fromString(id)).orElseThrow();
        return mapper.toResponse(list);
    }
    @Override public ListResponse update(String id, CreateListRequest req){
        TaskList list = repo.findById(UUID.fromString(id)).orElseThrow();
        if (req.title != null) list.setTitle(req.title);
        if (req.position != 0) list.setPosition(req.position);
        list = repo.save(list);
        return mapper.toResponse(list);
    }
    @Override public void delete(String id){
        repo.deleteById(UUID.fromString(id));
    }
}
