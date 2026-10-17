package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.dto.request.CreateBlockRequest;
import com.omada.realtime.dto.request.CreateDocumentRequest;
import com.omada.realtime.dto.request.UpdateBlockRequest;
import com.omada.realtime.dto.response.BlockResponse;
import com.omada.realtime.dto.response.DocumentResponse;
import com.omada.realtime.repository.BlockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end document/block persistence against real MongoDB: creation,
 * block ordering, optimistic-version conflicts and cascade delete.
 */
public class DocumentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired DocumentService documentService;
    @Autowired BlockService blockService;
    @Autowired BlockRepository blockRepository;

    @Test
    void createDocument_thenAppendOrderedBlocks() {
        DocumentResponse doc = documentService.create("ws-doc-1", new CreateDocumentRequest("Spec", null));
        assertThat(doc.getId()).isNotBlank();

        blockService.create(doc.getId(), block("heading", "Title", 0));
        blockService.create(doc.getId(), block("paragraph", "Body", 1));

        List<BlockResponse> blocks = blockService.findByDocument(doc.getId());
        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).getType()).isEqualTo("heading");
        assertThat(blocks.get(0).getText()).isEqualTo("Title");
        assertThat(blocks.get(1).getPosition()).isEqualTo(1);
    }

    @Test
    void updateBlock_rejectsStaleVersion() {
        DocumentResponse doc = documentService.create("ws-doc-2", new CreateDocumentRequest("Doc", null));
        BlockResponse block = blockService.create(doc.getId(), block("paragraph", "v1", 0));

        UpdateBlockRequest stale = new UpdateBlockRequest("paragraph", "v2", null, null, block.getVersion());
        BlockResponse updated = blockService.update(block.getId(), stale);
        assertThat(updated.getText()).isEqualTo("v2");
        assertThat(updated.getVersion()).isEqualTo(block.getVersion() + 1);

        // Re-submitting the old version is a conflict.
        UpdateBlockRequest conflict = new UpdateBlockRequest("paragraph", "v3", null, null, block.getVersion());
        assertThatThrownBy(() -> blockService.update(block.getId(), conflict))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteDocument_cascadesToBlocks() {
        DocumentResponse doc = documentService.create("ws-doc-3", new CreateDocumentRequest("Temp", null));
        blockService.create(doc.getId(), block("paragraph", "a", 0));
        blockService.create(doc.getId(), block("paragraph", "b", 1));
        assertThat(blockRepository.findByDocumentIdOrderByPositionAsc(doc.getId())).hasSize(2);

        documentService.delete(doc.getId());

        assertThat(blockRepository.findByDocumentIdOrderByPositionAsc(doc.getId())).isEmpty();
        assertThat(documentService.findByWorkspace("ws-doc-3")).isEmpty();
    }

    private static CreateBlockRequest block(String type, String text, int pos) {
        return new CreateBlockRequest(type, text, false, pos);
    }
}
