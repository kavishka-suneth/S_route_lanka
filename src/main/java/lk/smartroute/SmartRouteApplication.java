package lk.smartroute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for SmartRoute Lanka.
 *
 * SmartRoute Lanka - Web-Based Bus Scheduling and Booking System.
 */
@SpringBootApplication
@EnableScheduling
public class SmartRouteApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartRouteApplication.class, args);
    }
}
