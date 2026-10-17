package com.omada.realtime.entity;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.omada.realtime.security.IdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document(collection="memberships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Membership {
    // IDs are generated via IdGenerator.uuid() in service layer
    @Id private UUID id;
    private String organizationId;
    private String userId;
    private String role;
    private Instant joinedAt;
   }
