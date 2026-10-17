package com.omada.realtime.entity;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.omada.realtime.security.IdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection="cards")
@CompoundIndex(name = "list_pos_idx", def = "{'listId': 1, 'position': 1}")
@CompoundIndex(name = "board_pos_idx", def = "{'boardId': 1, 'position': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    // IDs are generated via IdGenerator.uuid() in service layer
    @Id private UUID id;
    private String listId;
    private String boardId;
    private String title;
    private String description;
    private int position;
    private List<String> assignees;
    private List<Label> labels;
    private Instant dueDate;
    private int version;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    public static class Label { private String name; private String color; public String getName(){return name;} public void setName(String name){this.name=name;} public String getColor(){return color;} public void setColor(String color){this.color=color;}}
    }
