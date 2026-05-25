package com.sportreserve.notification;

import com.sportreserve.reservation.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:no-reply@sportreserve.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendReservationConfirmation(Reservation reservation) {
        String subject = "Reserva confirmada - Pistas Deportivas";
        String body = buildConfirmationBody(reservation);
        sendEmail(reservation.getCustomerEmail(), subject, body);
        log.info("Confirmation email sent to {}", reservation.getCustomerEmail());
    }

    public void sendCancellationNotification(Reservation reservation) {
        String subject = "Reserva cancelada - Pistas Deportivas";
        String body = buildCancellationBody(reservation);
        sendEmail(reservation.getCustomerEmail(), subject, body);
        log.info("Cancellation email sent to {}", reservation.getCustomerEmail());
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildConfirmationBody(Reservation r) {
        return String.format("""
            Tu reserva ha sido confirmada.

            Pista: %s
            Fecha: %s
            Horario: %02d:00 - %02d:00
            Total: %.2f EUR
            Método de pago: %s
            Cliente: %s
            Email: %s

            ¡Gracias por tu reserva!
            """,
            r.getCourt().getName(),
            r.getDate().toString(),
            r.getStartTime(),
            r.getEndTime(),
            r.getTotalPrice(),
            r.getPaymentMethod().name(),
            r.getCustomerName(),
            r.getCustomerEmail()
        );
    }

    private String buildCancellationBody(Reservation r) {
        return String.format("""
            Tu reserva ha sido cancelada.

            Pista: %s
            Fecha: %s
            Horario: %02d:00 - %02d:00

            Si tienes alguna duda, contacta con nosotros.
            """,
            r.getCourt().getName(),
            r.getDate().toString(),
            r.getStartTime(),
            r.getEndTime()
        );
    }
}
