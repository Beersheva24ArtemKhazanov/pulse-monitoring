package telran.monitoring;
import telran.monitoring.logging.*;
public interface MailSender {
    static Logger [] loggers = new Logger[1];
    void sendMail(String subject, String recipientAddress, String text);
    static MailSender getMailSender(String mailSenderClassName, Logger logger) {
        loggers[0] = logger;
        try {
            return (MailSender)Class.forName(mailSenderClassName).getConstructor().newInstance();
        } catch(Exception e) {

            throw new RuntimeException(e);
        }
    }
}