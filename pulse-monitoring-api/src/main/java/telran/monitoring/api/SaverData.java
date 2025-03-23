package telran.monitoring.api;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import telran.monitoring.logging.Logger;

public interface SaverData {
    void saveData(Map<String, Object> data);
    static Logger[] loggers = new Logger[1];
    static  SaverData getSaverDataInstance(String className, Logger logger ) {
        loggers[0] = logger;
        try {
            return (SaverData) Class.forName(className).getConstructor().newInstance();
        } catch (Exception e) {
            logger.log("severe", "error: " + e);
            throw new RuntimeException(e);
        }
    }
}