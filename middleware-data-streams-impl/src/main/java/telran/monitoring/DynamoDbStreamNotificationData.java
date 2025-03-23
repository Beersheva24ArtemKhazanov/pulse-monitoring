package telran.monitoring;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import telran.monitoring.api.NotificationData;

public class DynamoDbStreamNotificationData extends DynamoDbStream<NotificationData> {

    public DynamoDbStreamNotificationData(String table) {
            super(table);
        }
    
        @Override
    Map<String, AttributeValue> getMap(NotificationData data) {
        HashMap<String, AttributeValue> map = new HashMap<>(){{
            put("patientId",AttributeValue.builder().n(data.patientId() + "").build());
            put("notificationId",AttributeValue.builder().n(data.notificationId() + "").build());
            put("email",AttributeValue.builder().s(data.email() + "").build());
            put("timestamp",AttributeValue.builder().n(data.timestamp() + "").build());
            put("notificationText",AttributeValue.builder().s(data.notificationText() + "").build());
        }};
        return map;
    }

}
