package com.sportreserve.notification;

import com.sportreserve.reservation.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import brevo.ApiClient;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.CreateSmtpEmail;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter SPANISH_DATE =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"));

    private final String apiKey;
    private final String senderName;
    private final String senderEmail;

    public EmailService(@Value("${app.brevo.api-key:}") String apiKey,
                        @Value("${app.brevo.sender-name:Pistas El Valle}") String senderName,
                        @Value("${app.brevo.sender-email:}") String senderEmail) {
        this.apiKey = apiKey;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
    }

    public void sendReservationConfirmation(Reservation reservation) {
        String subject = "Reserva confirmada - Pistas El Valle Perdido";
        String html = buildConfirmationHtml(reservation);
        sendEmail(reservation.getCustomerEmail(), subject, html);
        log.info("Confirmation email sent to {}", reservation.getCustomerEmail());
    }

    public void sendCancellationNotification(Reservation reservation) {
        String subject = "Reserva cancelada - Pistas El Valle Perdido";
        String html = buildCancellationHtml(reservation);
        sendEmail(reservation.getCustomerEmail(), subject, html);
        log.info("Cancellation email sent to {}", reservation.getCustomerEmail());
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        if (apiKey.isBlank() || senderEmail.isBlank() || "tu_api_key_de_brevo".equals(apiKey)) {
            log.warn("Brevo API key not configured. Skipping email to {}: {}", to, subject);
            return;
        }

        try {
            ApiClient client = Configuration.getDefaultApiClient();
            ApiKeyAuth auth = (ApiKeyAuth) client.getAuthentication("api-key");
            auth.setApiKey(apiKey);

            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(new SendSmtpEmailSender().name(senderName).email(senderEmail));
            email.setTo(List.of(new SendSmtpEmailTo().email(to)));
            email.setSubject(subject);
            email.setHtmlContent(htmlContent);

            TransactionalEmailsApi api = new TransactionalEmailsApi(client);
            CreateSmtpEmail response = api.sendTransacEmail(email);
            log.info("Brevo email sent: messageId={}", response.getMessageId());
        } catch (Exception e) {
            log.warn("Failed to send email via Brevo to {}: {}", to, e.getMessage());
        }
    }

    private String buildConfirmationHtml(Reservation r) {
        String customer = escapeHtml(r.getCustomerName());
        String courtName = escapeHtml(r.getCourt().getName());
        String method = formatPaymentMethod(r.getPaymentMethod());
        String date = escapeHtml(formatDate(r));
        String total = escapeHtml(formatMoney(r));
        String timeRange = formatTimeRange(r.getStartTime(), r.getEndTime());

        String template = loadTemplate("templates/email/confirmation.html");
        return String.format(template,
            customer, courtName, method, date, total, timeRange);
    }

    private String buildCancellationHtml(Reservation r) {
        String courtName = escapeHtml(r.getCourt().getName());
        String method = formatPaymentMethod(r.getPaymentMethod());
        String date = escapeHtml(formatDate(r));
        String total = escapeHtml(formatMoney(r));
        String timeRange = formatTimeRange(r.getStartTime(), r.getEndTime());

        String template = loadTemplate("templates/email/cancellation.html");
        return String.format(template,
            courtName, method, date, total, timeRange);
    }

    private String formatPaymentMethod(com.sportreserve.payment.PaymentMethod method) {
        return switch (method) {
            case ONLINE -> "Online (tarjeta)";
            case BIZUM -> "Bizum";
            case ONSITE -> "Pago en local";
        };
    }

    private String formatDate(Reservation reservation) {
        return reservation.getDate().format(SPANISH_DATE);
    }

    private String formatTimeRange(double startTime, double endTime) {
        return String.format("%s - %s", formatTime(startTime), formatTime(endTime));
    }

    private String formatTime(double time) {
        int hours = (int) time;
        int minutes = (int) Math.round((time - hours) * 60);
        return String.format("%02d:%02d", hours, minutes);
    }

    private String formatMoney(Reservation reservation) {
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f", reservation.getTotalPrice());
    }

    private String formatPaymentMethod(Reservation reservation) {
        return switch (reservation.getPaymentMethod()) {
            case ONLINE -> "Online (tarjeta)";
            case BIZUM -> "Bizum";
            case ONSITE -> "Pago en local";
        };
    }

    private String loadTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load email template: " + path, e);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
