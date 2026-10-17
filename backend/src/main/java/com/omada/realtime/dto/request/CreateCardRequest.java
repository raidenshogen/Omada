package com.omada.realtime.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateCardRequest {
    public String title;
    public String description;
    public Integer position;
    public List<String> assignees;
    public String boardId;
    public String listId;
    public List<LabelDto> labels;
    public Instant dueDate;

    public static class LabelDto {
        public String name;
        public String color;
    }
}