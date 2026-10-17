package com.omada.realtime.entity;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.omada.realtime.security.IdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document(collection="organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    // IDs are generated via IdGenerator.uuid() in service layer
    @Id private UUID id;
    @Indexed(unique=true) private String slug;
    private String name;
    private String createdBy;
    private Instant createdAt;
    }
