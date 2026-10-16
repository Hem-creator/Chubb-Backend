package com.chubb.claims.repository;

import com.chubb.claims.entity.Claim;
import com.chubb.claims.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /** "Open" claims are those still being worked on. APPROVED awaits settlement and is not counted as open. */
    Set<ClaimStatus> OPEN_STATUSES =
            EnumSet.of(ClaimStatus.NEW, ClaimStatus.UNDER_REVIEW, ClaimStatus.MORE_INFO_REQUIRED);

    // ---- Derived finders ----

    Optional<Claim> findByClaimNumber(String claimNumber);

    List<Claim> findByStatus(ClaimStatus status);

    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    List<Claim> findByAssignedOfficer(String assignedOfficer);

    Page<Claim> findByAssignedOfficer(String assignedOfficer, Pageable pageable);

    List<Claim> findByClaimantEmail(String claimantEmail);

    Page<Claim> findByPolicyNumber(String policyNumber, Pageable pageable);

    boolean existsByClaimNumber(String claimNumber);

    // ---- Aggregation queries (JPQL) ----

    /** Number of claims whose status is in {@code openStatuses}. */
    @Query("select count(c) from Claim c where c.status in :openStatuses")
    long countOpenClaims(@Param("openStatuses") Set<ClaimStatus> openStatuses);

    /** Sum of liabilityExposure over open claims; COALESCE returns 0 instead of null when there are none. */
    @Query("select coalesce(sum(c.liabilityExposure), 0) from Claim c where c.status in :openStatuses")
    BigDecimal sumOpenLiabilityExposure(@Param("openStatuses") Set<ClaimStatus> openStatuses);

    /** Claim count per status; statuses with no claims are absent from the result. */
    @Query("select c.status as status, count(c) as count from Claim c group by c.status")
    List<StatusCount> countClaimsByStatus();

    /** Open claim count per assigned officer; unassigned claims are excluded. */
    @Query("""
            select c.assignedOfficer as officer, count(c) as count
            from Claim c
            where c.assignedOfficer is not null and c.status in :openStatuses
            group by c.assignedOfficer
            order by count(c) desc
            """)
    List<OfficerCount> countOpenClaimsByOfficer(@Param("openStatuses") Set<ClaimStatus> openStatuses);

    // ---- Per-officer aggregations ----

    @Query("select count(c) from Claim c where c.assignedOfficer = :officer and c.status in :openStatuses")
    long countOpenClaimsByOfficer(@Param("officer") String officer, @Param("openStatuses") Set<ClaimStatus> openStatuses);

    @Query("""
            select coalesce(sum(c.liabilityExposure), 0) from Claim c
            where c.assignedOfficer = :officer and c.status in :openStatuses
            """)
    BigDecimal sumOpenLiabilityExposureByOfficer(@Param("officer") String officer,
                                                 @Param("openStatuses") Set<ClaimStatus> openStatuses);

    @Query("select c.status as status, count(c) as count from Claim c where c.assignedOfficer = :officer group by c.status")
    List<StatusCount> countClaimsByStatusForOfficer(@Param("officer") String officer);
    // ---- Convenience wrappers using the standard "open" definition ----

    default long countOpenClaims() {
        return countOpenClaims(OPEN_STATUSES);
    }

    default BigDecimal sumOpenLiabilityExposure() {
        return sumOpenLiabilityExposure(OPEN_STATUSES);
    }

    default long countOpenClaimsByOfficer(String officer) {
        return countOpenClaimsByOfficer(officer, OPEN_STATUSES);
    }

    default BigDecimal sumOpenLiabilityExposureByOfficer(String officer) {
        return sumOpenLiabilityExposureByOfficer(officer, OPEN_STATUSES);
    }
    default List<OfficerCount> countOpenClaimsByOfficer() {
        return countOpenClaimsByOfficer(OPEN_STATUSES);
    }
}
