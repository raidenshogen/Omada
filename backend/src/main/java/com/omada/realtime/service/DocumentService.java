package com.omada.realtime.service;

import com.omada.realtime.dto.request.CreateDocumentRequest;
import com.omada.realtime.dto.request.UpdateDocumentRequest;
import com.omada.realtime.dto.response.DocumentResponse;

import java.util.List;

public interface DocumentService {
    DocumentResponse create(String workspaceId, CreateDocumentRequest req);
    List<DocumentResponse> findByWorkspace(String workspaceId);
    DocumentResponse getById(String id);
    DocumentResponse update(String id, UpdateDocumentRequest req);
    void delete(String id);
}
