package telran.monitoring;

import java.util.HashMap;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import telran.monitoring.api.AbnormalPulseData;

public class DynamoDbStreamAbnormalPulseData extends DynamoDbStream<AbnormalPulseData>{
    public DynamoDbStreamAbnormalPulseData(String table) {
        super(table);
    }

    @Override
    public HashMap<String, AttributeValue> getMap(AbnormalPulseData data) {
        HashMap<String, AttributeValue> map = new HashMap<>(){{
            put("patientId",AttributeValue.builder().n(data.patientId() + "").build());
            put("value",AttributeValue.builder().n(data.value() + "").build());
            put("min_value",AttributeValue.builder().n(data.min_value() + "").build());
            put("max_value",AttributeValue.builder().n(data.max_value() + "").build());
            put("timestamp",AttributeValue.builder().n(data.timestamp() + "").build());


        }};
        return map;
    }
}