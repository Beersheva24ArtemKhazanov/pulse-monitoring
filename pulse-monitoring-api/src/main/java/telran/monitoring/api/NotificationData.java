package telran.monitoring.api;

public record NotificationData(long notificationId, long patientId, 
    String email, 
    String notificationText, long timestamp) {

}
