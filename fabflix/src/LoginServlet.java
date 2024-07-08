import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {

    private DataSource dataSource;

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    public void init() {
        try {
            // Look up the JDBC DataSource
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_read");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");

        LOGGER.info("Verifying customer...");

        final String username = request.getParameter("username");
        final String password = request.getParameter("password");
//        final String G_RECAPTCHA_RESPONSE = request.getParameter("g-recaptcha-response");
//        final String SECRET_KEY = getServletContext().getInitParameter("recaptcha-secret-key");

        String query =
                "SELECT\n" +
                        "id AS c_id,\n" +
                        "password AS encrypted_password\n" +
                        "FROM customers\n" +
                        "WHERE email = ?";

        JsonObject responseJsonObject = new JsonObject();

//        if (!ReCaptchaVerifyUtils.verify(G_RECAPTCHA_RESPONSE, SECRET_KEY)) {
//
//            LOGGER.info("reCAPTCHA failed...");
//
//            responseJsonObject.addProperty("status", "fail");
//            responseJsonObject.addProperty("message", "reCAPTCHA verification failed");
//            response.getWriter().write(responseJsonObject.toString());

//        } else {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement statement = conn.prepareStatement(query)) {

                statement.setString(1, username);

                try (ResultSet resultSet = statement.executeQuery()) {
                    // Verify customer credentials
                    if (resultSet.next() && EncryptedPasswordVerifyUtils.verifyPassword(password, resultSet.getString("encrypted_password"))) {

                        request.getSession().setAttribute("user", new User(username, resultSet.getInt("c_id")));

                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "Login successful");
                        response.getWriter().write(responseJsonObject.toString());

                        LOGGER.info("Customer login success");
                        response.setStatus(200);

                    } else {
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Invalid email or password.");
                        response.getWriter().write(responseJsonObject.toString());

                        LOGGER.info("Customer login failed");
                        response.setStatus(200);
                    }
                }
            } catch (Exception e) {
                // Log error to localhost log
                request.getServletContext().log("Login failed", e);
                // Prepare error message JSON response
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Login failed due to system error: " + e.getMessage());
                response.getWriter().write(responseJsonObject.toString());

                response.setStatus(401);
            }

//        }
    }
}
