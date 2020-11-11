package server;

import java.sql.*;
import java.util.TimeZone;
import com.mysql.jdbc.Driver;


public class ClientBase {
    private Connection connection;

    public ClientBase() {

        /*подключение драйвера*/
        try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("Connection successful!");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver not found");
        }

        try {
            DriverManager.registerDriver(new Driver());
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/client?serverTimezone=" + TimeZone.getDefault().getID(),
                    "root",
                    "root");

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Driver Registration error");
        }
    }

    public UserInfo getRecord(String login) {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format("SELECT * FROM client.record WHERE login = '%s';", login));
            if (resultSet.next()) {
                UserInfo record = new UserInfo (
                        resultSet.getInt("id"),
                        resultSet.getString("login"),
                        resultSet.getString("password")
                );
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return record;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Statement error");
        }

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
