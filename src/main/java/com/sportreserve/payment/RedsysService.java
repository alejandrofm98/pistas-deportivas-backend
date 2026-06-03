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
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RedsysService {

    private final String merchantCode;
    private final String terminal;
    private final String secretKey;
    private final String currency;
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
        @Value("${app.redsys.currency}") String currency,
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
        this.currency = currency;
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
        long amountCents = payment.getAmount()
            .multiply(java.math.BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("DS_MERCHANT_AMOUNT", String.valueOf(amountCents));
        params.put("DS_MERCHANT_ORDER", payment.getRedsysOrder());
        params.put("DS_MERCHANT_MERCHANTCODE", merchantCode);
        params.put("DS_MERCHANT_CURRENCY", currency);
        params.put("DS_MERCHANT_TRANSACTIONTYPE", "0");
        params.put("DS_MERCHANT_TERMINAL", terminal);
        params.put("DS_MERCHANT_MERCHANTURL", notifyUrl);
        params.put("DS_MERCHANT_URLOK", merchantReturnUrl + "?success=true");
        params.put("DS_MERCHANT_URLKO", merchantReturnUrl + "?success=false");
        params.put("DS_MERCHANT_MERCHANTNAME", merchantName);
        params.put("DS_MERCHANT_TITULAR", reservation.getCustomerName());
        params.put("DS_MERCHANT_PRODUCTDESCRIPTION", "Reserva: " + reservation.getCourt().getName());
        if (reservation.getPaymentMethod() == PaymentMethod.BIZUM) {
            params.put("DS_MERCHANT_PAYMETHODS", "z");
            String bizumPhone = normalizeBizumPhone(reservation.getCustomerPhone());
            if (!bizumPhone.isBlank()) {
                params.put("DS_MERCHANT_BIZUM_MOBILENUMBER", bizumPhone);
            }
        }

        try {
            String jsonParams = objectMapper.writeValueAsString(params);
            String encodedParams = Base64.getEncoder()
                .encodeToString(jsonParams.getBytes(StandardCharsets.UTF_8));

            String signature = createSignature(encodedParams, payment.getRedsysOrder());

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
            Map<String, String> params = decodeMerchantParameters(merchantParameters);

            String order = getParam(params, "Ds_Order");
            String responseCode = getParam(params, "Ds_Response");
            String transactionId = getParam(params, "Ds_AuthorisationCode");

            if (!verifySignature(merchantParameters, signature)) {
                throw new BusinessException("Invalid Redsys signature for order: " + order);
            }

            Payment payment = paymentRepository.findByRedsysOrder(order)
                .orElseThrow(() -> new BusinessException("Payment not found for order: " + order));

            boolean success = isSuccessfulResponse(responseCode);

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
            Map<String, String> params = decodeMerchantParameters(merchantParameters);
            String order = getParam(params, "Ds_Order");
            String expectedSignature = createSignature(merchantParameters, order);

            return normalizeSignature(expectedSignature).equals(normalizeSignature(signature));
        } catch (Exception e) {
            return false;
        }
    }

    private String createSignature(String merchantParameters, String order) throws Exception {
        byte[] diversifiedKey = createMerchantSignatureKey(order);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(diversifiedKey, "HmacSHA256"));
        byte[] signatureBytes = mac.doFinal(merchantParameters.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    private byte[] createMerchantSignatureKey(String order) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(
            Cipher.ENCRYPT_MODE,
            new SecretKeySpec(keyBytes, "DESede"),
            new IvParameterSpec(new byte[8])
        );
        byte[] orderBytes = order.getBytes(StandardCharsets.UTF_8);
        byte[] paddedOrder = Arrays.copyOf(orderBytes, ((orderBytes.length + 7) / 8) * 8);
        return cipher.doFinal(paddedOrder);
    }

    private Map<String, String> decodeMerchantParameters(String merchantParameters) throws Exception {
        String decodedJson = new String(decodeBase64Url(merchantParameters), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> rawParams = objectMapper.readValue(decodedJson, Map.class);
        Map<String, String> params = new LinkedHashMap<>();
        rawParams.forEach((key, value) -> params.put(key, value == null ? null : String.valueOf(value)));
        return params;
    }

    private byte[] decodeBase64Url(String value) {
        String padded = value + "=".repeat((4 - value.length() % 4) % 4);
        try {
            return Base64.getDecoder().decode(padded);
        } catch (IllegalArgumentException e) {
            return Base64.getUrlDecoder().decode(padded);
        }
    }

    private String getParam(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value != null) {
            return value;
        }
        return params.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    private boolean isSuccessfulResponse(String responseCode) {
        try {
            int code = Integer.parseInt(responseCode);
            return code >= 0 && code <= 99;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String normalizeSignature(String signature) {
        return signature == null ? "" : signature.replace('+', '-').replace('/', '_').replace("=", "");
    }

    private String normalizeBizumPhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("0034") && digits.length() == 13) {
            return digits.substring(2);
        }
        if (digits.startsWith("34") && digits.length() == 11) {
            return digits;
        }
        if (digits.length() == 9) {
            return "34" + digits;
        }
        return digits;
    }

    public String getTerminal() {
        return terminal;
    }

    public record PaymentConfirmResult(boolean success, String order, String transactionId) {}
}
