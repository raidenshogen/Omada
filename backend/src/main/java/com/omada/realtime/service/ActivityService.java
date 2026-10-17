package com.omada.realtime.service;
import com.omada.realtime.dto.response.ActivityResponse;
import com.omada.realtime.dto.response.PageResponse;

public interface ActivityService {
    PageResponse<ActivityResponse> findByWorkspace(String workspaceId, int page, int size);
    PageResponse<ActivityResponse> findByBoard(String boardId, int page, int size);
}
