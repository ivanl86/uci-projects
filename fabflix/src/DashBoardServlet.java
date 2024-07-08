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
import java.util.logging.Logger;

/**
 * This IndexServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/index.
 */
@WebServlet(name = "DashBoardServlet", urlPatterns = "/dashboard/employee")
public class DashBoardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashBoardServlet.class.getName());

    private static final long serialVersionUID = 1L;

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

        String[] tableNames = {"creditcards", "customers", "employees", "genres", "genres_in_movies", "movies", "prices", "ratings", "sales", "stars", "stars_in_movies"};
        String getColProcedure = "CALL GET_TABLE_COLUMN(?)";
        LOGGER.info("Getting meta data...");

        try (Connection connection = dataSource.getConnection();
             CallableStatement callableStatement = connection.prepareCall(getColProcedure);
             PrintWriter out = response.getWriter()) {

            JsonArray tableJsonArray = new JsonArray();

            for (String tableName : tableNames) {

                callableStatement.setString(1, tableName);

                try (ResultSet resultSet = callableStatement.executeQuery()) {

                    JsonObject tableDataJsonObject = new JsonObject();
                    tableDataJsonObject.addProperty("table-name", tableName);
                    JsonArray colNameJsonArray = new JsonArray();
                    JsonArray colTypeJsonArray = new JsonArray();

                    LOGGER.info(tableName);

                    while (resultSet.next()) {

                        colNameJsonArray.add(new JsonPrimitive(resultSet.getString("COLUMN_NAME")));
                        colTypeJsonArray.add(new JsonPrimitive(resultSet.getString("COLUMN_TYPE")));
                    }

                    tableDataJsonObject.add("col-name", colNameJsonArray);
                    tableDataJsonObject.add("col-type", colTypeJsonArray);

                    tableJsonArray.add(tableDataJsonObject);

                    LOGGER.info(tableJsonArray.toString());
                }
            }
            out.write(tableJsonArray.toString());

        } catch (SQLException e) {
            LOGGER.info(e.getMessage());
            e.printStackTrace();
        }
    }
}

