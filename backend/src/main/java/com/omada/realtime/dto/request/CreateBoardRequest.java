package com.omada.realtime.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateBoardRequest {
    public String workspaceId;
    public String title;
    public String description; }