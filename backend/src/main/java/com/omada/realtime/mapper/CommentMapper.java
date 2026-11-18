package com.omada.realtime.mapper;

import com.omada.realtime.dto.request.CreateCommentRequest;
import com.omada.realtime.dto.response.CommentResponse;
import com.omada.realtime.entity.Comment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {
   Comment toEntity(CreateCommentRequest req);
   CommentResponse toResponse(Comment c);
}