package com.omada.realtime.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequest {
    public String title;
    public String description;
    public Integer position;
    public String listId;
    public List<String> assignees;
    public Integer version; }