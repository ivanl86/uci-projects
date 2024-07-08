import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

// Declaring a WebServlet called DashBoardLoginServlet, which maps to url "/_dashboard"
@WebServlet(name = "DashBoardLoginServlet", urlPatterns = "/_dashboard/login")
public class DashBoardLoginServlet extends HttpServlet {

    private DataSource dataSource;

    private static final Logger LOGGER = Logger.getLogger(DashBoardLoginServlet.class.getName());

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
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");

        LOGGER.info("Verifying employee...");

        final String username = request.getParameter("username");
        final String password = request.getParameter("password");
//        final String G_RECAPTCHA_RESPONSE = request.getParameter("g-recaptcha-response");
//        final String SECRET_KEY = getServletContext().getInitParameter("recaptcha-secret-key");

        String query = "SELECT fullName AS f_name, password AS encrypted_password FROM employees WHERE email = ?";
        JsonObject responseJsonObject = new JsonObject();

//        if (!ReCaptchaVerifyUtils.verify(G_RECAPTCHA_RESPONSE, SECRET_KEY)) {
//
//            responseJsonObject.addProperty("status", "fail");
//            responseJsonObject.addProperty("message", "reCAPTCHA verification failed");
//
//            LOGGER.info("reCAPTCHA failed...");
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//
//        } else {

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                preparedStatement.setString(1, username);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {

                    if (resultSet.next() &&
                            EncryptedPasswordVerifyUtils.verifyPassword(password, resultSet.getString("encrypted_password"))) {

                        request.getSession().setAttribute("employee", new Employee(username, resultSet.getString("f_name")));

                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "Login successes");

                        LOGGER.info("Employee login success");
                        response.setStatus(HttpServletResponse.SC_OK);
                    } else {

                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Invalid email or password");

                        LOGGER.info("Employee login info incorrect");
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                }


            } catch (SQLException e) {

                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Login failed due to server error:\n" + e.getMessage());

                request.getServletContext().log("Server error:\n", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                e.printStackTrace();
            } catch (Exception e) {

                LOGGER.info(e.getMessage());
                e.printStackTrace();
            }
//        }
        response.getWriter().write(responseJsonObject.toString());
    }
}
