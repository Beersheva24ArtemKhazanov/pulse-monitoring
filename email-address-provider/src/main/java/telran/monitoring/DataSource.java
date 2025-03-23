package telran.monitoring;

import java.sql.*;
import java.util.NoSuchElementException;

import telran.monitoring.logging.Logger;


public class DataSource {
    private static final String DEFAULT_DRIVER_CLASS_NAME = "org.postgresql.Driver";
        private static final String EMAIL_ADDRESS = "email_address";
        PreparedStatement statement;
        static Logger logger;
    static String driverClassName;
        static {
           driverClassName  = getDriverClassName();
            
            try { 
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        Connection con;
    
        public DataSource(String connectionStr, String username, String password, Logger logger) {
            DataSource.logger = logger;
            logger.log("info","driver class name" + driverClassName);
            try {
                con = DriverManager.getConnection(connectionStr, username, password);
                statement = con.prepareStatement(String.format("select %s from " +
                 "notification_groups where id = (select notification_group_id from patients where patient_id = ?) ", EMAIL_ADDRESS));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public String getEmailAddress(long patientId) {
        try {
            statement.setLong(1, patientId);
            ResultSet rs = statement.executeQuery();
            if(rs.next()){
                String email = rs.getString(EMAIL_ADDRESS);
                return email;
            } else {
                throw new NoSuchElementException(String.format("patient with id %d doesn't exist", patientId));
            }

        } catch (SQLException e) {
           throw new RuntimeException(e);
        }
        
    }

    private static String getDriverClassName() {
        String driverClassName = System.getenv("DRIVER_CLASS_NAME");
        if (driverClassName == null) {
            driverClassName = DEFAULT_DRIVER_CLASS_NAME;
        }
        return driverClassName;
    }

}