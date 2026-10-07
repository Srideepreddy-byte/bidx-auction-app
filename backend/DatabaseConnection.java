import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Cloud database URL
    private static final String URL = "jdbc:mysql://mysql-22578928-nsrideepreddy-b942.l.aivencloud.com:23521/defaultdb?sslMode=REQUIRED";
    private static final String USER = "avnadmin";
    
    // Securely fetches the password from the Cloud Provider's environment variables
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("CRITICAL: MySQL JAR not found in classpath!", e);
        }
        
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}