package telran.monitoring;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;

import telran.monitoring.api.Range;
import telran.monitoring.logging.Logger;

public class RangeProviderClientHttp extends AbstractRangeProviderClient {
    private String baseURL = getBaseUrl();
    HttpClient httpClient = HttpClient.newHttpClient();

    public RangeProviderClientHttp(Logger logger) {
        super(logger);
        logger.log("info", "HTTP client for communicating with Range Provider Service");
        logger.log("config", "baseURL is " + baseURL);
    }

    private String getBaseUrl() {
        String baseUrl = System.getenv("RANGE_PROVIDER_URL");
        if (baseUrl == null) {
            throw new RuntimeException("No value for RANGE_PROVIDER_URL provided");
        }
        return baseUrl;
    }

    @Override
    public Range getRange(long patientId) {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(getURI(patientId))).build();
        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() > 399) {
                throw new Exception(response.body());
            }
            Range range = Range.getRangeFromJSON(response.body());
            logger.log("fine", "Range received from Range Provider API service is "  + range);
            return range;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
       
    }

    private String getURI(long patientId) {
        String uri = baseURL + "?id=" + patientId;
        logger.log("fine", "URI is " + uri);
        return uri;
    }

}