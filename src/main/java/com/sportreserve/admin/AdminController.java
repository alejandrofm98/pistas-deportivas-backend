package com.sportreserve.admin;

import com.sportreserve.court.CourtService;
import com.sportreserve.court.dto.CourtRequest;
import com.sportreserve.court.dto.CourtResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CourtService courtService;

    public AdminController(CourtService courtService) {
        this.courtService = courtService;
    }

    @GetMapping("/courts")
    public ResponseEntity<List<CourtResponse>> getAllCourts() {
        return ResponseEntity.ok(courtService.findAll());
    }

    @PostMapping("/courts")
    public ResponseEntity<CourtResponse> createCourt(@Valid @RequestBody CourtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(courtService.create(request));
    }

    @PutMapping("/courts/{id}")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable UUID id,
            @Valid @RequestBody CourtRequest request) {
        return ResponseEntity.ok(courtService.update(id, request));
    }

    @DeleteMapping("/courts/{id}")
    public ResponseEntity<Void> deleteCourt(@PathVariable UUID id) {
        courtService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/courts/{id}/toggle")
    public ResponseEntity<Void> toggleCourt(@PathVariable UUID id) {
        courtService.toggleActive(id);
        return ResponseEntity.ok().build();
    }
}
