package telran.monitoring;

public class EmailProviderClientTest implements EmailProviderClient{

    @Override
    public String getNotificationEmailAddress(long patientId) {
        return "lowshilla@@gmail.com";
    }

}