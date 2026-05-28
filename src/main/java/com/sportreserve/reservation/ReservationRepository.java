package com.sportreserve.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("SELECT r FROM Reservation r WHERE r.court.id = :courtId AND r.date = :date AND r.status <> 'CANCELLED'")
    List<Reservation> findActiveByCourtAndDate(@Param("courtId") UUID courtId, @Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.date < :today AND r.status = 'CONFIRMED'")
    List<Reservation> findConfirmedBeforeDate(@Param("today") LocalDate today);
}
