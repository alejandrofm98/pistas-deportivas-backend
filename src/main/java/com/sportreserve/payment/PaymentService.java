package com.sportreserve.payment;

import com.sportreserve.exception.BusinessException;
import com.sportreserve.exception.ResourceNotFoundException;
import com.sportreserve.payment.dto.*;
import com.sportreserve.reservation.Reservation;
import com.sportreserve.reservation.ReservationRepository;
import com.sportreserve.reservation.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final RedsysService redsysService;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationRepository reservationRepository,
                          RedsysService redsysService) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.redsysService = redsysService;
    }

    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        Reservation reservation = reservationRepository.findById(request.reservationId())
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("Cannot pay for a cancelled reservation");
        }

        var existingPayment = paymentRepository.findByReservationId(reservation.getId());
        if (existingPayment.isPresent()
            && existingPayment.get().getStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Reservation is already paid");
        }

        Payment payment = existingPayment.orElseGet(() -> {
            Payment newPayment = new Payment();
            newPayment.setReservation(reservation);
            newPayment.setAmount(reservation.getTotalPrice());
            newPayment.setRedsysOrder(generateOrderId(reservation));
            newPayment.setStatus(PaymentStatus.PENDING);
            newPayment.setCreatedAt(LocalDateTime.now());
            return newPayment;
        });

        payment.setRedsysOrder(generateOrderId(reservation));
        payment = paymentRepository.save(payment);

        return redsysService.createPaymentRequest(payment);
    }

    @Transactional
    public void handleRedsysNotification(String merchantParameters, String signature) {
        redsysService.processNotification(merchantParameters, signature);
    }

    public PaymentResponse getPaymentStatus(UUID id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment);
    }

    public PaymentResponse getPaymentByReservation(UUID reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found for reservation"));
        return toResponse(payment);
    }

    private String generateOrderId(Reservation reservation) {
        return System.currentTimeMillis() + "-" + reservation.getId().toString().substring(0, 8);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getReservation().getId(),
            payment.getRedsysOrder(),
            payment.getAmount(),
            payment.getStatus().name(),
            payment.getRedsysTransactionId(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
