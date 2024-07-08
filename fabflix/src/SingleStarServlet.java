import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.google.gson.JsonPrimitive;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_read");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String id = request.getParameter("id");

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // Construct a query with parameter represented by "?"
            String query = "SELECT " +
                                "s.name AS s_name, " +
                                "s.birthYear AS s_birth_year, " +
                                "GROUP_CONCAT(DISTINCT m.id ORDER BY m.year DESC, m.title SEPARATOR ', ') AS m_ids, " +
                                "GROUP_CONCAT(DISTINCT m.title ORDER BY m.year DESC, m.title SEPARATOR ', ') AS m_titles " +
                            "FROM " +
                                "stars s " +
                            "LEFT JOIN " +
                                "stars_in_movies sim ON sim.starId = s.id " +
                            "LEFT JOIN " +
                                "movies m ON m.id = sim.movieId " +
                            "WHERE " +
                                "s.id = ?";

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            statement.setString(1, id);

            // Perform the query
            ResultSet resultSet = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (resultSet.next()) {

                String starName = resultSet.getString("s_name");
                String starBirthYear = resultSet.getString("s_birth_year");
                String[] movieIds = resultSet.getString("m_ids").split(", ");
                String[] movieTitles = resultSet.getString("m_titles").split(", ");

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("star-name", starName);
                jsonObject.addProperty("star-birth-year", starBirthYear == null ? "N/A" : starBirthYear);

                JsonArray movieIdJsonArray = new JsonArray();

                for (String movieId : movieIds) {
                    movieIdJsonArray.add(new JsonPrimitive(movieId));
                }

                jsonObject.add("movie-ids", movieIdJsonArray);

                JsonArray movieTitleJsonArray = new JsonArray();

                for (String movieTitle : movieTitles) {
                    movieTitleJsonArray.add(new JsonPrimitive(movieTitle));
                }

                jsonObject.add("movie-titles", movieTitleJsonArray);

                jsonArray.add(jsonObject);
            }
            resultSet.close();
            statement.close();

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

}
