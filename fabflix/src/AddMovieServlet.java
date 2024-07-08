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
 * This AddMovieServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /dashboard/add-movie.
 */
@WebServlet(name = "AddMovieServlet", urlPatterns = "/dashboard/add-movie")
public class AddMovieServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AddMovieServlet.class.getName());

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        LOGGER.info("Adding a new movie");

        String addMovieProcedure = "CALL ADD_MOVIE(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        JsonObject responseJsonObject = new JsonObject();

        try (PrintWriter out = response.getWriter()) {

            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall(addMovieProcedure)) {

                for (String parameterName : request.getParameterMap().keySet()) {
                    LOGGER.info("Parameter: " + parameterName + " = " + request.getParameter(parameterName));
                }
                String movieYear = request.getParameter("movie-year");
                String birthYear = request.getParameter("star-birth-year");

                callableStatement.setString(1, request.getParameter("movie-title"));
                callableStatement.setInt(2, Integer.parseInt(movieYear));
                callableStatement.setString(3, request.getParameter("movie-director"));
                callableStatement.setString(4, request.getParameter("movie-star"));
                callableStatement.setInt(5, birthYear.isEmpty() ? -1 : Integer.parseInt(birthYear));
                callableStatement.setString(6, request.getParameter("genre-name"));
                callableStatement.registerOutParameter(7, Types.VARCHAR);
                callableStatement.registerOutParameter(8, Types.VARCHAR);
                callableStatement.registerOutParameter(9, Types.INTEGER);
                callableStatement.registerOutParameter(10, Types.INTEGER);

                int rowCount = callableStatement.executeUpdate();
                String statusCode = callableStatement.getString(10);

                LOGGER.info("Row count: " + rowCount);
                LOGGER.info("Status code: " + statusCode);

                if (Integer.parseInt(statusCode) == 1) {
                    String movieId = callableStatement.getString(7);
                    String starId = callableStatement.getString(8);
                    String genreId = callableStatement.getString(9);

                    responseJsonObject.addProperty("status", "Success");
                    responseJsonObject.addProperty("message", "Movie added successfully");
                    responseJsonObject.addProperty("movie-id", movieId);
                    responseJsonObject.addProperty("star-id", starId);
                    responseJsonObject.addProperty("genre-id", genreId);
                    responseJsonObject.addProperty("status-code", statusCode);
                } else {
                    responseJsonObject.addProperty("status", "Error");
                    responseJsonObject.addProperty("message", "Duplicate movie");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                responseJsonObject.addProperty("status", "Error");
                responseJsonObject.addProperty("message", "Server error");
            } finally {
                LOGGER.info(responseJsonObject.toString());
                out.write(responseJsonObject.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
