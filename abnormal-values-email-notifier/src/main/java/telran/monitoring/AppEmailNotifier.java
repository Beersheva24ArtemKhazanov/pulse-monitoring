package telran.monitoring;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;
import telran.monitoring.api.AbnormalPulseData;
import telran.monitoring.logging.*;

public class AppEmailNotifier {
    private static final String DEFAULT_REGION_FOR_AWS = "us-east-1";
    private static final String DEFAULT_EMAIL_PROVIDER_CLASS_NAME = "telran.monitoring.EmailProviderClientHttp";
    private static final String DEFAULT_ADDRESS_PREFIX = "";
    public static final String DEFAULT_SENDER_EMAIL_ADDRESS = "yuriaws25@gmail.com";
    private static final String DEFAULT_EMAIL_SUBJECT = "Abnormal pulse value patient ";
    Logger logger = new LoggerStandard("email-notifier");
    Map<String, String> env = System.getenv();
    String providerClientClassName = getProviderClientClassName();
    String senderEmail = getSenderEmail();
    String addressPrefix = getAddressPrefix();
    Region region = getRegion();
    String subject = getSubject();
    SesClient sesClient;
    EmailProviderClient providerClient;

    public AppEmailNotifier() {
        configLog();
        try {
            sesClient = SesClient.builder()
                    .region(region)
                    .build();
            providerClient = EmailProviderClient.getEmailProviderClient(providerClientClassName,
                    logger);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getSubject() {
        return env.getOrDefault("EMAIL_SUBJECT",
                DEFAULT_EMAIL_SUBJECT);
    }

    private void configLog() {
        logger.log("config", "providerClientClassName is " + providerClientClassName);
        logger.log("config", "emailAddressSender is " + senderEmail);
        logger.log("config", "addressPrefix is " + addressPrefix);
        logger.log("config", "region is " + region);
        logger.log("config", "email subject " + subject);
    }

    private String getProviderClientClassName() {
        return env.getOrDefault("EMAIL_PROVIDER_CLASS_NAME",
                DEFAULT_EMAIL_PROVIDER_CLASS_NAME);
    }

    private String getAddressPrefix() {
        return env.getOrDefault("ADDRESS_PREFIX", DEFAULT_ADDRESS_PREFIX);
    }

    private String getSenderEmail() {
        return env.getOrDefault("SENDER_EMAIL_ADDRESS", DEFAULT_SENDER_EMAIL_ADDRESS);
    }

    private Region getRegion() {
        String regionStr = env.getOrDefault("REGION_FOR_AWS", DEFAULT_REGION_FOR_AWS);
        logger.log("finest", "region value of the REGION_FOR_AWS variable is " + regionStr);
        return Region.of(regionStr);
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(this::sensorDataProcessing);

    }

    private void sensorDataProcessing(DynamodbStreamRecord record) {
        String eventName = record.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = record.getDynamodb().getNewImage();
            if (map != null) {
                AbnormalPulseData abnormalPulseValue = getAbnormalPulseData(map);
                logger.log("finest", abnormalPulseValue.toString());
                SendEmailResponse response = sendMail(abnormalPulseValue);
                logger.log("fine", "response: " + response);

            } else {
                logger.log("severe", "no new image found in event");
            }

        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    private SendEmailResponse sendMail(AbnormalPulseData abnormalPulseValue) {
        long patientId = abnormalPulseValue.patientId();
        String recipientEmail = addressPrefix + providerClient.getNotificationEmailAddress(patientId);
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(recipientEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject + patientId).build())
                            .body(Body.builder()
                                    .text(Content.builder().data(getEmailText(abnormalPulseValue)).build())
                                    .build())
                            .build())
                    .source(senderEmail) // Must be the verified email
                    .build();

            // Send email
            return sesClient.sendEmail(emailRequest);

        } catch (SesException e) {
            logger.log("severe", "error of sending mail: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException(e);
        }
    }

    private String getEmailText(AbnormalPulseData abnormalPulseValue) {
        String text = String.format("Notification about abnormal pulse value\n"
                + "Patient with id %d has pulse value %d\n"
                + "the patient belongs to group with\n"
                + "minimal pulse value - %d\n"
                + "maximal pulse value - %d",
                abnormalPulseValue.patientId(),
                abnormalPulseValue.value(),
                abnormalPulseValue.min_value(),
                abnormalPulseValue.max_value());
        logger.log("finest", "text: " + text);
        return text;
    }

    private AbnormalPulseData getAbnormalPulseData(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        int minValue = Integer.parseInt(map.get("min_value").getN());
        int maxValue = Integer.parseInt(map.get("max_value").getN());
        ;
        AbnormalPulseData abnormalPulseValue = new AbnormalPulseData(patientId, value, minValue, maxValue, timestamp);
        return abnormalPulseValue;
    }

}