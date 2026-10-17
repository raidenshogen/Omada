package com.omada.realtime.mapper;

import com.omada.realtime.dto.request.CreateBoardRequest;
import com.omada.realtime.dto.response.BoardResponse;
import com.omada.realtime.entity.Board;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface BoardMapper {
    @Mapping(target = "id", ignore = true)
    Board toEntity(CreateBoardRequest req);
    BoardResponse toResponse(Board board);
}
