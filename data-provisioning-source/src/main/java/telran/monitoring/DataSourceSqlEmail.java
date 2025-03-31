package telran.monitoring;

import java.sql.ResultSet;
import java.util.NoSuchElementException;

public class DataSourceSqlEmail extends DataSourceSQL {

    private static final String EMAIL_ADDRESS = "email_address";
    private static final String NOTIFICATION_GROUPS_TABLE = "notification_groups";
    private static final String NOTIFICATIONS_GROUP_ID = "notification_group_id";
    private static final String PATIENTS_TABLE = "patients";
    private static final String PATIENT_ID = "patient_id";

    protected DataSourceSqlEmail() {
        super(String.format("select %s from %s where id = (select %s from %s where %s = ?)",
                EMAIL_ADDRESS, NOTIFICATION_GROUPS_TABLE, NOTIFICATIONS_GROUP_ID,
                PATIENTS_TABLE, PATIENT_ID));

    }

    @Override
    protected String resultSetProcessing(ResultSet resultSet) {
        try {
            if (resultSet.next()) {
                    String emailAddress = resultSet.getString(EMAIL_ADDRESS);
                    logger.log("fine", "email address received from DB is " + emailAddress);
                    return emailAddress;
            } else {
                throw new NoSuchElementException("no data about patient exist in Database");
            }
        } catch (Exception e) {
           logger.log("severe", "error: " + e);
           throw new NoSuchElementException();
        }

    }

}