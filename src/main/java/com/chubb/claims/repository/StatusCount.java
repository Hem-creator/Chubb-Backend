package com.chubb.claims.repository;

import com.chubb.claims.entity.ClaimStatus;

/** Projection for "claims grouped by status" aggregation. */
public interface StatusCount {

    ClaimStatus getStatus();

    long getCount();
}
