import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

    private static final Logger LOGGER = Logger.getLogger(PaymentServlet.class.getName());

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_write");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String ccNumber = request.getParameter("ccNumber");
        String expirationDate = request.getParameter("expirationDate");

        LOGGER.info(firstName + " " + lastName + " " + ccNumber + " " + expirationDate);

        PrintWriter out = response.getWriter();

        LOGGER.info("doPost called properly");

        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User is not logged in.");
            return;
        }
        int customerId = user.getCustomerId();

        JsonArray cart = (JsonArray) request.getSession().getAttribute("cart");
        if (cart == null || cart.size() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Your cart is empty.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            LOGGER.info("Looking up credit card info");
            String query = "SELECT id FROM creditcards WHERE firstName = ? AND lastName = ? AND id = ? AND expiration = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, ccNumber);
            statement.setString(4, expirationDate);

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                LOGGER.info("Credit card info is correct");

                conn.setAutoCommit(false);
                String insertSaleQuery = "INSERT INTO sales (customerId, movieId, saleDate, quantity) VALUES (?, ?, NOW(), ?)";
                PreparedStatement saleStatement = conn.prepareStatement(insertSaleQuery);

                for (JsonElement element : cart) {
                    JsonObject item = element.getAsJsonObject();
                    String movieId = item.get("movie-id").getAsString();
                    int quantity = item.get("quantity").getAsInt();

                    saleStatement.setInt(1, customerId);
                    saleStatement.setString(2, movieId);
                    saleStatement.setInt(3, quantity);
                    saleStatement.addBatch();
                }

                saleStatement.executeBatch();
                conn.commit();
                saleStatement.close();

                LOGGER.info("Insert successfull");

                request.getSession().setAttribute("cart", new JsonArray());

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("status", "success");
                jsonObject.addProperty("message", "Transaction completed successfully.");
                out.write(jsonObject.toString());
                response.setStatus(200);

            } else {
                LOGGER.info("Credit card info is incorrect");
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("status", "fail");
                jsonObject.addProperty("message", "Invalid credit card details. Please check and try again.");
                out.write(jsonObject.toString());
                response.setStatus(401);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            request.getServletContext().log("Error during payment processing:", e);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", "Failed to process payment: " + e.toString());
            out.write(jsonObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
