package telran.monitoring;

import java.time.*;
import java.util.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import telran.monitoring.api.*;

import telran.monitoring.logging.*;

public class AppReducedValuesSaver {
    String DEFAULT_DATA_SAVER_CLASS_NAME = "telran.monitoring.SaverDataMongoDB";
    Map<String, String> env = System.getenv();
    Logger logger = new LoggerStandard("reduced-values-saver");
    String dataSaverClassName = getDataSaverClassName();
    SaverData saverData;

    public AppReducedValuesSaver() {
        logger.log("config", "Data Saver class name is " + dataSaverClassName);
        saverData = SaverData.getSaverDataInstance(dataSaverClassName, logger);
        
    }

    private String getDataSaverClassName() {
        return env.getOrDefault("DATA_SAVER_CLASS_NAME", DEFAULT_DATA_SAVER_CLASS_NAME);
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(this::sensorDataProcessing);

    }

    private void sensorDataProcessing(DynamodbStreamRecord record) {
        String eventName = record.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = record.getDynamodb().getNewImage();
            if (map != null) {
                SensorData sensorData = getSensorData(map);
                logger.log("finest", sensorData.toString());
                Map<String, Object> mapForSaving = getMapForSaving(sensorData);
                logger.log("finest", "map passed to saver is " + mapForSaving);
                saverData.saveData(mapForSaving);
            } else {
                logger.log("severe", "no new image found in event");
            }

        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    private Map<String, Object> getMapForSaving(SensorData sensorData) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("patientId", sensorData.patientId());
        map.put("avg_value", sensorData.value());
        map.put("date_time", getDateTime(sensorData.timestamp()));
        return map;
    }

    private LocalDateTime getDateTime(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp / 1000);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime;
    }

    private SensorData getSensorData(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        SensorData sensorData = new SensorData(patientId, value, timestamp);
        return sensorData;
    }

}