package telran.monitoring;

import telran.monitoring.logging.Logger;

public interface EmailProviderClient {
    Logger[] loggers = new Logger[1];
String getNotificationEmailAddress(long patientId);
static EmailProviderClient getEmailProviderClient(String providerClassName, Logger logger) {
    loggers[0] = logger;
    try {
        EmailProviderClient client = (EmailProviderClient) Class.forName(providerClassName).getConstructor().newInstance();
        return client;
    } catch (Exception e) {
        logger.log("severe", "error: " + e);
        throw new RuntimeException(e);
    }
}
}