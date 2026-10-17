package com.omada.realtime.security;

import com.omada.realtime.entity.Document;
import com.omada.realtime.repository.DocumentRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RBAC checks for documents/blocks, used by @PreAuthorize. A document only
 * carries its workspaceId, so we walk up to the workspace's organization and
 * resolve the caller's membership (read = VIEWER+, edit = MEMBER+).
 * Unknown or malformed ids resolve to false (never throw) → 403.
 */
@Component("documentSecurity")
public class DocumentSecurity {

    private final DocumentRepository documents;
    private final WorkspaceSecurity workspaces;

    public DocumentSecurity(DocumentRepository documents, WorkspaceSecurity workspaces) {
        this.documents = documents;
        this.workspaces = workspaces;
    }

    /** Read a document: any member of its workspace's organization. */
    public boolean canViewDocument(String documentId, String userId) {
        Document doc = doc(documentId);
        return doc != null && workspaces.canViewWorkspace(doc.getWorkspaceId(), userId);
    }

    /** Edit a document or its blocks: MEMBER+. */
    public boolean canEditDocument(String documentId, String userId) {
        Document doc = doc(documentId);
        return doc != null && workspaces.canWriteWorkspaceContent(doc.getWorkspaceId(), userId);
    }

    private Document doc(String documentId) {
        try {
            return documents.findById(UUID.fromString(documentId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
