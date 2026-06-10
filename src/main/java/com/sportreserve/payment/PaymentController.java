package com.sportreserve.payment;

import com.sportreserve.payment.dto.PaymentConfirmRequest;
import com.sportreserve.payment.dto.PaymentConfirmResponse;
import com.sportreserve.payment.dto.PaymentInitiateRequest;
import com.sportreserve.payment.dto.PaymentInitiateResponse;
import com.sportreserve.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiate(
            @Valid @RequestBody PaymentInitiateRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notify(
            @RequestParam("Ds_MerchantParameters") String merchantParameters,
            @RequestParam("Ds_Signature") String signature) {
        paymentService.handleRedsysNotification(merchantParameters, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify-json")
    public ResponseEntity<Void> notifyJson(@RequestBody Map<String, String> body) {
        String merchantParameters = body.get("Ds_MerchantParameters");
        String signature = body.get("Ds_Signature");
        paymentService.handleRedsysNotification(merchantParameters, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @Valid @RequestBody PaymentConfirmRequest request) {
        RedsysService.PaymentConfirmResult result = paymentService.confirmPayment(
            request.dsMerchantParameters(), request.dsSignature());
        return ResponseEntity.ok(new PaymentConfirmResponse(
            result.success(), result.order(), result.transactionId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(id));
    }

    @GetMapping("/by-reservation/{reservationId}")
    public ResponseEntity<PaymentResponse> getByReservation(
            @PathVariable UUID reservationId) {
        return ResponseEntity.ok(paymentService.getPaymentByReservation(reservationId));
    }
}
