package com.omada.realtime.entity;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document(collection="boards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    // IDs are generated via IdGenerator.uuid() in service layer
    @Id private UUID id;
    private String workspaceId;
    private String title;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean archived;
    public UUID getId(){return id;} public void setId(UUID id){this.id=id;}
    public String getWorkspaceId(){return workspaceId;} public void setWorkspaceId(String workspaceId){this.workspaceId=workspaceId;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String createdBy){this.createdBy=createdBy;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
    public boolean isArchived(){return archived;} public void setArchived(boolean archived){this.archived=archived;}
}
