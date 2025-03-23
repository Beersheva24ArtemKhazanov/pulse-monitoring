package telran.monitoring;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import telran.monitoring.api.LatestValuesSaver;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class AppReducer {
    private static final String DEFAULT_STREAM_NAME = "average-pulse-values";
    private static final String DEFAULT_STREAM_CLASS_NAME = "telran.monitoring.DynamoDbStreamSensorData";
    private static final long DEFAULT_REDUCING_TIME_WINDOW = 10 * 60 * 1000;
    private static final int DEFAULT_REDUCING_SIZE = 100;
    private Map<String, String> env = System.getenv();
    private String streamName = getStreamName();

    Logger logger = new LoggerStandard(streamName);
    private String streamClassName = getStreamClassName();
    MiddlewareDataStream<SensorData> dataStream;
    int reducingSize = getReducingSize();
    long reducingTimeWindow = getReducingTimeWindow();
    LatestValuesSaver latestValuesSaver = new LatestValuesSaverMap(logger);

    @SuppressWarnings("unchecked")
    public AppReducer() {
        logger.log("config", "Stream name is " + streamName);
        try {

            dataStream = (MiddlewareDataStream<SensorData>) MiddlewareDataStreamFactory.getStream(streamClassName,
                    streamName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long getReducingTimeWindow() {
        long res = 0;
        String resStr = env.get("REDUCING_TIME_WINDOW");
        try {
            res = resStr == null ? DEFAULT_REDUCING_TIME_WINDOW : Long.parseLong(resStr);

        } catch (NumberFormatException e) {
            res = DEFAULT_REDUCING_TIME_WINDOW;
            logger.log("severe", "Wrong env variable ReducingTimeWindow value - taken default value");
        }
        logger.log("config", "Reducing Time Window is " + res);
        return res;
    }

    private int getReducingSize() {
        int res = 0;
        String resStr = env.get("REDUCING_SIZE");
        try {
            res = resStr == null ? DEFAULT_REDUCING_SIZE : Integer.parseInt(resStr);

        } catch (NumberFormatException e) {
            res = DEFAULT_REDUCING_SIZE;
            logger.log("severe", "Wrong env variable ReducingSize value - taken default value");
        }
        logger.log("config", "Reducing Size is " + res);
        return res;
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(r -> {
            sensorDataProcessing(r);
        });

    }

    private String getStreamName() {
        String result = env.getOrDefault("STREAM_NAME", DEFAULT_STREAM_NAME);

        return result;
    }

    private String getStreamClassName() {
        String result = env.getOrDefault("STREAM_CLASS_NAME", DEFAULT_STREAM_CLASS_NAME);
        logger.log("config", "Stream class name is " + result);
        return result;
    }

    private void sensorDataProcessing(DynamodbStreamRecord r) {
        String eventName = r.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
            if (map != null) {
                SensorData sensorData = getSensorData(map);
                logger.log("finest", sensorData.toString());
                SensorData avgSensorData = avgReducing(sensorData);
                if (avgSensorData != null) {
                    dataStream.publish(avgSensorData);
                    logger.log("debug", "Published Average SensorData: " + avgSensorData);
                }
            } else {
                logger.log("severe", "no new image found in event");
            }

        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    private SensorData avgReducing(SensorData sensorData) {
        long patientId = sensorData.patientId();
        List<SensorData> latestData = latestValuesSaver.getAllValues(patientId);

        SensorData sensorDataResult = null;
        if (!latestData.isEmpty()) {
            long lastTimestamp = latestData.getLast().timestamp();
            if (latestData.size() >= reducingSize ||
                    System.currentTimeMillis() - lastTimestamp >= reducingTimeWindow) {
                sensorDataResult = new SensorData(patientId, getAvgValue(latestData),
                 lastTimestamp);
                latestValuesSaver.clearValues(patientId);
            }
        }
        latestValuesSaver.addValue(sensorData);
        logger.log("finest", "adding  value in saver " + sensorData);

        return sensorDataResult;
    }

    private int getAvgValue(List<SensorData> latestData) {
        int res = (int) Math.round(latestData.stream().mapToInt(SensorData::value).average().orElse(0));
        logger.log("fine", "Average pulse value is " + res);
        return res;
    }

    private SensorData getSensorData(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        SensorData sensorData = new SensorData(patientId, value, timestamp);
        return sensorData;
    }

}