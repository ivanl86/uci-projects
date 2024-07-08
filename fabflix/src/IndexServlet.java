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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

/**
 * This IndexServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/index.
 */
@WebServlet(name = "IndexServlet", urlPatterns = "/api/index")
public class IndexServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(IndexServlet.class.getName());

    private static final long serialVersionUID = 3L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) {

        LOGGER.info("Connecting to database: moviedb");
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_read");
        } catch (NamingException e) {
            LOGGER.info("Connection failed");
            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace().toString()));
        }
    }

    /**
     * handles GET requests to store session information
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession();
        String sessionId = session.getId();
        long lastAccessTime = session.getLastAccessedTime();

        getGenreList(request, response);
    }

    private void getGenreList(HttpServletRequest request, HttpServletResponse response) throws IOException {

        LOGGER.info("Fetching genres");
        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {

            // Declare our statement
            Statement statement = conn.createStatement();

            String query = "SELECT\n" +
                    "g.id AS g_id,\n" +
                    "g.name AS g_name\n" +
                    "FROM\n" +
                    "genres g\n" +
                    "ORDER BY\n" +
                    "g.name ASC";

            // Perform the query
            ResultSet resultSet = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of resultSet
            while (resultSet.next()) {

                String genreId = resultSet.getString("g_id");
                String genreName = resultSet.getString("g_name");

                // Create a JsonObject based on the data we retrieve from resultSet
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("genre-id", genreId);
                jsonObject.addProperty("genre-name", genreName);

                jsonArray.add(jsonObject);
            }

            resultSet.close();
            statement.close();

            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " genres");

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("error", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
