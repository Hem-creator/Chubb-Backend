package com.chubb.claims.repository;

import com.chubb.claims.entity.ClaimNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimNoteRepository extends JpaRepository<ClaimNote, Long> {

    /** Chronological order; id breaks ties between notes created within the same instant. */
    List<ClaimNote> findByClaimIdOrderByCreatedAtAscIdAsc(Long claimId);

    Page<ClaimNote> findByClaimId(Long claimId, Pageable pageable);

    long countByClaimId(Long claimId);

    void deleteByClaimId(Long claimId);
}
