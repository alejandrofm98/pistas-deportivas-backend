package com.sportreserve.payment;

import tools.jackson.databind.ObjectMapper;
import com.sportreserve.exception.BusinessException;
import com.sportreserve.payment.dto.PaymentInitiateResponse;
import com.sportreserve.reservation.Reservation;
import com.sportreserve.reservation.ReservationRepository;
import com.sportreserve.reservation.ReservationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RedsysService {

    private final String merchantCode;
    private final String terminal;
    private final String secretKey;
    private final String url;
    private final String merchantName;
    private final String merchantReturnUrl;
    private final String notifyUrl;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
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
        ReservationService reservationService,
        ObjectMapper objectMapper) {
        this.merchantCode = merchantCode;
        this.terminal = terminal;
        this.secretKey = secretKey;
        this.url = url;
        this.merchantName = merchantName;
        this.merchantReturnUrl = merchantReturnUrl;
        this.notifyUrl = notifyUrl;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
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
        params.put("DS_MERCHANT_MERCHANTURL", notifyUrl);
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
        processPaymentResult(merchantParameters, signature);
    }

    public PaymentConfirmResult confirmPayment(String merchantParameters, String signature) {
        return processPaymentResult(merchantParameters, signature);
    }

    private PaymentConfirmResult processPaymentResult(String merchantParameters, String signature) {
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

            boolean success = "0000".equals(responseCode) || "00".equals(responseCode)
                || "0".equals(responseCode);

            if (success) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setRedsysTransactionId(transactionId);
                payment.setRedsysResponseCode(responseCode);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                Reservation paymentReservation = payment.getReservation();
                paymentReservation.setPaymentStatus(PaymentStatus.PAID);
                reservationRepository.save(paymentReservation);

                UUID bookingGroup = paymentReservation.getBookingGroup();
                if (bookingGroup != null) {
                    reservationService.confirmByBookingGroup(bookingGroup, transactionId);
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setRedsysResponseCode(responseCode);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }

            return new PaymentConfirmResult(success, order, transactionId);
        } catch (Exception e) {
            throw new BusinessException("Failed to process payment result: " + e.getMessage());
        }
    }

    public boolean verifySignature(String merchantParameters, String signature) {
        try {
            String decodedJson = new String(Base64.getUrlDecoder().decode(merchantParameters),
                StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, String> params = objectMapper.readValue(decodedJson, Map.class);
            String order = params.get("Ds_Order");

            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(keySpec);

            byte[] orderBytes = order.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes2 = mac.doFinal(orderBytes);

            Mac mac2 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec2 = new SecretKeySpec(keyBytes2, "HmacSHA256");
            mac2.init(keySpec2);

            byte[] signatureBytes = mac2.doFinal(merchantParameters.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signatureBytes);

            return expectedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public String getTerminal() {
        return terminal;
    }

    public record PaymentConfirmResult(boolean success, String order, String transactionId) {}
}
