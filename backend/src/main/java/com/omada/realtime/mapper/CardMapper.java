package com.omada.realtime.mapper;

import com.omada.realtime.dto.request.CreateCardRequest;
import com.omada.realtime.dto.response.CardResponse;
import com.omada.realtime.entity.Card;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardMapper {
    Card toEntity(CreateCardRequest req);
    CardResponse toResponse(Card card);
}
