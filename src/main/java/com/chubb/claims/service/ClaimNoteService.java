package com.chubb.claims.service;

import com.chubb.claims.dto.request.AddNoteRequest;
import com.chubb.claims.dto.response.ClaimNoteResponse;

import java.util.List;

public interface ClaimNoteService {

    ClaimNoteResponse addNote(Long claimId, AddNoteRequest request);

    /** Notes for the claim, oldest first. */
    List<ClaimNoteResponse> getNotes(Long claimId);
}
