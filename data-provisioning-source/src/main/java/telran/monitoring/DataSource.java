package telran.monitoring;

import telran.monitoring.logging.Logger;

public interface DataSource {
    String getData(long patientId);
    static Logger [] loggers = new Logger[1];
    static DataSource getDataSource(String dataSourceClassName, Logger logger) {
        try {
            loggers[0] = logger;
            return (DataSource) Class.forName(dataSourceClassName).getConstructor().newInstance();
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }
}
