package com.sportreserve.court;

import com.sportreserve.court.dto.*;
import com.sportreserve.exception.BusinessException;
import com.sportreserve.exception.ResourceNotFoundException;
import com.sportreserve.reservation.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class CourtService {

    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 60, 90, 120);

    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final CourtMapper courtMapper;

    public CourtService(CourtRepository courtRepository,
                        ReservationRepository reservationRepository,
                        CourtMapper courtMapper) {
        this.courtRepository = courtRepository;
        this.reservationRepository = reservationRepository;
        this.courtMapper = courtMapper;
    }

    public List<CourtResponse> findAllActive() {
        return courtRepository.findByIsActiveTrue().stream()
            .map(courtMapper::toResponse)
            .collect(Collectors.toList());
    }

    public List<CourtResponse> findAll() {
        return courtRepository.findAll().stream()
            .map(courtMapper::toResponse)
            .collect(Collectors.toList());
    }

    public CourtResponse findById(UUID id) {
        return courtRepository.findById(id)
            .map(courtMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Court not found: " + id));
    }

    public Court getCourtEntity(UUID id) {
        return courtRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Court not found: " + id));
    }

    public AvailabilityResponse getAvailability(UUID courtId, LocalDate date) {
        Court court = getCourtEntity(courtId);
        var reservations = reservationRepository.findActiveByCourtAndDate(courtId, date);

        var slots = IntStream.rangeClosed(14, 48) // 7.0 to 24.0 in half-hour increments
            .mapToDouble(i -> i / 2.0)
            .filter(time -> time <= 24.0)
            .mapToObj(time -> {
                boolean available = reservations.stream()
                    .noneMatch(r -> time >= r.getStartTime() && time < r.getEndTime());
                return new AvailabilityResponse.TimeSlot(time, available);
            })
            .collect(Collectors.toList());

        return new AvailabilityResponse(date, slots);
    }

    @Transactional
    public CourtResponse create(CourtRequest request) {
        validateDuration(request.durationMinutes());
        Court court = courtMapper.toEntity(request);
        court = courtRepository.save(court);
        return courtMapper.toResponse(court);
    }

    @Transactional
    public CourtResponse update(UUID id, CourtRequest request) {
        validateDuration(request.durationMinutes());
        Court court = getCourtEntity(id);
        courtMapper.updateEntity(court, request);
        court = courtRepository.save(court);
        return courtMapper.toResponse(court);
    }

    private void validateDuration(Integer durationMinutes) {
        if (!ALLOWED_DURATIONS.contains(durationMinutes)) {
            throw new BusinessException(
                "Duration must be one of: 30, 60, 90, or 120 minutes. Got: " + durationMinutes);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Court court = getCourtEntity(id);
        court.setIsActive(false);
        courtRepository.save(court);
    }

    @Transactional
    public void toggleActive(UUID id) {
        Court court = getCourtEntity(id);
        court.setIsActive(!court.getIsActive());
        courtRepository.save(court);
    }
}
