package com.sportreserve.court;

import com.sportreserve.court.dto.AvailabilityResponse;
import com.sportreserve.court.dto.CourtRequest;
import com.sportreserve.court.dto.CourtResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courts")
public class CourtController {

    private final CourtService courtService;

    public CourtController(CourtService courtService) {
        this.courtService = courtService;
    }

    @GetMapping
    public ResponseEntity<List<CourtResponse>> getAll() {
        return ResponseEntity.ok(courtService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourtResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(courtService.findById(id));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(courtService.getAvailability(id, date));
    }
}
