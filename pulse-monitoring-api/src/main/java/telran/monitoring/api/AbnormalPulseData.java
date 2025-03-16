package telran.monitoring.api;

import org.json.JSONObject;

public record AbnormalPulseData (long patientId, int value, int min_value, int max_value,
 long timestamp){
    public static AbnormalPulseData getAbnormalPulseValueFromJSON (String json) {
        JSONObject jsonObj = new JSONObject(json);
        long patientId = jsonObj.getLong("patientId");
        int value = jsonObj.getInt("value");
        int min_value = jsonObj.getInt("min_value");
        int max_value = jsonObj.getInt("max_value");
        long timestamp = jsonObj.getLong("timestamp");
        return new AbnormalPulseData(patientId, value, min_value, max_value, timestamp);
    }
    @Override
    public String toString() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("patientId", patientId);
        jsonObj.put("value", value);
        jsonObj.put("min_value", min_value);
        jsonObj.put("max_value", max_value);
        jsonObj.put("timestamp", timestamp);
        return jsonObj.toString();
    }
}