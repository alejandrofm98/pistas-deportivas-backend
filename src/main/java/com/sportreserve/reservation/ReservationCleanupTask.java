package com.sportreserve.reservation;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupTask.class);
    private static final int EXPIRATION_MINUTES = 5;

    private final ReservationRepository reservationRepository;

    public ReservationCleanupTask(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Scheduled(fixedRate = 120000)
    @Transactional
    public void cancelStaleReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);
        var stale = reservationRepository.findStalePendingReservations(cutoff);
        if (stale.isEmpty()) return;

        for (Reservation r : stale) {
            r.setStatus(ReservationStatus.CANCELLED);
        }
        reservationRepository.saveAll(stale);
        log.info("Auto-cancelled {} stale PENDING_PAYMENT reservations", stale.size());
    }
}
