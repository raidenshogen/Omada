package com.omada.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One board member: membership id, board, user and join timestamp. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardMembershipResponse {
    public String id;
    public String boardId;
    public String userId;
    public String joinedAt;
    public String email;
    public String name;
}
