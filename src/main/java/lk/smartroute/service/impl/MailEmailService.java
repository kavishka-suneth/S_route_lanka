package lk.smartroute.service.impl;

import jakarta.mail.internet.MimeMessage;
import lk.smartroute.model.Booking;
import lk.smartroute.model.BookingPassenger;
import lk.smartroute.model.BookingSeat;
import lk.smartroute.model.OtpPurpose;
import lk.smartroute.service.EmailService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Primary
@Service
public class MailEmailService implements EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(MailEmailService.class);

    private final JavaMailSender mailSender;
    private final String username;
    private final String password;
    private final String from;
    private final boolean developmentFallback;

    public MailEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.development-fallback:true}") boolean developmentFallback
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password.trim();
        this.from = (from == null || from.isBlank())
                ? this.username
                : from.trim();
        this.developmentFallback = developmentFallback;
    }

    @Override
    public void sendNotificationEmail(String email, String subject, String htmlBody) {
        if (username.isBlank() || password.isBlank() || from.isBlank()) {
            if (developmentFallback) {
                log.info("SMTP not configured. Notification email attempt recorded for {} subject {}.", email, subject);
                return;
            }
            throw new IllegalStateException("Email service is not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            if (developmentFallback) {
                log.warn("Failed to send notification email to {} subject {}.", email, subject);
                return;
            }
            throw new IllegalStateException("Unable to send notification email", ex);
        }
    }

    @Override
    public void sendTicket(String email, Booking booking, String qrCodeDataUri) {
        if (username.isBlank() || password.isBlank() || from.isBlank()) {
            if (developmentFallback) {
                log.info("SMTP not configured. Ticket email attempt recorded for booking {} to {}.", booking.getBookingReference(), email);
                return;
            }
            throw new IllegalStateException("Email service is not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setFrom(from);
            helper.setSubject("Your SmartRoute Lanka ticket - " + booking.getBookingReference());
            helper.setText(buildTicketEmailHtml(booking), true);
            byte[] pdf = buildTicketPdf(booking, qrCodeDataUri);
            helper.addAttachment(
                    "SmartRoute-ticket-" + booking.getBookingReference() + ".pdf",
                    new ByteArrayResource(pdf),
                    "application/pdf"
            );
            mailSender.send(message);
        } catch (Exception ex) {
            if (developmentFallback) {
                log.warn("Failed to send ticket email for booking {} to {}.", booking.getBookingReference(), email);
                return;
            }
            throw new IllegalStateException("Unable to send ticket email", ex);
        }
    }

    @Override
    public void sendOtp(
            String email,
            String otp,
            OtpPurpose purpose,
            int expiryMinutes
    ) {

        if (username.isBlank() || password.isBlank() || from.isBlank()) {

            if (developmentFallback) {
                log.info(
                        "SMTP not configured. Email attempt recorded for {} purpose {}. OTP intentionally not logged.",
                        email,
                        purpose
                );
                return;
            }

            throw new IllegalStateException(
                    "Email service is not configured"
            );
        }

        String subject = "SmartRoute Lanka verification code";

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setFrom(from);
            helper.setSubject(subject);

            String html = buildHtml(
                    purpose,
                    otp,
                    expiryMinutes
            );

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception ex) {

            if (developmentFallback) {
                log.warn(
                        "Failed to send OTP email to {} for purpose {}. OTP intentionally not logged.",
                        email,
                        purpose
                );
                return;
            }

            throw new IllegalStateException(
                    "Unable to send verification email",
                    ex
            );
        }
    }

    private String buildHtml(
            OtpPurpose purpose,
            String otp,
            int expiryMinutes
    ) {

        Instant expiresAt = Instant.now()
                .plusSeconds(expiryMinutes * 60L);

        String expiry = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm z")
                .withZone(ZoneId.of("Asia/Colombo"))
                .format(expiresAt);

        String title = switch (purpose) {
            case REGISTRATION ->
                    "email registration verification";

            case CARD_VERIFICATION ->
                    "saved card verification";

            case WALLET_TOP_UP ->
                    "wallet top-up confirmation";

            case PASSWORD_RESET ->
                    "password reset";
        };

        return """
                <div style="font-family:Arial,sans-serif;
                            max-width:560px;
                            margin:auto;
                            border:1px solid #dbe3eb;
                            border-radius:12px;
                            overflow:hidden">

                    <div style="background:#155bd7;
                                color:white;
                                padding:22px 26px">

                        <h1 style="margin:0;font-size:22px">
                            SmartRoute Lanka
                        </h1>

                        <p style="margin:6px 0 0;color:#dce9ff">
                            Verification code
                        </p>
                    </div>

                    <div style="padding:26px;color:#102c47">

                        <p>
                            Use this verification code to complete your
                            <strong>%s</strong>:
                        </p>

                        <p style="
                            font-size:32px;
                            letter-spacing:8px;
                            font-weight:700;
                            margin:24px 0">
                            %s
                        </p>

                        <p>
                            This code expires at
                            <strong>%s</strong>
                            and can only be used once.
                        </p>

                        <p style="
                            color:#637b92;
                            font-size:13px">
                            If you did not request this,
                            you can safely ignore this email.
                        </p>

                    </div>
                </div>
                """.formatted(title, otp, expiry);
    }

    private String buildTicketEmailHtml(Booking booking) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto;border:1px solid #dbe3eb;border-radius:12px;overflow:hidden">
                  <div style="background:#155bd7;color:white;padding:22px 26px">
                    <h1 style="margin:0;font-size:22px">SmartRoute Lanka</h1>
                    <p style="margin:6px 0 0;color:#dce9ff">Your ticket is attached</p>
                  </div>
                  <div style="padding:26px;color:#102c47">
                    <p>Your booking <strong>%s</strong> is confirmed.</p>
                    <p>The PDF ticket is attached to this email. Please keep it for boarding.</p>
                    <p style="color:#637b92;font-size:13px">You can also view this ticket from My bookings in SmartRoute Lanka.</p>
                  </div>
                </div>
                """.formatted(escape(booking.getBookingReference()));
    }

    private byte[] buildTicketPdf(Booking booking, String qrCodeDataUri) throws IOException {
        String route = booking.getSchedule().getRoute().getOrigin() + " to " + booking.getSchedule().getRoute().getDestination();
        String seats = booking.getBookingSeats().stream()
                .map(BookingSeat::getSeat)
                .map(seat -> String.valueOf(seat.getSeatNumber()))
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        List<String> passengers = booking.getPassengerDetails().stream()
                .sorted(java.util.Comparator.comparingInt(BookingPassenger::getSeatNumber))
                .map(passenger -> "Seat " + passenger.getSeatNumber() + " - " + passenger.getFullName())
                .toList();
        if (passengers.isEmpty()) {
            passengers = List.of(booking.getPassenger().getUser().getFullName());
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                content.setNonStrokingColor(21, 91, 215);
                content.addRect(0, 742, PDRectangle.A4.getWidth(), 100);
                content.fill();
                content.setNonStrokingColor(255, 255, 255);
                writeText(content, PDType1Font.HELVETICA_BOLD, 22, 48, y, "SmartRoute Lanka");
                writeText(content, PDType1Font.HELVETICA, 12, 48, y - 24, "Confirmed travel ticket");

                content.setNonStrokingColor(16, 44, 71);
                y = 700;
                writeText(content, PDType1Font.HELVETICA_BOLD, 16, 48, y, "Booking Reference: " + booking.getBookingReference());
                y -= 36;
                y = writeLabelValue(content, y, "Route", route);
                y = writeLabelValue(content, y, "Travel date", String.valueOf(booking.getSchedule().getTravelDate()));
                y = writeLabelValue(content, y, "Departure", String.valueOf(booking.getSchedule().getDepartureTime()));
                y = writeLabelValue(content, y, "Arrival", String.valueOf(booking.getSchedule().getArrivalTime()));
                y = writeLabelValue(content, y, "Seats", seats);
                y = writeLabelValue(content, y, "Total", "LKR " + booking.getTotalAmount());

                y -= 12;
                writeText(content, PDType1Font.HELVETICA_BOLD, 13, 48, y, "Passengers");
                y -= 22;
                for (String passenger : passengers) {
                    writeText(content, PDType1Font.HELVETICA, 11, 62, y, passenger);
                    y -= 18;
                }

                byte[] qrBytes = qrPngBytes(qrCodeDataUri);
                if (qrBytes.length > 0) {
                    PDImageXObject qr = PDImageXObject.createFromByteArray(document, qrBytes, "ticket-qr");
                    content.drawImage(qr, 365, 495, 150, 150);
                    writeText(content, PDType1Font.HELVETICA, 9, 375, 478, "Scan at boarding");
                }

                content.setNonStrokingColor(99, 123, 146);
                writeText(content, PDType1Font.HELVETICA, 9, 48, 60, "Keep this PDF ticket with you when boarding. You can also view it from My bookings.");
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private float writeLabelValue(PDPageContentStream content, float y, String label, String value) throws IOException {
        writeText(content, PDType1Font.HELVETICA_BOLD, 11, 48, y, label + ":");
        for (String line : wrap(value, 58)) {
            writeText(content, PDType1Font.HELVETICA, 11, 145, y, line);
            y -= 18;
        }
        return y;
    }

    private void writeText(PDPageContentStream content, PDType1Font font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(pdfSafe(text));
        content.endText();
    }

    private List<String> wrap(String text, int maxLength) {
        String value = String.valueOf(text == null ? "" : text);
        if (value.length() <= maxLength) {
            return List.of(value);
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            if (line.length() + word.length() + 1 > maxLength) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                }
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private byte[] qrPngBytes(String qrCodeDataUri) {
        if (qrCodeDataUri == null || qrCodeDataUri.isBlank()) {
            return new byte[0];
        }
        String data = qrCodeDataUri;
        int comma = data.indexOf(',');
        if (comma >= 0) {
            data = data.substring(comma + 1);
        }
        return Base64.getDecoder().decode(data);
    }

    private String pdfSafe(String value) {
        return String.valueOf(value == null ? "" : value)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String escape(Object value) {
        return String.valueOf(value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
