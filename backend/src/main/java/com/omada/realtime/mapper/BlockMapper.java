package com.omada.realtime.mapper;

import com.omada.realtime.dto.request.CreateBlockRequest;
import com.omada.realtime.dto.response.BlockResponse;
import com.omada.realtime.entity.Block;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BlockMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Block toEntity(CreateBlockRequest req);

    BlockResponse toResponse(Block block);
}
