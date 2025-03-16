package telran.monitoring;

import java.util.HashMap;
import java.util.NoSuchElementException;

import telran.monitoring.api.Range;
import telran.monitoring.logging.Logger;

public class RangeProviderClientMap extends AbstractRangeProviderClient{
   
    HashMap<Long, Range> map = new HashMap<>(){{
        put(101L, new Range(35, 250));
        put(102L, new Range(55, 210));
        put(103L, new Range(65, 200));
       
    }};
    public RangeProviderClientMap(Logger logger) {
        super(logger);
        logger.log("finest", "Only for Test Range Provider based on regular HashMap");
    }
    @Override
    public Range getRange(long patientId) {
        Range range = map.get(patientId);
        if (range == null) {
            throw new NoSuchElementException(String.format("Patient %d not found", patientId));

        }
        return range;
    }

}