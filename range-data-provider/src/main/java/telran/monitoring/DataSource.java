package telran.monitoring;

import java.sql.*;
import java.util.NoSuchElementException;

import telran.monitoring.api.Range;
import telran.monitoring.logging.Logger;


public class DataSource {
    private static final String DEFAULT_DRIVER_CLASS_NAME = "org.postgresql.Driver";
        private static final String MIN_PULSE_VALUE = "min_pulse_value";
        private static final String MAX_PULSE_VALUE = "max_pulse_value";
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
                statement = con.prepareStatement(String.format("select %s, %s from " +
                 "groups where id = (select group_id from patients where patient_id = ?) ", MIN_PULSE_VALUE, MAX_PULSE_VALUE));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Range getRange(long patientId) {
        try {
            statement.setLong(1, patientId);
            ResultSet rs = statement.executeQuery();
            if(rs.next()){
                int min = rs.getInt(MIN_PULSE_VALUE);
                int max = rs.getInt(MAX_PULSE_VALUE);
                Range range = new Range(min, max);
                return range;
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