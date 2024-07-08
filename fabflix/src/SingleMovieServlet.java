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
import java.util.logging.Logger;

// Declaring a WebServlet called SingleMovieServlet, which maps to url "/api/single-movie"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    private static final Logger LOGGER = Logger.getLogger(SingleMovieServlet.class.getName());

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

        LOGGER.info("Fetching movie by id: " + id);

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // Construct a query with parameter represented by "?"
            String query = "SELECT " +
                    "m.id AS movieId, " +
                    "m.title AS title, " +
                    "m.year AS year, " +
                    "m.director AS director, " +
                    "GROUP_CONCAT(DISTINCT g.id ORDER BY g.name SEPARATOR ', ') AS g_ids, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS g_names, " +
                    "GROUP_CONCAT(DISTINCT s.id ORDER BY s.movie_count DESC, s.name SEPARATOR ', ') AS s_ids, " +
                    "GROUP_CONCAT(DISTINCT s.name ORDER BY s.movie_count DESC, s.name SEPARATOR ', ') AS s_names, " +
                    "r.rating AS rating " +
                    "FROM movies AS m " +
                    "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
                    "LEFT JOIN genres AS g ON gim.genreId = g.id " +
                    "LEFT JOIN stars_in_movies AS sim ON m.id = sim.movieId " +
                    "LEFT JOIN ratings AS r ON m.id = r.movieId " +
                    "LEFT JOIN ( " +
                    "    SELECT s.id, s.name, COUNT(sim.movieId) AS movie_count " +
                    "    FROM stars AS s " +
                    "    JOIN stars_in_movies AS sim ON s.id = sim.starId " +
                    "    GROUP BY s.id " +
                    ") AS s ON sim.starId = s.id " +
                    "WHERE m.id = ? " +
                    "GROUP BY m.id ";

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            statement.setString(1, id);

            // Perform the query
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {

                String movieTitle = rs.getString("title");
                String movieYear = rs.getString("year");
                String movieDirector = rs.getString("director");

                LOGGER.info("Movie title: " + movieTitle);
                LOGGER.info("Movie id: " + id);
                LOGGER.info("Movie year: " + movieYear);
                LOGGER.info("Movie director: " + movieDirector);

                String[] genreId = (rs.getString("g_ids") != null) ? rs.getString("g_ids").split(", ") : null;
                String[] genreName = (rs.getString("g_names") != null) ? rs.getString("g_names").split(", ") : null;

                String[] starId = (rs.getString("s_ids") != null) ? rs.getString("s_ids").split(", ") : null;
                String[] starName = (rs.getString("s_names") != null) ? rs.getString("s_names").split(", ") : null;

                String rating = rs.getString("rating") == null ? "N/A" : rs.getString("rating") ;

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);
                JsonArray genreIdArray = new JsonArray();
                if (genreId != null) {

                    for (String gid : genreId) {
                        genreIdArray.add(new JsonPrimitive(gid));
                    }
                }
                jsonObject.add("genre_id", genreIdArray);

                JsonArray genreNameArray = new JsonArray();
                if (genreName != null) {

                    for (String gname : genreName) {
                        genreNameArray.add(new JsonPrimitive(gname));
                    }
                }
                jsonObject.add("genre_name", genreNameArray);

                JsonArray starIdArray = new JsonArray();
                if (starId != null) {

                    for (String sid : starId) {
                        starIdArray.add(new JsonPrimitive(sid));
                    }
                }
                jsonObject.add("star_id", starIdArray);

                JsonArray starNameArray = new JsonArray();
                if (starName != null) {

                    for (String sname : starName) {
                        starNameArray.add(new JsonPrimitive(sname));
                    }
                }
                jsonObject.add("star_name", starNameArray);
                jsonObject.addProperty("movie_rating", rating);

                jsonArray.add(jsonObject);
            }
            rs.close();
            statement.close();

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
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