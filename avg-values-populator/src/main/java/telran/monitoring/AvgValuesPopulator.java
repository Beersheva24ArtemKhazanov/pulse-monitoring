package telran.monitoring;

import java.time.Instant;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;

import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class AvgValuesPopulator {
    private static final String DEFAULT_MONGODB_USERNAME = "root";
    private static final String DEFAULT_MONGODB_CLUSTER = "cluster0.i3qkk.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

    Logger logger = new LoggerStandard("avg-values-populator");
    Map<String, String> env = System.getenv();
    MongoClient mongoClient = MongoClients
            .create("mongodb+srv://%s:%s@%s"
            .formatted(getMongoUser(), getMongoPassword(), getMongoCluster()));
    MongoCollection<Document> collection = mongoClient
            .getDatabase("pulse_monitoring")
            .getCollection("avg_pulse_values");

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(r -> {
            SensorData avgSensorData = getSensorData(r);
            logger.log("finest", "data for saving: %s".formatted(avgSensorData.toString()));
            saveAvgPulseValueToDB(avgSensorData);
        });
    }

    private String getMongoCluster() {
        String res = env.getOrDefault("MONGODB_CLUSTER", DEFAULT_MONGODB_CLUSTER);
        return res;
    }

    private Object getMongoPassword() {
        String res = env.get("MONGODB_PASSWORD");
        if (res == null) {
            throw new RuntimeException("password must be specified in env variable");
        }
        return res;
    }

    private Object getMongoUser() {
        String res = env.getOrDefault("MONGODB_USERNAME", DEFAULT_MONGODB_USERNAME);
        return res;
    }

    private void saveAvgPulseValueToDB(SensorData avgSensorPulseData) {
        Document doc = new Document()
                .append("_id", new ObjectId())
                .append("patientId", avgSensorPulseData.patientId())
                .append("avgValue", avgSensorPulseData.value())
                .append("timestamp", Instant.ofEpochSecond(avgSensorPulseData.timestamp()).toString());
        try {
            collection.insertOne(doc);
            logger.log("fine", "saving to db: %s".formatted(doc.toString()));
        } catch (Exception e) {
            logger.log("error", e.toString());
        }
    }

    private SensorData getSensorData(DynamodbStreamRecord r) {
        Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
        long patientId = Long.parseLong(map.get("patientId").getN());
        int avgValue = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        SensorData avgPulseData = new SensorData(patientId, avgValue, timestamp);
        return avgPulseData;
    }

}