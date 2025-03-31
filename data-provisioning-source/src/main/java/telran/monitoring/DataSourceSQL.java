package telran.monitoring;

import java.sql.*;
import java.util.*;

import telran.monitoring.logging.*;

public abstract class DataSourceSQL implements DataSource {
    private static final String DEFAULT_DRIVER_CLASS_NAME = "org.postgresql.Driver";
    private static final String DEFAULT_USER_NAME = "postgres"; // jdbc:postgresql://localhost:5432/database_name"
    private static final String DEFAULT_DB_CONNECTION_STRING = "jdbc:postgresql://patients-db.cwh0qakgm7w0.us-east-1.rds.amazonaws.com:5432/postgres";
    protected Map<String, String> env = System.getenv();
    protected String connectionString = getConnectionString();
    String password = getPassword();
    String username = getUsername();
    protected String statementString;
    protected PreparedStatement statement;
    protected String driverClassName = getDriverClassName();
    protected Connection connection;
    Logger logger = loggers[0];

    protected DataSourceSQL(String statementString) {
        this.statementString = statementString;
        configLog();
        try {
            Class.forName(driverClassName);
            connection = DriverManager.getConnection(connectionString, username, password);
            statement = connection.prepareStatement(statementString);

        } catch (Exception e) {
            logger.log("severe", "error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void configLog() {
        logger.log("config", "connection string is " + connectionString);
        logger.log("config", "statement string is " + statementString);
        logger.log("config", "driver class name is " + driverClassName);
        logger.log("config", "username is " + username);
        logger.log("finest", "password is " + password);
    }

    @Override
    public String getData(long patientId) {
        try {
            statement.setLong(1, patientId);
            ResultSet resultSet = statement.executeQuery();
            String result = resultSetProcessing(resultSet);
            logger.log("fine", "result returning from resultSetProcessing is " + result);
            return result;
        } catch (SQLException e) {
            logger.log("severe", "error: " + e);
            throw new RuntimeException(e);

        }

    }

    protected abstract String resultSetProcessing(ResultSet resultSet);

    private String getDriverClassName() {
        return env.getOrDefault("DRIVER_CLASS_NAME", DEFAULT_DRIVER_CLASS_NAME);
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