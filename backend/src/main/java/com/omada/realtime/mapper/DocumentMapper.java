package com.omada.realtime.mapper;

import com.omada.realtime.dto.request.CreateDocumentRequest;
import com.omada.realtime.dto.response.DocumentResponse;
import com.omada.realtime.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspaceId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Document toEntity(CreateDocumentRequest req);

    DocumentResponse toResponse(Document document);
}
