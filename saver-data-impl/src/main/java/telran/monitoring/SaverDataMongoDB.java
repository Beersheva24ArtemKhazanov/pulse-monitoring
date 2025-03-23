package telran.monitoring;

import java.util.Map;
import org.bson.Document;
import com.mongodb.client.*;
import telran.monitoring.api.SaverData;
import telran.monitoring.logging.Logger;

public class SaverDataMongoDB implements SaverData {
    MongoClient mongoClient;
    MongoDatabase database;
    MongoCollection<Document> collection;
    Map<String, String> envMap = System.getenv();
    static final String DEFAULT_DB_PROTOCOL = "mongodb+srv";
    static final String DEFAULT_DB_USER = "root";
    static final String DEFAULT_DB_CONNECTION_OPTIONS = "retryWrites=true&w=majority&appName=Cluster0";
    static final String DEFAULT_HOST = "cluster0.ujqan.mongodb.net";
    static final String DEFAULT_PORT = "";
    static final String DEFAULT_DATABASE_NAME = "pulse_monitoring";
    static final String DEFAULT_COLLECTION_NAME = "avg_pulse_values";

    Logger logger = loggers[0];
    String protocol = getDbProtocol();
    String user = getDbUser();
    String password = getDbPassword();
    String host = getHost();
    String port = getPort();
    String options = getDbConnectionOptions();
    String databaseName = getDatabaseName();
    String collectionName = getCollectionName();
    String connectionString = getConnectionString();

    public SaverDataMongoDB() {
        configLog();
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(databaseName);
        collection = database.getCollection(collectionName);
        logger.log("info", "connection to MongoDB established");

    }

    private void configLog() {
        logger.log("config", "connection string is " + connectionString);
        logger.log("config", "user: " + user);
        logger.log("config", "host: " + host);
        logger.log("config", "port: " + (port.isEmpty() ? "no port for Atlas" : port));
        logger.log("finest", "password: " + password);
        logger.log("config", "Database: " + databaseName);
        logger.log("config", "Collection: " + collectionName);
    }

    private String getCollectionName() {
        return envMap.getOrDefault("COLLECTION_NAME", DEFAULT_COLLECTION_NAME);
    }

    @Override
    public void saveData(Map<String, Object> data) {
        Document doc = getMongoDocument(data);
        var res = collection.insertOne(doc);
        logger.log("finest", res.getInsertedId() + " inserted document: " + doc);
    }

    private Document getMongoDocument(Map<String, Object> data) {
        Document doc = new Document(data);
        return doc;
    }

    private String getPort() {
        return envMap.getOrDefault("PORT", DEFAULT_PORT);
    }

    private String getHost() {
        return envMap.getOrDefault("HOST", DEFAULT_HOST);
    }

    private String getConnectionString() {
        String connectionString = String.format("%s://%s:%s@%s%s/?%s", protocol,
                user, password, host, port, options);
        return connectionString;
    }

    private String getDatabaseName() {
        return envMap.getOrDefault("DATABASE_NAME", DEFAULT_DATABASE_NAME);
    }

    private String getDbConnectionOptions() {
        return envMap.getOrDefault("DB_CONNECTION_OPTIONS", DEFAULT_DB_CONNECTION_OPTIONS);
    }

    private String getDbPassword() {
        return envMap.get("DB_PASSWORD");
    }

    private String getDbUser() {
        return envMap.getOrDefault("DB_USER", DEFAULT_DB_USER);
    }

    private String getDbProtocol() {
        return envMap.getOrDefault("DB_PROTOCOL", DEFAULT_DB_PROTOCOL);
    }

}