package telran.monitoring;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;

public class LatestDataSaverS3 extends AbstractDataSaverLogger {
    private static final String DEFAULT_REGION_FOR_AWS = "us-east-1";
        Map<String, String> env = System.getenv();
        Region region = getRegion();
        String bucketName = getBucketName();
        S3Client s3Client;
    
        public LatestDataSaverS3(Logger logger) {
            super(logger);
            logger.log("config", "region is " + region);
            if (bucketName == null) {
                logger.log("severe", "error: no value of BUCKET_NAME");
                throw new RuntimeException("no value of BUCKET_NAME env. variable");
            }
            logger.log("config", "bucket name is " + bucketName);
            s3Client = S3Client.builder()
                    .region(region)
                    .build();
        }
    
        private String getBucketName() {
            return env.get("BUCKET_NAME");
        }
    
        private Region getRegion() {
            String regionStr = env.getOrDefault("REGION_FOR_AWS", DEFAULT_REGION_FOR_AWS);
        logger.log("finest", "region value of the REGION_FOR_AWS variable is " + regionStr );
        return Region.of(regionStr);
    }

    @Override
    public void addValue(SensorData sensorData) {
        getAndPutList(sensorData, false);
    }

    private void getAndPutList(SensorData sensorData, boolean withClear) {
        long patientId = sensorData.patientId();
        List<SensorData> list = getListForPatientId(patientId);
        if (withClear) {
            list.clear();
        }
        list.add(sensorData);
        putListForPatientId(patientId, list);
    }

    private void putListForPatientId(long patientId, List<SensorData> list) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(patientId + ".json")
                            .contentType("application/json") // Important for JSON files
                            .build(),
                    RequestBody.fromString(getJsonFromList(list), StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.log("severe", "error: " + e);
        }
    }

    private String getJsonFromList(List<SensorData> list) {
        JSONArray jsonArray = new JSONArray();
        list.stream().map(SensorData::toString).forEach(jsonArray::put);
        String res = jsonArray.toString();
        logger.log("finest", "json content is " + res);
        return res;
    }

    private List<SensorData> getListForPatientId(long patientId) {
        List<SensorData> list = new ArrayList<>();
        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(patientId + ".json")
                            .build());
            String jsonData = objectBytes.asString(StandardCharsets.UTF_8);
            fillListFromJson(jsonData, list);
        } catch (NoSuchKeyException e) {
            logger.log("finest",
                    String.format("no json file for patient with id %d, empty list will be returned", patientId));
            
        } catch (Exception e) {
            logger.log("severe", "error: " + e);
            throw e;
        }
        return list;
    }

    private void fillListFromJson(String jsonData, List<SensorData> list) {
        logger.log("finest", "JSON received in getListFromJson is " + jsonData);
        JSONArray jsonArray = new JSONArray(jsonData);
        jsonArray.forEach(jo -> list.add(SensorData.of(jo.toString())));
        logger.log("finest", "list returned from getListFromJson is " + list);
    }

    @Override
    public List<SensorData> getAllValues(long patientId) {
        List<SensorData> list = getListForPatientId(patientId);
        logger.log("finest", "received list in getAllValues is " + list);
        return list;
    }

    @Override
    public SensorData getLastValue(long patientId) {
        List<SensorData> list = getListForPatientId(patientId);
        logger.log("finest", "received list in getAllValues is " + list);
        return list.isEmpty() ? null : list.getLast();
    }

    @Override
    public void clearValues(long patientId) {
        List<SensorData> list = getListForPatientId(patientId);
        list.clear();
        putListForPatientId(patientId, list);
    }

    @Override
    public void clearAndAddValue(long patientId, SensorData sensorData) {
        getAndPutList(sensorData, true);
    }

}