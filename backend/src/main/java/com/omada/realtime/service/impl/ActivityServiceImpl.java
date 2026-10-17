package com.omada.realtime.service.impl;
import com.omada.realtime.dto.response.ActivityResponse;
import com.omada.realtime.dto.response.PageResponse;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.mapper.ActivityMapper;
import com.omada.realtime.repository.ActivityRepository;
import com.omada.realtime.service.ActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityServiceImpl implements ActivityService {
    private final ActivityRepository repo;
    private final ActivityMapper mapper;

    public ActivityServiceImpl(ActivityRepository repo, ActivityMapper mapper){ this.repo=repo; this.mapper=mapper; }

    @Override public PageResponse<ActivityResponse> findByWorkspace(String workspaceId, int page, int size){
        String ws = normalizeId(workspaceId);
        if (ws == null) return PageResponse.empty(page, size);
        return toPage(repo.findByWorkspaceId(ws, pageable(page, size)), page, size);
    }

    @Override public PageResponse<ActivityResponse> findByBoard(String boardId, int page, int size){
        String b = normalizeId(boardId);
        if (b == null) return PageResponse.empty(page, size);
        return toPage(repo.findByBoardId(b, pageable(page, size)), page, size);
    }

    private static Pageable pageable(int page, int size){
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<ActivityResponse> toPage(Page<Activity> result, int page, int size){
        List<ActivityResponse> content = result.getContent().stream().map(mapper::toResponse).toList();
        return PageResponse.of(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /** Normalizes a UUID string; returns null for malformed ids so callers return an empty page. */
    private static String normalizeId(String id){
        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
