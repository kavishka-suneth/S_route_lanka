package lk.smartroute.service.impl;

import lk.smartroute.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MockSmsService implements SmsService {
    private static final Logger log = LoggerFactory.getLogger(MockSmsService.class);
    private final String providerName;
    private final boolean enabled;

    public MockSmsService(
        @Value("${app.sms.provider:mock}") String providerName,
        @Value("${app.sms.enabled:false}") boolean enabled
    ) {
        this.providerName = providerName == null ? "mock" : providerName;
        this.enabled = enabled;
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        if (!enabled || "mock".equalsIgnoreCase(providerName)) {
            log.info("SMS {} provider recorded message for {}", providerName, phoneNumber);
            return;
        }
        log.warn("SMS provider {} is configured but no concrete adapter is installed. Message recorded for {}", providerName, phoneNumber);
    }
}

