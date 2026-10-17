package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.CreateCardRequest;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Card;
import com.omada.realtime.mapper.CardMapper;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.service.ActivityProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies card changes publish an Activity carrying the owning workspace id
 * and the acting user — the regression that previously left the workspace feed empty.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardServiceImplActivityTest {

    @Mock CardRepository repo;
    @Mock BoardRepository boardRepo;
    @Mock CardMapper mapper;
    @Mock ActivityProducer producer;
    @Mock ApplicationEventPublisher events;

    CardServiceImpl service;

    static final UUID BOARD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final String WS = "33333333-3333-3333-3333-333333333333";

    @BeforeEach
    void setUp() {
        service = new CardServiceImpl(repo, boardRepo, mapper, producer, events);
        when(mapper.toEntity(any())).thenReturn(Card.builder().boardId(BOARD_ID.toString()).listId("l1").build());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(boardRepo.findById(BOARD_ID))
                .thenReturn(Optional.of(Board.builder().id(BOARD_ID).workspaceId(WS).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPublishesActivityWithWorkspaceIdAndActor() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-9", null));

        service.create(new CreateCardRequest());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(producer).publish(captor.capture());
        Activity activity = captor.getValue();
        assertThat(activity.getType()).isEqualTo("CARD_CREATED");
        assertThat(activity.getWorkspaceId()).isEqualTo(WS);
        assertThat(activity.getBoardId()).isEqualTo(BOARD_ID.toString());
        assertThat(activity.getActorId()).isEqualTo("user-9");
    }

    @Test
    void createFallsBackToSystemActorWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        service.create(new CreateCardRequest());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(producer).publish(captor.capture());
        assertThat(captor.getValue().getActorId()).isEqualTo("system");
    }

    @Test
    void createWithUnknownBoardPublishesActivityWithoutWorkspace() {
        when(mapper.toEntity(any())).thenReturn(Card.builder().boardId("not-a-uuid").listId("l1").build());

        service.create(new CreateCardRequest());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(producer).publish(captor.capture());
        assertThat(captor.getValue().getWorkspaceId()).isNull();
    }
}
