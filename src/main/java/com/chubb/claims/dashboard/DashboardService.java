package com.chubb.claims.dashboard;

public interface DashboardService {

    /** Manager view: open claims, exposure, status distribution and per-officer workload. */
    DashboardResponse getWorkload();

    /** Officer view: that officer's open claims, exposure and claims by status (zeros if none assigned). */
    OfficerWorkloadResponse getOfficerWorkload(String officer);
}
