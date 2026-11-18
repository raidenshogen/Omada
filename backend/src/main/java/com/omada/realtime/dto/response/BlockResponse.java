package com.omada.realtime.dto.response;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor public class BlockResponse {
    public String id;
    public String documentId;
    public String type;
    public String text;
    public boolean checked;
    public int position;
    public int version;
    public String createdBy;
    public String updatedBy;
    public String createdByName;
    public String updatedByName;
}
