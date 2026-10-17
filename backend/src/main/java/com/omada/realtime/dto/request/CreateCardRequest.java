package com.omada.realtime.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateCardRequest {
    @NotBlank
    public String title;
    public String description;
    public Integer position;
    public List<String> assignees;
    @NotBlank
    public String boardId;
    @NotBlank
    public String listId;
    public List<LabelDto> labels;
    public Instant dueDate;

    public static class LabelDto {
        public String name;
        public String color;
    }
}