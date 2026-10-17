package com.omada.realtime.dto.response;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class CardResponse {
    public String id;
    public String title;
    public String description;
    public int position;
    public int version; }