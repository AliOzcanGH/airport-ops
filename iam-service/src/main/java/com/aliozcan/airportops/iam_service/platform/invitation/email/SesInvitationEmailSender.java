package com.aliozcan.airportops.iam_service.platform.invitation.email;

import com.aliozcan.airportops.iam_service.config.MailProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class SesInvitationEmailSender implements InvitationEmailSender {

    private static final String SUBJECT = "You're invited to Airport Ops";
    private static final String CHARSET = "UTF-8";

    private final SesV2Client sesV2Client;
    private final MailProperties mailProperties;

    public SesInvitationEmailSender(
            SesV2Client sesV2Client,
            MailProperties mailProperties) {
        this.sesV2Client = sesV2Client;
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(InvitationEmailMessage message) {
        if (mailProperties.from() == null || mailProperties.from().isBlank()) {
            throw new InvitationEmailSendException("app.mail.from is not configured");
        }

        try {
            sesV2Client.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(mailProperties.from())
                    .destination(Destination.builder()
                            .toAddresses(message.recipientEmail())
                            .build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(content(SUBJECT))
                                    .body(Body.builder()
                                            .text(content(textBody(message)))
                                            .html(content(htmlBody(message)))
                                            .build())
                                    .build())
                            .build())
                    .build());
        } catch (RuntimeException exception) {
            throw new InvitationEmailSendException(
                    "SES invitation email delivery failed",
                    exception);
        }
    }

    private Content content(String value) {
        return Content.builder()
                .charset(CHARSET)
                .data(value)
                .build();
    }

    private String textBody(InvitationEmailMessage message) {
        return """
                You have been invited to manage %s in Airport Ops.

                Accept your invitation:
                %s

                This invitation expires at %s.
                """
                .formatted(
                        message.organizationName(),
                        message.acceptUrl(),
                        formattedExpiration(message));
    }

    private String htmlBody(InvitationEmailMessage message) {
        return """
                <p>You have been invited to manage <strong>%s</strong> in Airport Ops.</p>
                <p><a href="%s">Accept your invitation</a></p>
                <p>This invitation expires at %s.</p>
                """
                .formatted(
                        escapeHtml(message.organizationName()),
                        escapeHtml(message.acceptUrl()),
                        formattedExpiration(message));
    }

    private String formattedExpiration(InvitationEmailMessage message) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .withZone(ZoneOffset.UTC)
                .format(message.expiresAt());
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
