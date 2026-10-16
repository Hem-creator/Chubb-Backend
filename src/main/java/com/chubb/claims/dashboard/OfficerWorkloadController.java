package com.chubb.claims.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/officers")
@RequiredArgsConstructor
public class OfficerWorkloadController {

    private final DashboardService dashboardService;

    @Operation(summary = "Personal workload of one officer (matched on the exact assigned-officer name)")
    @GetMapping("/{officer}/workload")
    public OfficerWorkloadResponse workload(@PathVariable String officer) {
        return dashboardService.getOfficerWorkload(officer);
    }
}
