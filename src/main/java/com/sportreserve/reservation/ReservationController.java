package com.sportreserve.reservation;

import com.sportreserve.reservation.dto.ReservationRequest;
import com.sportreserve.reservation.dto.ReservationResponse;
import com.sportreserve.payment.PaymentStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(reservationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll() {
        return ResponseEntity.ok(reservationService.findAll());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(reservationService.findByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam ReservationStatus status) {
        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable UUID id,
            @RequestParam PaymentStatus paymentStatus) {
        reservationService.updatePaymentStatus(id, paymentStatus);
        return ResponseEntity.ok().build();
    }
}
