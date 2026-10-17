package com.omada.realtime.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** One organization member: user identity + org role. */
@Data @NoArgsConstructor @AllArgsConstructor
public class MembershipResponse {
    public String id;
    public String organizationId;
    public String userId;
    public String email;
    public String name;
    public String role;
}
