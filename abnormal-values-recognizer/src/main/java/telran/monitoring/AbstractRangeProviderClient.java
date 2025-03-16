package telran.monitoring;

import telran.monitoring.logging.Logger;

public abstract class AbstractRangeProviderClient implements RangeProviderClient{
    protected  Logger logger;
    protected AbstractRangeProviderClient(Logger logger) {
        this.logger = logger;
    }

}