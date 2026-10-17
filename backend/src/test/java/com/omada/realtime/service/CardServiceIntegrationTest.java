package com.omada.realtime.service;

import com.omada.realtime.dto.request.CreateCardRequest;
import com.omada.realtime.dto.request.UpdateCardRequest;
import com.omada.realtime.dto.response.CardResponse;
import com.omada.realtime.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class CardServiceIntegrationTest extends com.omada.realtime.AbstractIntegrationTest {
    @Autowired
    private com.omada.realtime.service.CardService cardService;

    @Autowired
    private CardRepository cardRepository;

    @Test
    void createCard_persistsAndReturnsResponse() {
        CreateCardRequest req = new CreateCardRequest();
        req.setBoardId("board-1");
        req.setListId("list-1");
        req.setTitle("Implement tests");
        req.setDescription("Add Testcontainers tests");

        CardResponse resp = cardService.create(req);
        assertThat(resp).isNotNull();
        assertThat(resp.getTitle()).isEqualTo("Implement tests");

        List<com.omada.realtime.entity.Card> cards = cardRepository.findByBoardId("board-1");
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getTitle()).isEqualTo("Implement tests");
    }

    @Test
    void updateCard_changesTitle() {
        CreateCardRequest create = new CreateCardRequest();
        create.setBoardId("board-2");
        create.setListId("list-2");
        create.setTitle("Old title");
        CardResponse created = cardService.create(create);
        String id = created.getId();

        UpdateCardRequest update = new UpdateCardRequest();
        update.setTitle("New title");
        CardResponse updated = cardService.update(id, update);
        assertThat(updated.getTitle()).isEqualTo("New title");
    }

    @Test
    void findByBoard_returnsCards() {
        CreateCardRequest req = new CreateCardRequest();
        req.setBoardId("board-3");
        req.setListId("list-3");
        req.setTitle("Card A");
        cardService.create(req);

        List<CardResponse> cards = cardService.findByBoard("board-3");
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getTitle()).isEqualTo("Card A");
    }
}
