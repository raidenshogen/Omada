package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.CreateBoardRequest;
import com.omada.realtime.dto.response.BoardResponse;
import com.omada.realtime.entity.Board;
import com.omada.realtime.mapper.BoardMapper;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.service.BoardService;
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

    public BoardServiceImpl(BoardRepository boardRepository, BoardMapper boardMapper) {
        this.boardRepository = boardRepository;
        this.boardMapper = boardMapper;
    }

    @Override
    public BoardResponse create(CreateBoardRequest req) {
        Board board = boardMapper.toEntity(req);
        board.setId(UUID.randomUUID());
        board.setCreatedAt(Instant.now());
        board.setUpdatedAt(Instant.now());
        board = boardRepository.save(board);
        return boardMapper.toResponse(board);
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