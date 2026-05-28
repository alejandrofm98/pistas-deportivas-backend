package com.sportreserve.reservation;

import com.sportreserve.court.Court;
import com.sportreserve.court.CourtService;
import com.sportreserve.exception.BusinessException;
import com.sportreserve.exception.ResourceNotFoundException;
import com.sportreserve.notification.EmailService;
import com.sportreserve.payment.PaymentStatus;
import com.sportreserve.reservation.dto.ReservationMapper;
import com.sportreserve.reservation.dto.ReservationRequest;
import com.sportreserve.reservation.dto.ReservationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtService courtService;
    private final ReservationMapper reservationMapper;
    private final EmailService emailService;

    public ReservationService(ReservationRepository reservationRepository,
                              CourtService courtService,
                              ReservationMapper reservationMapper,
                              EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.courtService = courtService;
        this.reservationMapper = reservationMapper;
        this.emailService = emailService;
    }

    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream()
            .map(reservationMapper::toResponse)
            .collect(Collectors.toList());
    }

    public ReservationResponse findById(UUID id) {
        return reservationRepository.findById(id)
            .map(reservationMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    public Reservation getReservationEntity(UUID id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        Court court = courtService.getCourtEntity(request.courtId());

        if (!court.getIsActive()) {
            throw new BusinessException("Court is not active");
        }

        validateTimeRange(request.startTime(), request.endTime());
        checkOverlapping(request.courtId(), request.date(), request.startTime(), request.endTime());

        int duration = request.endTime() - request.startTime();
        BigDecimal totalPrice = court.getPricePerHour().multiply(BigDecimal.valueOf(duration));

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomerName(request.customerName());
        reservation.setCustomerEmail(request.customerEmail());
        reservation.setCustomerPhone(request.customerPhone());
        reservation.setDate(request.date());
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setTotalPrice(totalPrice);
        reservation.setPaymentMethod(request.paymentMethod());
        reservation.setPaymentStatus(PaymentStatus.PENDING);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.now());

        reservation = reservationRepository.save(reservation);

        emailService.sendReservationConfirmation(reservation);

        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancel(UUID id) {
        Reservation reservation = getReservationEntity(id);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("Reservation is already cancelled");
        }
        if (reservation.getDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Cannot cancel a past reservation");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);

        emailService.sendCancellationNotification(reservation);
        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateStatus(UUID id, ReservationStatus status) {
        Reservation reservation = getReservationEntity(id);
        reservation.setStatus(status);
        reservation = reservationRepository.save(reservation);
        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public void updatePaymentStatus(UUID id, com.sportreserve.payment.PaymentStatus paymentStatus) {
        Reservation reservation = getReservationEntity(id);
        reservation.setPaymentStatus(paymentStatus);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void completePastReservations() {
        List<Reservation> pastReservations = reservationRepository
            .findConfirmedBeforeDate(LocalDate.now());
        for (Reservation r : pastReservations) {
            r.setStatus(ReservationStatus.COMPLETED);
        }
        reservationRepository.saveAll(pastReservations);
    }

    private void validateTimeRange(int startTime, int endTime) {
        if (startTime < 8 || endTime > 23) {
            throw new BusinessException("Reservations allowed between 08:00 and 23:00");
        }
        if (startTime >= endTime) {
            throw new BusinessException("Start time must be before end time");
        }
    }

    private void checkOverlapping(UUID courtId, LocalDate date, int startTime, int endTime) {
        var existing = reservationRepository.findActiveByCourtAndDate(courtId, date);
        boolean overlaps = existing.stream()
            .anyMatch(r -> startTime < r.getEndTime() && endTime > r.getStartTime());
        if (overlaps) {
            throw new BusinessException("Time slot is already reserved");
        }
    }
}
