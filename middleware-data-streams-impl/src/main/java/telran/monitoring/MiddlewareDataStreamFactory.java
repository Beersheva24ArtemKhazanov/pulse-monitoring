package telran.monitoring;

import java.lang.reflect.Constructor;

public interface MiddlewareDataStreamFactory {
    @SuppressWarnings("rawtypes")
    public static MiddlewareDataStream getStream(String className, String streamName) throws Exception{
            @SuppressWarnings("unchecked")
            Class<MiddlewareDataStream> clazz = (Class<MiddlewareDataStream>)Class.forName(className);
            Constructor<MiddlewareDataStream> constructor = clazz.getConstructor(String.class);
            MiddlewareDataStream res = constructor.newInstance(streamName);
            return res;
    }
}