package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.PaymentRequest;
import lk.smartroute.model.Booking;
import lk.smartroute.model.Payment;
import lk.smartroute.model.PaymentStatus;
import lk.smartroute.service.PaymentGateway;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public GatewayResult process(Booking booking, PaymentRequest request) {
        boolean shouldFail = Boolean.TRUE.equals(request.simulateFailure())
            || request.transactionReference().toUpperCase().contains("FAIL");
        if (shouldFail) {
            return new GatewayResult(PaymentStatus.FAILED, "MOCK-" + UUID.randomUUID(), "Mock gateway declined the payment");
        }
        return new GatewayResult(PaymentStatus.SUCCESS, "MOCK-" + UUID.randomUUID(), null);
    }

    @Override
    public GatewayResult refund(Payment payment) {
        return new GatewayResult(PaymentStatus.REFUNDED, "MOCK-REFUND-" + UUID.randomUUID(), null);
    }
}
