package telran.monitoring;

import telran.monitoring.logging.Logger;

public class MailSenderTest implements MailSender{
    Logger logger = loggers[0];
    public void sendMail(String subject, String recipientAddress, String text){
        logger.log("finest", String.format("subjec: %s, recipientAddress: %s, textr: %s", subject,
         recipientAddress, text));
    }
}