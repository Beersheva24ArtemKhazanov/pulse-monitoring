package telran.monitoring.api;

import org.json.JSONObject;

public record Range(int min, int max) {
    public static Range getRangeFromJSON(String json) {
            JSONObject jsonObj = new JSONObject(json);
            int min = jsonObj.getInt("min");
            int max = jsonObj.getInt("max");
            return new Range(min, max);
    }
    @Override
    public String toString() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("min", min);
        jsonObj.put("max", max);
        return jsonObj.toString();
    }
}