package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.CreateBoardRequest;
import com.omada.realtime.dto.response.BoardResponse;
import com.omada.realtime.dto.request.CreateCardRequest;
import com.omada.realtime.dto.request.CreateListRequest;
import com.omada.realtime.entity.Board;
import com.omada.realtime.mapper.BoardMapper;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.TaskListRepository;
import com.omada.realtime.service.BoardService;
import com.omada.realtime.service.CardService;
import com.omada.realtime.service.TaskListService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardMapper boardMapper;
    private final TaskListService taskListService;
    private final CardService cardService;
    private final TaskListRepository taskListRepository;
    private final CardRepository cardRepository;

    public BoardServiceImpl(BoardRepository boardRepository, BoardMapper boardMapper,
                            TaskListService taskListService, CardService cardService,
                            TaskListRepository taskListRepository, CardRepository cardRepository) {
        this.boardRepository = boardRepository;
        this.boardMapper = boardMapper;
        this.taskListService = taskListService;
        this.cardService = cardService;
        this.taskListRepository = taskListRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    public BoardResponse create(CreateBoardRequest req) {
        Board board = boardMapper.toEntity(req);
        board.setId(UUID.randomUUID());
        board.setCreatedAt(Instant.now());
        board.setUpdatedAt(Instant.now());
        board = boardRepository.save(board);
        try {
            seedDefaultContent(board.getId().toString());
        } catch (RuntimeException e) {
            cardRepository.deleteByBoardId(board.getId().toString());
            taskListRepository.deleteByBoardId(board.getId().toString());
            boardRepository.deleteById(board.getId());
            throw e;
        }
        return boardMapper.toResponse(board);
    }

    private void seedDefaultContent(String boardId) {
        String[] listTitles = {"Todo", "Doing", "Done"};
        String[] cardTitles = {"Design the homepage", "Set up the project structure", "Create the workspace"};
        for (int i = 0; i < listTitles.length; i++) {
            CreateListRequest listRequest = new CreateListRequest();
            listRequest.boardId = boardId;
            listRequest.title = listTitles[i];
            listRequest.position = i;
            String listId = taskListService.create(listRequest).getId();

            CreateCardRequest cardRequest = new CreateCardRequest();
            cardRequest.boardId = boardId;
            cardRequest.listId = listId;
            cardRequest.title = cardTitles[i];
            cardRequest.description = "Example task in " + listTitles[i];
            cardRequest.position = 0;
            cardService.create(cardRequest);
        }
    }

    @Override
    public List<BoardResponse> findByWorkspace(UUID wsId) {
        return boardRepository.findByWorkspaceId(wsId.toString())
                .stream()
                .map(boardMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BoardResponse getById(UUID id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Board not found: " + id));
        return boardMapper.toResponse(board);
    }

    @Override
    public BoardResponse update(UUID id, CreateBoardRequest req) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Board not found: " + id));
        if (req.title != null) board.setTitle(req.title);
        if (req.description != null) board.setDescription(req.description);
        board.setUpdatedAt(Instant.now());
        board = boardRepository.save(board);
        return boardMapper.toResponse(board);
    }

    @Override
    public void delete(UUID id) {
        boardRepository.deleteById(id);
    }
}