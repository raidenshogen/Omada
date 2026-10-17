package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.CreateCommentRequest;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Card;
import com.omada.realtime.entity.Comment;
import com.omada.realtime.mapper.CommentMapper;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.CommentRepository;
import com.omada.realtime.service.ActivityProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comments must (a) record their author and (b) publish a COMMENT_ADDED
 * activity carrying the owning workspace so the feed stays populated.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceImplActivityTest {

    @Mock CommentRepository repo;
    @Mock CardRepository cardRepo;
    @Mock BoardRepository boardRepo;
    @Mock com.omada.realtime.mapper.CommentMapper mapper;
    @Mock ActivityProducer producer;

    CommentServiceImpl service;

    static final UUID CARD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID BOARD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final String WS = "33333333-3333-3333-3333-333333333333";
    static final String USER_UUID = "44444444-4444-4444-4444-444444444444";

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(repo, cardRepo, boardRepo, mapper, producer);
        when(mapper.toEntity(any())).thenReturn(Comment.builder().body("hi").build());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cardRepo.findById(CARD_ID)).thenReturn(Optional.of(Card.builder().id(CARD_ID).boardId(BOARD_ID.toString()).build()));
        when(boardRepo.findById(BOARD_ID)).thenReturn(Optional.of(Board.builder().id(BOARD_ID).workspaceId(WS).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRecordsAuthorAndPublishesCommentActivity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_UUID, null));

        service.create(CARD_ID.toString(), new com.omada.realtime.dto.request.CreateCommentRequest("hi"));

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getAuthorId()).isEqualTo(UUID.fromString(USER_UUID));

        ArgumentCaptor<Activity> activity = ArgumentCaptor.forClass(Activity.class);
        verify(producer).publish(activity.capture());
        assertThat(activity.getValue().getType()).isEqualTo("COMMENT_ADDED");
        assertThat(activity.getValue().getWorkspaceId()).isEqualTo(WS);
        assertThat(activity.getValue().getBoardId()).isEqualTo(BOARD_ID.toString());
        assertThat(activity.getValue().getActorId()).isEqualTo(USER_UUID);
    }
}
