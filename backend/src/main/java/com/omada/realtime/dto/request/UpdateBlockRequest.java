package com.omada.realtime.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateBlockRequest {
    public String type;
    public String text;
    public Boolean checked;
    public Integer position;
    /** Expected current version — enables optimistic concurrency ("simple OT"). */
    public Integer version;
}
