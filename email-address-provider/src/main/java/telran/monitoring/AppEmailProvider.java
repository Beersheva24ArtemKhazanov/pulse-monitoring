package telran.monitoring;

import java.util.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import telran.monitoring.logging.*;

public class AppEmailProvider implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String DEFAULT_USER_NAME = "postgres"; // jdbc:postgresql://localhost:5432/database_name"
    private static final String DEFAULT_DB_CONNECTION_STRING = "jdbc:postgresql://patients-db.cwh0qakgm7w0.us-east-1.rds.amazonaws.com:5432/postgres";
    Map<String, String> env = System.getenv();
    String connectionStr = getConnectionString();
    String username = getUsername();
    String password = getPassword();
    Logger logger = new LoggerStandard("range-data-provider");
    DataSource dataSource = new DataSource(connectionStr, username, password, logger);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        configLog();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Map<String, String> path = input.getQueryStringParameters();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            String patientIdStr = "";
            if (path == null || (patientIdStr = path.get("id")) == null) {
                throw new IllegalArgumentException("id parameter must exist");
            }
            long patientId = 0;
            try {
                patientId = Long.parseLong(patientIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("patient id must be number");
            }
            String email = dataSource.getEmailAddress(patientId);

            response
                    .withStatusCode(200)
                    .withBody(email);
        } catch (NoSuchElementException e) {
            response
                    .withBody(e.toString())
                    .withStatusCode(404);
        } catch (IllegalArgumentException e) {
            response
                    .withBody(e.toString())
                    .withStatusCode(400);
        } catch (Exception e) {
            response
                    .withBody(e.toString())
                    .withStatusCode(500);
        }
        return response;
    }

    private void configLog() {
        logger.log("config", "connection string is " + connectionStr);
        logger.log("config", "username is " + username);
        logger.log("finest", "password is " + password);
    }

    private String getPassword() {
        String password = env.get("DB_PASSWORD");
        if (password == null) {
            throw new RuntimeException("password must be specified in environment variable");
        }
        return password;
    }

    private String getUsername() {
        String username = env.getOrDefault("USERNAME", DEFAULT_USER_NAME);
        return username;
    }

    private String getConnectionString() {
        String connectionString = env.getOrDefault("DB_CONNECTION_STRING", DEFAULT_DB_CONNECTION_STRING);
        return connectionString;
    }

}