package com.omada.realtime.service;

import com.omada.realtime.dto.request.CreateBoardRequest;
import com.omada.realtime.dto.response.BoardResponse;
import com.omada.realtime.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

public class BoardServiceIntegrationTest extends com.omada.realtime.AbstractIntegrationTest {
    @Autowired
    private com.omada.realtime.service.BoardService boardService;

    @Autowired
    private BoardRepository boardRepository;

    @Test
    void createBoard_persistsAndMapsResponse() {
        CreateBoardRequest req = new CreateBoardRequest();
        req.setWorkspaceId("11111111-1111-1111-1111-111111111111");
        req.setTitle("Sprint Board");
        req.setDescription("Board for sprint");

        BoardResponse resp = boardService.create(req);
        assertThat(resp).isNotNull();
        assertThat(resp.getTitle()).isEqualTo("Sprint Board");

        List<com.omada.realtime.entity.Board> boards = boardRepository
                .findByWorkspaceId("11111111-1111-1111-1111-111111111111");
        assertThat(boards).hasSize(1);
        assertThat(boards.get(0).getTitle()).isEqualTo("Sprint Board");
    }

    @Test
    void findByWorkspace_returnsEmptyWhenNone() {
        List<BoardResponse> result = boardService
                .findByWorkspace(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        assertThat(result).isEmpty();
    }
}
