package com.omada.realtime.service;
import com.omada.realtime.dto.request.CreateBoardRequest;
import com.omada.realtime.dto.response.BoardResponse;
import java.util.List;
import java.util.UUID;

public interface BoardService {
    BoardResponse create(CreateBoardRequest req);
    List<BoardResponse> findByWorkspace(UUID wsId);
    BoardResponse getById(UUID id);
    BoardResponse update(UUID id, CreateBoardRequest req);
    void delete(UUID id);
}