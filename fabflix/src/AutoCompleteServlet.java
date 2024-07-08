import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet(name = "AutoCompleteServlet", urlPatterns = "/api/autocomplete")
public class AutoCompleteServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) {
        try {
            // Look up the data source from JNDI
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_read");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String query = request.getParameter("query");
        String[] searchTerms = query.split(" ");
        String title = Arrays.stream(searchTerms)
                .map(term -> term + "* ")
                .collect(Collectors.joining(" "));

        try (Connection conn = dataSource.getConnection()) {
            String sqlQuery =
                    "SELECT id, title\n" +
                            "FROM movies\n" +
                            "WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE)\n" +
                            "OR title LIKE CONCAT(?, '%')\n" +
                            "LIMIT 10";
            try (PreparedStatement statement = conn.prepareStatement(sqlQuery)) {
                statement.setString(1, title);
                statement.setString(2, query);

                JsonArray jsonArray = new JsonArray();
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        jsonArray.add(generateJsonObject(rs.getString("id"), rs.getString("title")));
                    }
                }
                out.write(jsonArray.toString());
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (Exception e) {
            out.write("[]");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    private static JsonObject generateJsonObject(String movieID, String movieTitle) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("value", movieTitle);

        JsonObject additionalDataJsonObject = new JsonObject();
        additionalDataJsonObject.addProperty("movieID", movieID);

        jsonObject.add("data", additionalDataJsonObject);
        return jsonObject;
    }
}
