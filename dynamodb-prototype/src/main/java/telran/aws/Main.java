package telran.aws;


import java.util.HashMap;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest.Builder;
import telran.monitoring.api.SensorData;

public class Main {
    public static void main(String[] args) {
        DynamoDbClient client = DynamoDbClient.builder().build();
        Builder request = PutItemRequest.builder();
        request = request.tableName("pulse_values");
        client.putItem(request.item(getMap()).build());
    }

    static HashMap<String, AttributeValue> getMap () {
        SensorData sensorData = new SensorData(3, 80, System.currentTimeMillis());
        HashMap<String, AttributeValue> map = new HashMap<>(){{
            put("patientId", AttributeValue.builder().n(sensorData.patientId() + "").build());
            put("value", AttributeValue.builder().n(sensorData.value() + "").build());
            put("timestamp", AttributeValue.builder().n(sensorData.timestamp() + "").build());
        }};
        return map;
    }
}