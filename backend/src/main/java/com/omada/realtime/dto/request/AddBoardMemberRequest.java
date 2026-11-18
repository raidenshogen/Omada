package com.omada.realtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Add a user to a board (user identified by userId). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddBoardMemberRequest {
    @NotBlank
    public String userId;
}
