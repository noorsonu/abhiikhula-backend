package in.abhiikhula.zone.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.zone.dto.CreateZoneRequest;
import in.abhiikhula.zone.dto.ZoneResponseDto;
import in.abhiikhula.zone.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@Tag(name = "M1 Discovery - Zones", description = "Hyperlocal Zones API (BRD FR-101, Section 30)")
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    @Operation(summary = "Get all active zones", description = "Returns active hyperlocal zones (e.g. 2 km radius clusters) for discovery and area picker")
    public ResponseEntity<ApiResponse<List<ZoneResponseDto>>> getActiveZones() {
        List<ZoneResponseDto> zones = zoneService.getAllActiveZones();
        return ResponseEntity.ok(ApiResponse.ok(zones, "Active zones fetched successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get zone by ID", description = "Returns details of a specific zone")
    public ResponseEntity<ApiResponse<ZoneResponseDto>> getZoneById(@PathVariable UUID id) {
        ZoneResponseDto zone = zoneService.getZoneById(id);
        return ResponseEntity.ok(ApiResponse.ok(zone));
    }

    @PostMapping
    @Operation(summary = "Create a new zone", description = "Admin endpoint to register a new hyperlocal cluster (BRD AD-04)")
    public ResponseEntity<ApiResponse<ZoneResponseDto>> createZone(@Valid @RequestBody CreateZoneRequest request) {
        ZoneResponseDto createdZone = zoneService.createZone(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(createdZone, "Zone registered successfully"));
    }
}

