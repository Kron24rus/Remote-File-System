package tea.tess.server;

import org.apache.commons.codec.digest.DigestUtils;

import java.sql.*;

/**
 * Created by kron on 14.10.15.
 */
public class ServerDao {

    public static Connection connection;
    public static Statement statement;
    public static ResultSet resultSet;

    private static final String DATABASE_NAME = "users.db";
    public static final String TABLE_NAME_USERS = "users";
    public static final String LOGIN = "login";
    public static final String PASS = "pass";
    public static final String LAST_IP = "last_ip";

    private static final String SQL_CREATE_TABLE_USERS = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_USERS +
            "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            LOGIN + " TEXT NOT NULL," +
            PASS + " TEXT NOT NULL," +
            LAST_IP + " CHAR(15))";
    private static final String SQL_DROP_TABLE_USERS = "DROP TABLE IF EXISTS " + TABLE_NAME_USERS;

    public static void Connect() throws ClassNotFoundException, SQLException {
        connection = null;
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_NAME);
        System.out.println("Connection established");
    }

    public static void CreateDB() throws SQLException {
        statement = connection.createStatement();
        statement.execute(SQL_CREATE_TABLE_USERS);
        System.out.println("Table created or already exists");
    }

    public static void CloseDB() throws SQLException {
        connection.close();
        statement.close();
        resultSet.close();
        System.out.println("Connections closed");
    }

    public static void ReadDB() throws SQLException {
        resultSet = statement.executeQuery("SELECT * FROM " + TABLE_NAME_USERS);
        while (resultSet.next()) {
            int id = resultSet.getInt("ID");
            String login = resultSet.getString(LOGIN);
            String pass = resultSet.getString(PASS);
            String ipadr = resultSet.getString(LAST_IP);
            System.out.println(id + " " + login + " : " + pass + " : " + ipadr);
        }
    }

    public static void addUser(String login, String pass, String IP) throws SQLException {
        String sqlIn = "INSERT INTO " + TABLE_NAME_USERS + "(" + LOGIN + "," + PASS + "," + LAST_IP + ")" +
                "VALUES ('" + login + "','" + DigestUtils.md5Hex(pass) + "'," + "'" + IP + "');";
        statement.execute(sqlIn);
    }

    public static void updateUser(String login, String pass, String newIP) throws SQLException {
        String sqlUpdate = "UPDATE " + TABLE_NAME_USERS + " SET " + LAST_IP +
                " = ('" + newIP + "')" + " WHERE " + LOGIN + " = '" + login + "';";
        statement.executeUpdate(sqlUpdate);
    }

    public static int isMember(String login, String pass) throws SQLException {
        String sqlCheck = " SELECT * FROM " + TABLE_NAME_USERS + " WHERE " + LOGIN + "= '" + login + "';";
        resultSet = statement.executeQuery(sqlCheck);
        int total = 0;
        while (resultSet.next()) total++;
        if (total == 1) {
            resultSet = statement.executeQuery(sqlCheck);
            String tmp = resultSet.getString(PASS);
            if (tmp.equals(DigestUtils.md5Hex(pass))) return 1;
            else return 0;
        } else {
            return -1;
        }
    }

    public static int isMember(String login) throws SQLException {
        String sqlCheck = "SELECT * FROM " + TABLE_NAME_USERS + " WHERE " + LOGIN + "= '" + login + "';";
        resultSet = statement.executeQuery(sqlCheck);
        int total = 0;
        while (resultSet.next()) total++;
        if (total == 1) {
            return -1;
        } else {
            return 0;
        }
    }

    public static void dropDatabase() throws SQLException {
        statement = connection.createStatement();
        statement.executeUpdate(SQL_DROP_TABLE_USERS);
    }
}
