package com.sportreserve.payment;

import tools.jackson.databind.ObjectMapper;
import com.sportreserve.exception.BusinessException;
import com.sportreserve.notification.EmailService;
import com.sportreserve.payment.dto.PaymentInitiateResponse;
import com.sportreserve.reservation.Reservation;
import com.sportreserve.reservation.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RedsysService {

    private final String merchantCode;
    private final String terminal;
    private final String secretKey;
    private final String url;
    private final String merchantName;
    private final String merchantReturnUrl;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    private static final String DS_SIGNATURE_VERSION = "HMAC_SHA256_V1";

    public RedsysService(
        @Value("${app.redsys.merchant-code}") String merchantCode,
        @Value("${app.redsys.terminal}") String terminal,
        @Value("${app.redsys.secret-key}") String secretKey,
        @Value("${app.redsys.url}") String url,
        @Value("${app.redsys.merchant-name}") String merchantName,
        @Value("${app.redsys.merchant-return-url}") String merchantReturnUrl,
        @Value("${app.redsys.notify-url}") String notifyUrl,
        PaymentRepository paymentRepository,
        ReservationRepository reservationRepository,
        EmailService emailService,
        ObjectMapper objectMapper) {
        this.merchantCode = merchantCode;
        this.terminal = terminal;
        this.secretKey = secretKey;
        this.url = url;
        this.merchantName = merchantName;
        this.merchantReturnUrl = merchantReturnUrl;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    public PaymentInitiateResponse createPaymentRequest(Payment payment) {
        Reservation reservation = payment.getReservation();
        long amountCents = payment.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("DS_MERCHANT_AMOUNT", String.valueOf(amountCents));
        params.put("DS_MERCHANT_ORDER", payment.getRedsysOrder());
        params.put("DS_MERCHANT_MERCHANTCODE", merchantCode);
        params.put("DS_MERCHANT_CURRENCY", "978");
        params.put("DS_MERCHANT_TRANSACTIONTYPE", "0");
        params.put("DS_MERCHANT_TERMINAL", terminal);
        params.put("DS_MERCHANT_MERCHANTURL", "");
        params.put("DS_MERCHANT_URLOK", merchantReturnUrl + "?success=true");
        params.put("DS_MERCHANT_URLKO", merchantReturnUrl + "?success=false");
        params.put("DS_MERCHANT_MERCHANTNAME", merchantName);
        params.put("DS_MERCHANT_TITULAR", reservation.getCustomerName());
        params.put("DS_MERCHANT_PRODUCTDESCRIPTION", "Reserva: " + reservation.getCourt().getName());

        try {
            String jsonParams = objectMapper.writeValueAsString(params);
            String encodedParams = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(jsonParams.getBytes(StandardCharsets.UTF_8));

            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(keySpec);

            byte[] orderBytes = payment.getRedsysOrder().getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes2 = mac.doFinal(orderBytes);

            Mac mac2 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec2 = new SecretKeySpec(keyBytes2, "HmacSHA256");
            mac2.init(keySpec2);

            byte[] signatureBytes = mac2.doFinal(encodedParams.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signatureBytes);

            return new PaymentInitiateResponse(
                url,
                DS_SIGNATURE_VERSION,
                encodedParams,
                signature
            );
        } catch (Exception e) {
            throw new BusinessException("Failed to create Redsys payment request: " + e.getMessage());
        }
    }

    public void processNotification(String merchantParameters, String signature) {
        try {
            String decodedJson = new String(Base64.getUrlDecoder().decode(merchantParameters),
                StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, String> params = objectMapper.readValue(decodedJson, Map.class);

            String order = params.get("Ds_Order");
            String responseCode = params.get("Ds_Response");
            String transactionId = params.get("Ds_AuthorisationCode");

            Payment payment = paymentRepository.findByRedsysOrder(order)
                .orElseThrow(() -> new BusinessException("Payment not found for order: " + order));

            if ("0000".equals(responseCode) || "00".equals(responseCode)
                || "0".equals(responseCode)) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setRedsysTransactionId(transactionId);
                Reservation reservation = payment.getReservation();
                reservation.setPaymentStatus(PaymentStatus.PAID);
                reservation.setStatus(com.sportreserve.reservation.ReservationStatus.CONFIRMED);
                reservationRepository.save(reservation);
                emailService.sendReservationConfirmation(reservation);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }

            payment.setRedsysResponseCode(responseCode);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

        } catch (Exception e) {
            throw new BusinessException("Failed to process Redsys notification: " + e.getMessage());
        }
    }
}
