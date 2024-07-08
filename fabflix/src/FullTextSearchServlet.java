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

@WebServlet(name = "FullTextSearchServlet", urlPatterns = "/api/full-text-search")
public class FullTextSearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_read");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String query = request.getParameter("new-search-title");
        if (query == null || query.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"errorMessage\": \"Query parameter is missing\"}");
            return;
        }

        String[] tokens = query.split("\\s+");
        StringBuilder booleanQuery = new StringBuilder();
        for (String token : tokens) {
            booleanQuery.append("+").append(token).append("* ");
        }

        String sqlQuery = "SELECT \n" +
                "    m.id AS m_id, \n" +
                "    m.title AS m_title, \n" +
                "    m.year AS m_year, \n" +
                "    m.director AS m_director,\n" +
                "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.id ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_ids,\n" +
                "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_names,\n" +
                "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.id ORDER BY s_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_ids,\n" +
                "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.name ORDER BY s_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_names,\n" +
                "    r.rating AS m_rating\n" +
                "FROM \n" +
                "    movies m\n" +
                "LEFT JOIN genres_in_movies gim ON gim.movieId = m.id\n" +
                "LEFT JOIN genres g ON g.id = gim.genreId\n" +
                "LEFT JOIN stars_in_movies sim ON sim.movieId = m.id\n" +
                "LEFT JOIN (\n" +
                "    SELECT \n" +
                "        s.id, \n" +
                "        s.name, \n" +
                "        COUNT(*) AS s_count\n" +
                "    FROM \n" +
                "        stars s\n" +
                "    JOIN stars_in_movies sim ON sim.starId = s.id\n" +
                "    GROUP BY \n" +
                "        s.id, s.name\n" +
                ") AS s ON s.id = sim.starId\n" +
                "LEFT JOIN ratings r ON r.movieId = m.id\n" +
                "WHERE \n" +
                "    MATCH(m.title) AGAINST(? IN BOOLEAN MODE)\n" +
                "GROUP BY \n" +
                "    m.id, m.title, m.year, m.director, r.rating;\n";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sqlQuery)) {

            statement.setString(1, booleanQuery.toString().trim());

            ResultSet resultSet = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            while (resultSet.next()) {
                JsonObject jsonObject = new JsonObject();
                String movieId = resultSet.getString("m_id");
                String movieTitle = resultSet.getString("m_title");
                String movieYear = resultSet.getString("m_year");
                String movieDirector = resultSet.getString("m_director");
                String[] genreIds = resultSet.getString("g_ids").split(", ");
                String[] genreNames = resultSet.getString("g_names").split(", ");
                String[] starIds = (resultSet.getString("s_ids") != null) ? resultSet.getString("s_ids").split(", ") : null;
                String[] starNames = (resultSet.getString("s_names") != null) ? resultSet.getString("s_names").split(", ") : null;
                String movieRating = resultSet.getString("m_rating");

                JsonArray genreIdJsonArray = new JsonArray();
                for (String genreId : genreIds) {
                    genreIdJsonArray.add(new JsonPrimitive(genreId));
                }
                JsonArray genreNameJsonArray = new JsonArray();
                for (String genreName : genreNames) {
                    genreNameJsonArray.add(new JsonPrimitive(genreName));
                }
                JsonArray starIdJsonArray = new JsonArray();
                if (starIds != null) {
                    for (String starId : starIds) {
                        starIdJsonArray.add(new JsonPrimitive(starId));
                    }
                }
                JsonArray starNameJsonArray = new JsonArray();
                if (starNames != null) {
                    for (String starName : starNames) {
                        starNameJsonArray.add(new JsonPrimitive(starName));
                    }
                }
                JsonObject movieJsonObject = new JsonObject();

                movieJsonObject.addProperty("movie-id", movieId);
                movieJsonObject.addProperty("movie-title", movieTitle);
                movieJsonObject.addProperty("movie-year", movieYear);
                movieJsonObject.addProperty("movie-director", movieDirector);
                movieJsonObject.add("genre-ids", genreIdJsonArray);
                movieJsonObject.add("genre-names", genreNameJsonArray);
                movieJsonObject.add("star-ids", starIdJsonArray);
                movieJsonObject.add("star-names", starNameJsonArray);
                movieJsonObject.addProperty("movie-rating", movieRating == null ?
                        "N/A" : movieRating);

                jsonArray.add(movieJsonObject);
            }

            resultSet.close();
            statement.close();

            out.write(jsonArray.toString());
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            out.close();
        }
    }
}