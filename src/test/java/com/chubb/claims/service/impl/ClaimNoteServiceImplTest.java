package com.chubb.claims.service.impl;

import com.chubb.claims.dto.request.AddNoteRequest;
import com.chubb.claims.dto.response.ClaimNoteResponse;
import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimNote;
import com.chubb.claims.exception.ResourceNotFoundException;
import com.chubb.claims.mapper.ClaimNoteMapper;
import com.chubb.claims.repository.ClaimNoteRepository;
import com.chubb.claims.repository.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimNoteServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimNoteRepository claimNoteRepository;
    @Spy
    private ClaimNoteMapper claimNoteMapper = new ClaimNoteMapper();
    @InjectMocks
    private ClaimNoteServiceImpl service;

    private final Claim claim = Claim.builder().id(1L).build();

    @Test
    void addNote_savesTrimmedNoteLinkedToClaim() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimNoteRepository.save(any(ClaimNote.class))).thenAnswer(inv -> {
            ClaimNote n = inv.getArgument(0);
            n.setId(10L);
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });

        ClaimNoteResponse response = service.addNote(1L, new AddNoteRequest("  Requested repair invoice. "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.claimId()).isEqualTo(1L);
        assertThat(response.note()).isEqualTo("Requested repair invoice.");
    }

    @Test
    void addNote_throwsNotFoundForMissingClaim() {
        when(claimRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addNote(2L, new AddNoteRequest("x")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(claimNoteRepository, never()).save(any());
    }

    @Test
    void getNotes_returnsRepositoryOrder() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        LocalDateTime t = LocalDateTime.now();
        when(claimNoteRepository.findByClaimIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of(
                ClaimNote.builder().id(1L).claim(claim).note("first").createdAt(t).build(),
                ClaimNote.builder().id(2L).claim(claim).note("second").createdAt(t.plusSeconds(1)).build()));

        assertThat(service.getNotes(1L)).extracting(ClaimNoteResponse::note).containsExactly("first", "second");
    }

    @Test
    void getNotes_throwsNotFoundForMissingClaim() {
        when(claimRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNotes(3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
