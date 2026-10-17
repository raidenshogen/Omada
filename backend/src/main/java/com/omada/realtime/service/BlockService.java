package com.omada.realtime.service;

import com.omada.realtime.dto.request.CreateBlockRequest;
import com.omada.realtime.dto.request.UpdateBlockRequest;
import com.omada.realtime.dto.response.BlockResponse;

import java.util.List;

public interface BlockService {
    List<BlockResponse> findByDocument(String documentId);
    BlockResponse create(String documentId, CreateBlockRequest req);
    BlockResponse update(String id, UpdateBlockRequest req);
    void delete(String id);
}
