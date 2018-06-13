
import java.sql.*;

public class DbConnector {

    private static final String URL = "jdbc:mysql://localhost:3306/cloud?autoReconnect=true&" +
            "useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWD = "zcofcjex";
    private static String pstmtGetNicknameQuery = "SELECT nickname FROM users WHERE username = ? AND " +
            "password = ? ";
    private static String pstmtCreateUserQuery = "INSERT INTO users " +
            "(fio, username,password,nickname) " +
            "VALUES(?, ?, ?, ?);";

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet resultSet;
    private static PreparedStatement pstmtGetNickname, pstmtRegisterUser;

    static void connect() throws SQLException {

        connection = DriverManager.getConnection(URL, USER, PASSWD);
        pstmtGetNickname = connection.prepareStatement(pstmtGetNicknameQuery);
        pstmtRegisterUser = connection.prepareStatement(pstmtCreateUserQuery);

    }

    static String getNickname(String username, String password) {

        String nickname = null;

        try {
            pstmtGetNickname.setString(1, username);
            pstmtGetNickname.setString(2, password);
            resultSet = pstmtGetNickname.executeQuery();
            System.out.println(resultSet);
            while (resultSet.next()) {
                nickname = resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nickname;
    }

    static String registerUser(String fio, String username, String password, String nick) {
        String nickname = null;

        try {
            pstmtRegisterUser.setString(1, fio);
            pstmtRegisterUser.setString(2, username);
            pstmtRegisterUser.setString(3, password);
            pstmtRegisterUser.setString(4, nick);
            pstmtRegisterUser.executeUpdate();

            nickname = getNickname(username, password);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nickname;
    }


}
