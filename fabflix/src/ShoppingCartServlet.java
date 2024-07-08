import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.logging.Logger;

// Declaring a WebServlet called ShoppingCartServlet, which maps to url "/shopping-cart"
@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ShoppingCartServlet.class.getName());

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {

        LOGGER.info("Connecting to database: moivedb");
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_read");
        } catch (NamingException e) {
            LOGGER.info("Connection failed");
            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace()));
        }
    }

    /**
     * handles GET requests to manage items in the shopping cart
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Get a instance of current session on the request
        HttpSession session = request.getSession();

        // Retrieve data named "cart" from session
        JsonArray cartJsonArray = (JsonArray) session.getAttribute("cart");

        if (cartJsonArray == null) {
            cartJsonArray = new JsonArray();
        }

        String action = request.getParameter("action");

        switch (action) {
            case "add":
                addMovie(request, response, session, cartJsonArray);
                break;
            case "increase":
                increaseQuantity(request, response, session, cartJsonArray);
                break;
            case "decrease":
                decreaseQuantity(request, response, session, cartJsonArray);
                break;
            case "delete":
                deleteMovie(request, response, session, cartJsonArray);
                break;
            default:
                displayCart(response, cartJsonArray);
        }
    }

    private void displayCart(HttpServletResponse response, JsonArray cartJsonArray) {

        try (PrintWriter out = response.getWriter()) {

            out.write(cartJsonArray.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addMovie(HttpServletRequest request, HttpServletResponse response, HttpSession session, JsonArray cartJsonArray) {

        String movieId = request.getParameter("movie-id");

        // check if the movie is already in the cart
        // if yes, update the quantity and return without query
        for (JsonElement jsonElement : cartJsonArray) {

            if (jsonElement.isJsonObject()) {

                JsonObject movieJsonObject = jsonElement.getAsJsonObject();

                if (movieJsonObject.get("movie-id").getAsString().equals(movieId)) {

                    LOGGER.info("Added another copy");
                    movieJsonObject.addProperty("quantity", movieJsonObject.get("quantity").getAsInt() + 1);
                    session.setAttribute("cart", cartJsonArray); // Update the cart in the session
                    return;
                }
            }
        }

        // Retrieve the movie id, title, and price
        try (Connection conn = dataSource.getConnection()) {

            LOGGER.info("Find movie in database");

            String query = "SELECT m.title as m_title, p.purchasePrice as p_price FROM movies m LEFT JOIN prices p ON p.movieId = m.id WHERE m.id = ?";

            PreparedStatement statement = conn.prepareStatement(query);

            statement.setString(1, movieId);

            LOGGER.info("Execute the query");
            ResultSet resultSet = statement.executeQuery();

            String movieTitle;
            String purchasePrice;

            while (resultSet.next()) {

                LOGGER.info("Movie found");
                movieTitle = resultSet.getString("m_title");
                purchasePrice = resultSet.getString("p_price");

                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("movie-id", movieId);
                jsonObject.addProperty("movie-title", movieTitle);
                jsonObject.addProperty("purchase-price", purchasePrice);
                jsonObject.addProperty("quantity", 1);

                cartJsonArray.add(jsonObject);
            }

            session.setAttribute("cart", cartJsonArray);
            resultSet.close();
            statement.close();

            response.setStatus(200);
        } catch (Exception e) {
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {

        }
    }

    private void increaseQuantity(HttpServletRequest request, HttpServletResponse response, HttpSession session, JsonArray cartJsonArray) {

        String movieId = request.getParameter("movie-id");

        // If "cartJsonArray" is not found on session, return
        if (cartJsonArray == null) {
            LOGGER.info("Cart is empty");
            return;
        }

        LOGGER.info("movie id: " + movieId);

        // check if the movie is already in the cart
        // if yes, update the quantity
        Iterator<JsonElement> cartIterator = cartJsonArray.iterator();

        while (cartIterator.hasNext()) {

            JsonElement jsonElement = cartIterator.next();

            if (jsonElement.isJsonObject()) {

                JsonObject movieJsonObject = jsonElement.getAsJsonObject();

                if (movieJsonObject.get("movie-id").getAsString().equals(movieId)) {

                    LOGGER.info("Adds another copy");
                    movieJsonObject.addProperty("quantity", movieJsonObject.get("quantity").getAsInt() + 1);

                    LOGGER.info("Updates the cart in session");
                    session.setAttribute("cart", cartJsonArray); // Update the cart in the session

                    break;
                }
            }
        }

        displayCart(response, cartJsonArray);
    }

    private void decreaseQuantity(HttpServletRequest request, HttpServletResponse response, HttpSession session, JsonArray cartJsonArray) {

        String movieId = request.getParameter("movie-id");

        // If "cartJsonArray" is not found on session, return
        if (cartJsonArray == null) {
            LOGGER.info("Cart is empty");
            return;
        }

        LOGGER.info("movie id: " + movieId);

        // check if the movie is already in the cart
        // if yes, update the quantity
        Iterator<JsonElement> cartIterator = cartJsonArray.iterator();

        while (cartIterator.hasNext()) {

            JsonElement jsonElement = cartIterator.next();

            if (jsonElement.isJsonObject()) {

                JsonObject movieJsonObject = jsonElement.getAsJsonObject();

                if (movieJsonObject.get("movie-id").getAsString().equals(movieId)) {

                    int updatedQuantity = movieJsonObject.get("quantity").getAsInt() - 1;

                    if (updatedQuantity > 0) {
                        LOGGER.info("Removes another copy");
                        movieJsonObject.addProperty("quantity", updatedQuantity);
                    } else {
                        LOGGER.info("Removes the movie");
                        cartIterator.remove();
                    }

                    LOGGER.info("Updates the cart in session");
                    session.setAttribute("cart", cartJsonArray); // Update the cart in the session

                    break;
                }
            }
        }

        displayCart(response, cartJsonArray);
    }

    private void deleteMovie(HttpServletRequest request, HttpServletResponse response, HttpSession session, JsonArray cartJsonArray) {

        String movieId = request.getParameter("movie-id");

        // If "cartJsonArray" is not found on session, means this is a new user,
        // thus we create a new movieList ArrayList for the user
        if (cartJsonArray == null) {
            LOGGER.info("Cart is empty");
            cartJsonArray = new JsonArray();
            return;
        }

        Iterator<JsonElement> cartIterator = cartJsonArray.iterator();

        while (cartIterator.hasNext()) {

            JsonElement jsonElement = cartIterator.next();

            if (jsonElement.isJsonObject()) {

                JsonObject movieJsonObject = jsonElement.getAsJsonObject();

                if (movieJsonObject.get("movie-id").getAsString().equals(movieId)) {

                    cartIterator.remove();
                    LOGGER.info("Movie deleted");
                    LOGGER.info("Updates the cart in session");
                    session.setAttribute("cart", cartJsonArray); // Update the cart in the session

                    break;
                }
            }
        }

        displayCart(response, cartJsonArray);
    }
}
