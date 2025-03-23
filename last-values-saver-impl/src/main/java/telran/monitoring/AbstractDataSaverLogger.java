package telran.monitoring;

import telran.monitoring.api.LatestValuesSaver;
import telran.monitoring.logging.Logger;

public abstract class AbstractDataSaverLogger implements LatestValuesSaver{
    protected Logger logger;
    protected AbstractDataSaverLogger(Logger logger) {
        this.logger = logger;
    }
}