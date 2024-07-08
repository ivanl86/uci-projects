import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Arrays;

public class EncryptEmployeePassword {
    /*
     *
     * This program updates your existing moviedb employees table to change the
     * plain text passwords to encrypted passwords.
     *
     * You should only run this program **once**, because this program uses the
     * existing passwords as real passwords, then replace them. If you run it more
     * than once, it will treat the encrypted passwords as real passwords and
     * generate wrong values.
     *
     */
    public static void main(String[] args) {

        try {
            // Look up the JDBC DataSource
            String loginUser = "mytestuser";
            String loginPasswd = "mypassword";
            String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            String alterEmployeesQuery =
                    "ALTER TABLE employees\n" +
                            "MODIFY COLUMN password VARCHAR(128)";
            String selectEmployeeQuery = "SELECT email, password FROM employees";

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                 Statement statement = connection.createStatement()) {

                int alteredRow = statement.executeUpdate(alterEmployeesQuery);
                System.out.println("Altering employees table schema completed, " + alteredRow + " row(s) affected");

                PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

                String updatePasswordQuery = "UPDATE employees SET password = ? WHERE email = ?";
                int[] batchResults;

                System.out.println("Encrypting employees password...");
                try (ResultSet resultSet = statement.executeQuery(selectEmployeeQuery);
                     PreparedStatement preparedStatement = connection.prepareStatement(updatePasswordQuery)) {

                    connection.setAutoCommit(false);

                    while (resultSet.next()) {

                        String email = resultSet.getString("email");
                        String encryptedPassword = passwordEncryptor.encryptPassword(resultSet.getString("password"));

                        preparedStatement.setString(1, encryptedPassword);
                        preparedStatement.setString(2, email);
                        preparedStatement.addBatch();
                    }

                    batchResults = preparedStatement.executeBatch();
                    preparedStatement.clearBatch();
                    connection.commit();
                    connection.setAutoCommit(true);
                }

                int successBatchCount = Arrays.stream(batchResults).filter(statusValue -> statusValue == 1).sum();
                int failedBatchCount = Arrays.stream(batchResults).filter(statusValue -> statusValue < 0).sum();

                System.out.println("Password encryption completed,");
                System.out.println(successBatchCount + " row(s) affected");
                System.out.println(failedBatchCount + " row(s) rejected");
            }
        } catch (SQLException e) {
            e.printStackTrace();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();

        } catch (InstantiationException e) {
            e.printStackTrace();

        } catch (IllegalAccessException e) {
            e.printStackTrace();

        } catch (InvocationTargetException e) {
            e.printStackTrace();

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
