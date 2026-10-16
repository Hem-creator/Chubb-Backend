package com.chubb.claims.service.impl;

import com.chubb.claims.dto.request.AddNoteRequest;
import com.chubb.claims.dto.response.ClaimNoteResponse;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimNote;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.mapper.ClaimNoteMapper;
import com.chubb.claims.repository.ClaimNoteRepository;
import com.chubb.claims.repository.ClaimRepository;
import com.chubb.claims.service.ClaimNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimNoteServiceImpl implements ClaimNoteService {

    private final ClaimRepository claimRepository;
    private final ClaimNoteRepository claimNoteRepository;
    private final ClaimNoteMapper claimNoteMapper;

    @Override
    @Transactional
    public ClaimNoteResponse addNote(Long claimId, AddNoteRequest request) {
        Claim claim = findClaim(claimId);
        ClaimNote saved = claimNoteRepository.save(
                ClaimNote.builder().claim(claim).note(request.note().trim()).build());
        log.info("Added note {} to claim {}", saved.getId(), claimId);
        return claimNoteMapper.toResponse(saved);
    }

    @Override
    public List<ClaimNoteResponse> getNotes(Long claimId) {
        findClaim(claimId);
        return claimNoteRepository.findByClaimIdOrderByCreatedAtAscIdAsc(claimId).stream()
                .map(claimNoteMapper::toResponse)
                .toList();
    }

    private Claim findClaim(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));
    }
}
