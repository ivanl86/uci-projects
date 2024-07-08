import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.google.gson.JsonPrimitive;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

/**
 * This AddStarServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /dashboard/add-star.
 */
@WebServlet(name = "AddStarServlet", urlPatterns = "/dashboard/add-star")
public class AddStarServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AddStarServlet.class.getName());

    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) {

        LOGGER.info("Connecting to database: moviedb");
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_write");
        } catch (NamingException e) {
            LOGGER.info("Connection failed");
            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace().toString()));
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

        response.setContentType("application/json");
        LOGGER.info("Adding a new star");

        String addStarProcedure = "CALL ADD_STAR(?, ?, ?)";
        JsonObject responseJsonObject = new JsonObject();

        try (PrintWriter out = response.getWriter()) {

            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall(addStarProcedure)) {

                for (String parameterName : request.getParameterMap().keySet()) {
                    LOGGER.info("Parameter: " + parameterName + " = " + request.getParameter(parameterName));
                }
                String birthYear = request.getParameter("star-birth-year");

                callableStatement.setString(1, request.getParameter("star-name"));
                callableStatement.setInt(2, birthYear.isEmpty() ? -1 : Integer.parseInt(birthYear));

                int rowCount = callableStatement.executeUpdate();

                if (rowCount > 0) {
                    String starId = callableStatement.getString(3);
                    responseJsonObject.addProperty("status", "Success");
                    responseJsonObject.addProperty("star-id", starId);
                } else {
                    responseJsonObject.addProperty("status", "Error");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                responseJsonObject.addProperty("status", "Error");
            } finally {
                LOGGER.info(responseJsonObject.toString());
                out.write(responseJsonObject.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
