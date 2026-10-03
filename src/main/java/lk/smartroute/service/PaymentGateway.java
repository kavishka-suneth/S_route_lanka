package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.PaymentRequest;
import lk.smartroute.model.Booking;
import lk.smartroute.model.Payment;
import lk.smartroute.model.PaymentStatus;

public interface PaymentGateway {
    GatewayResult process(Booking booking, PaymentRequest request);

    GatewayResult refund(Payment payment);

    record GatewayResult(PaymentStatus status, String gatewayReference, String failureReason) {
    }
}
