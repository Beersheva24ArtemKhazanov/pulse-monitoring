package telran.monitoring;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;

import telran.monitoring.logging.Logger;

public class EmailProviderClientHttp implements EmailProviderClient {
    private String baseURL = getBaseUrl();
    HttpClient httpClient = HttpClient.newHttpClient();
    Logger logger = loggers[0];
    public EmailProviderClientHttp() {
        logger.log("info", "HTTP client for communicating with Email Provider Service");
        logger.log("config", "baseURL is " + baseURL);
    }

    private String getBaseUrl() {
        String baseUrl = System.getenv("EMAIL_PROVIDER_URL");
        if (baseUrl == null) {
            throw new RuntimeException("No value for RANGE_PROVIDER_URL provided");
        }
        return baseUrl;
    }

    @Override
    public String getNotificationEmailAddress(long patientId) {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(getURI(patientId))).build();
        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() > 399) {
                throw new Exception(response.body());
            }
            String email = response.body();
            logger.log("fine", "Email address received from Email Address Provider API service is " + email);
            return email;
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