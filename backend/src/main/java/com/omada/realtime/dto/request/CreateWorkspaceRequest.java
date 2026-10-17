package com.omada.realtime.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateWorkspaceRequest {
    public String organizationId;
    public String name;
    public String description; }