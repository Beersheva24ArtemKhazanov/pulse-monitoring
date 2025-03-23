package telran.monitoring.api;

import java.util.List;

import telran.monitoring.logging.Logger;

public interface LatestValuesSaver {
   void addValue(SensorData sensorData);
   List <SensorData> getAllValues(long patientId);
   SensorData getLastValue(long patientId);
   void clearValues(long patientId);
   void clearAndAddValue(long patientId, SensorData sensorData);
   public static LatestValuesSaver getLatestValuesSaver(String saverClassName, Logger logger) {
      try {
         LatestValuesSaver res = null;
         res = (LatestValuesSaver) Class.forName(saverClassName).getConstructor(Logger.class)
         .newInstance(logger);
         return res;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
}