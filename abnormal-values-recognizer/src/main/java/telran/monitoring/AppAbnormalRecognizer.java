package telran.monitoring;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import telran.monitoring.api.AbnormalPulseData;
import telran.monitoring.api.Range;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.*;

public class AppAbnormalRecognizer {
    private static final String DEFAULT_STREAM_NAME = "abnormal-pulse-values";
    private static final String DEFAULT_RANGE_PROVIDER_CLASS = "telran.monitoring.RangeProviderClientHttp";
    private static final String DEFAULT_STREAM_CLASS_NAME = "telran.monitoring.DynamoDbAbnormalPulseValueStream";

    Map<String, String> env = System.getenv();
    String providerClientClassName = getProviderClientClassName();
    String streamName = getStreamName();
    String streamClassName = getStreamClassName();
    Logger logger = new LoggerStandard(streamName);
    MiddlewareDataStream<AbnormalPulseData> dataStream;
    RangeProviderClient providerClient;

    @SuppressWarnings("unchecked")
    public AppAbnormalRecognizer() {
        logger.log("config", "Stream name is " + streamName);
        logger.log("config", "Stream class name is " + streamClassName);
        logger.log("config", "Range Provider Class Name is " + providerClientClassName);
        try {

            dataStream = (MiddlewareDataStream<AbnormalPulseData>) MiddlewareDataStreamFactory.getStream(
                    streamClassName,
                    streamName);
            providerClient = (RangeProviderClient) Class.forName(providerClientClassName).getConstructor(Logger.class)
                    .newInstance(logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getProviderClientClassName() {
        String res = env.getOrDefault("RANGE_PROVIDER_CLASS", DEFAULT_RANGE_PROVIDER_CLASS);
        return res;
    }

    private String getStreamName() {
        String res = env.getOrDefault("STREAM_NAME", DEFAULT_STREAM_NAME);
        return res;
    }

    private String getStreamClassName() {
        String res = env.getOrDefault("STREAM_CLASS_NAME", DEFAULT_STREAM_CLASS_NAME);
        return res;
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
                AbnormalPulseData abnormalPulseValue = checkAbnormal(sensorData);
                if (abnormalPulseValue != null) {
                    dataStream.publish(abnormalPulseValue);
                    logger.log("debug", "Published Abnormal pulse value: " + abnormalPulseValue);
                }
            } else {
                logger.log("severe", "no new image found in event");
            }

        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    private AbnormalPulseData checkAbnormal(SensorData sensorData) {
        long patientId = sensorData.patientId();
        int pulseValue = sensorData.value();
        AbnormalPulseData abnormalPulseValue = null;
        try {
            Range range = providerClient.getRange(patientId);
            int min = range.min();
            int max = range.max();
            logger.log("finest", "returned from Range provider client: " + range);
            if (pulseValue < min || pulseValue > max) {
                abnormalPulseValue = new AbnormalPulseData(patientId, pulseValue, min, max, sensorData.timestamp());
            }
        } catch (Exception e) {
            logger.log("severe", "error - " + e.toString());
        }
        return abnormalPulseValue;

    }

    private SensorData getSensorData(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        SensorData sensorData = new SensorData(patientId, value, timestamp);
        return sensorData;
    }

}