package com.chubb.claims.repository;

/** Projection for "open claims per officer" aggregation. */
public interface OfficerCount {

    String getOfficer();

    long getCount();
}
