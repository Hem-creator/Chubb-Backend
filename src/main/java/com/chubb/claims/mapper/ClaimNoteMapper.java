package com.chubb.claims.mapper;

import com.chubb.claims.dto.response.ClaimNoteResponse;
import com.chubb.claims.entity.ClaimNote;
import org.springframework.stereotype.Component;

@Component
public class ClaimNoteMapper {

    public ClaimNoteResponse toResponse(ClaimNote note) {
        return new ClaimNoteResponse(
                note.getId(),
                note.getClaim().getId(),
                note.getNote(),
                note.getCreatedAt());
    }
}
