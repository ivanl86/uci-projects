import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MoviesServlet.class.getName());

    private static final long serialVersionUID = 3L;

    private static final String defaultQuery =
            "SELECT * FROM movies ORDER BY title LIMIT 25";
    private static final String browseByGenreQuery =
            "SELECT\n" +
                    "    m.id AS m_id,\n" +
                    "    m.title AS m_title,\n" +
                    "    m.year AS m_year,\n" +
                    "    m.director AS m_director,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.id ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_names,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.id ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.name ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_names,\n" +
                    "    r.rating AS m_rating\n" +
                    "FROM (\n" +
                    "    SELECT\n" +
                    "        m.id AS id,\n" +
                    "        m.title AS title,\n" +
                    "        m.year AS year,\n" +
                    "        m.director AS director,\n" +
                    "        g.id AS g_id,\n" +
                    "        g.name AS g_name\n" +
                    "    FROM\n" +
                    "        movies m\n" +
                    "    LEFT JOIN\n" +
                    "        genres_in_movies gim ON gim.movieId = m.id\n" +
                    "    LEFT JOIN\n" +
                    "        genres g ON g.id = gim.genreId\n" +
                    "    WHERE\n" +
                    "        g.id = ?) AS m\n" +
                    "LEFT JOIN\n" +
                    "    ratings r ON r.movieId = m.id\n" +
                    "LEFT JOIN\n" +
                    "    genres_in_movies gim ON gim.movieId = m.id\n" +
                    "LEFT JOIN\n" +
                    "    genres g ON g.id = gim.genreId\n" +
                    "LEFT JOIN\n" +
                    "    stars_in_movies sim ON sim.movieId = m.id\n" +
                    "LEFT JOIN (\n" +
                    "    SELECT\n" +
                    "        s.id AS id, s.name AS name, count(s.id) AS m_count\n" +
                    "    FROM\n" +
                    "        stars_in_movies sim\n" +
                    "    LEFT JOIN\n" +
                    "        stars s ON s.id = sim.starId\n" +
                    "    GROUP BY\n" +
                    "        s.id) AS s ON s.id = sim.starId\n";
    private static final String browseByTitleQuery =
            "SELECT\n" +
                    "    m.id AS m_id,\n" +
                    "    m.title AS m_title,\n" +
                    "    m.year AS m_year,\n" +
                    "    m.director AS m_director,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.id ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_names,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.id ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.name ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_names,\n" +
                    "    r.rating AS m_rating\n" +
                    "FROM (\n" +
                    "         SELECT\n" +
                    "             m.id AS id,\n" +
                    "             m.title AS title,\n" +
                    "             m.year AS year,\n" +
                    "             m.director AS director,\n" +
                    "             g.id AS g_id,\n" +
                    "             g.name AS g_name\n" +
                    "         FROM\n" +
                    "             movies m\n" +
                    "                 LEFT JOIN\n" +
                    "             genres_in_movies gim ON gim.movieId = m.id\n" +
                    "                 LEFT JOIN\n" +
                    "             genres g ON g.id = gim.genreId\n" +
                    "         WHERE\n" +
                    "             m.title RLIKE ?) AS m\n" +
                    "         LEFT JOIN\n" +
                    "     ratings r ON r.movieId = m.id\n" +
                    "         LEFT JOIN\n" +
                    "     genres_in_movies gim ON gim.movieId = m.id\n" +
                    "         LEFT JOIN\n" +
                    "     genres g ON g.id = gim.genreId\n" +
                    "         LEFT JOIN\n" +
                    "     stars_in_movies sim ON sim.movieId = m.id\n" +
                    "         LEFT JOIN (\n" +
                    "    SELECT\n" +
                    "        s.id AS id, s.name AS name, count(s.id) AS m_count\n" +
                    "    FROM\n" +
                    "        stars_in_movies sim\n" +
                    "            LEFT JOIN\n" +
                    "        stars s ON s.id = sim.starId\n" +
                    "    GROUP BY\n" +
                    "        s.id) AS s ON s.id = sim.starId\n";
    private static final String fullTextSearchQuery =
            "SELECT m.id               AS m_id,\n" +
                    "       m.title            AS m_title,\n" +
                    "       m.year             AS m_year,\n" +
                    "       m.director         AS m_director,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.id ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_names,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.id ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.name ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_names,\n" +
                    "       r.rating           AS m_rating\n" +
                    "FROM movies m\n" +
                    "         LEFT JOIN\n" +
                    "     ratings r ON m.id = r.movieId\n" +
                    "         LEFT JOIN\n" +
                    "     genres_in_movies gim ON m.id = gim.movieId\n" +
                    "         LEFT JOIN\n" +
                    "     genres g ON gim.genreId = g.id\n" +
                    "         LEFT JOIN\n" +
                    "     stars_in_movies sim ON m.id = sim.movieId\n" +
                    "         LEFT JOIN\n" +
                    "     (SELECT s.id, s.name, COUNT(sim.movieId) AS m_count\n" +
                    "      FROM stars s\n" +
                    "               JOIN stars_in_movies sim ON s.id = sim.starId\n" +
                    "      GROUP BY s.id) AS s ON s.id = sim.starId\n" +
                    "WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE)\n" +
                    "OR title LIKE CONCAT(?, '%')";
    private static final String searchMoviesQueryPartOne =
            "SELECT\n" +
                    "    m.id AS m_id,\n" +
                    "    m.title AS m_title,\n" +
                    "    m.year AS m_year,\n" +
                    "    m.director AS m_director,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.id ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), ', ', 3) AS g_names,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.id ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_ids,\n" +
                    "    SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT s.name ORDER BY s.m_count DESC, s.name SEPARATOR ', '), ', ', 3) AS s_names,\n" +
                    "    r.rating AS m_rating\n" +
                    "FROM (\n" +
                    "         SELECT\n" +
                    "             m.id AS id,\n" +
                    "             m.title AS title,\n" +
                    "             m.year AS year,\n" +
                    "             m.director AS director,\n" +
                    "             g.id AS g_id,\n" +
                    "             g.name AS g_name\n" +
                    "         FROM\n" +
                    "             movies m\n" +
                    "                 LEFT JOIN\n" +
                    "             genres_in_movies gim ON gim.movieId = m.id\n" +
                    "                 LEFT JOIN\n" +
                    "             genres g ON g.id = gim.genreId\n" +
                    "                LEFT JOIN\n" +
                    "             stars_in_movies sim ON sim.movieId = m.id\n" +
                    "                LEFT JOIN\n" +
                    "             stars s ON s.id = sim.starId\n" +
                    "         WHERE\n" +
                    "             m.title LIKE ?\n"; // Not working
    private static final String searchMoviesQueryPartTwo =
            ") AS m\n" +
                    "         LEFT JOIN\n" +
                    "     ratings r ON r.movieId = m.id\n" +
                    "         LEFT JOIN\n" +
                    "     genres_in_movies gim ON gim.movieId = m.id\n" +
                    "         LEFT JOIN\n" +
                    "     genres g ON g.id = gim.genreId\n" +
                    "         LEFT JOIN\n" +
                    "     stars_in_movies sim ON sim.movieId = m.id\n" +
                    "         LEFT JOIN (\n" +
                    "    SELECT\n" +
                    "        s.id AS id, s.name AS name, count(s.id) AS m_count\n" +
                    "    FROM\n" +
                    "        stars_in_movies sim\n" +
                    "            LEFT JOIN\n" +
                    "        stars s ON s.id = sim.starId\n" +
                    "    GROUP BY\n" +
                    "        s.id) AS s ON s.id = sim.starId\n";
    private static final String paginationQuery =
            "LIMIT ?\n" +
                    "OFFSET ?";

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
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        LOGGER.info(request.getRequestURI());
        LOGGER.info("Fetching movies");
        response.setContentType("application/json"); // Response mime type
//        PrintWriter out = response.getWriter();

        String genreId = request.getParameter("genre") == null ? "" : request.getParameter("genre");
        String firstChar = request.getParameter("start-with") == null ? "" : request.getParameter("start-with");
        String searchTitle = request.getParameter("search-title") == null ? "" : request.getParameter("search-title");
        String fullTextSearch = request.getParameter("full-text-search") == null ? "" : request.getParameter("full-text-search");

        String searchYear = request.getParameter("search-year") == null ? "" : request.getParameter("search-year");
        String searchDirector = request.getParameter("search-director") == null ? "" : request.getParameter("search-director");
        String searchStar = request.getParameter("search-star") == null ? "" : request.getParameter("search-star");

        Map<String, String> searchFieldMap = new LinkedHashMap<>();
//        List<String> searchFieldList = new ArrayList<>(Arrays.asList(searchTitle, searchYear, searchDirector, searchStar));

        LOGGER.info("Genre ID: " + genreId);
        LOGGER.info("First char: " + firstChar);
        LOGGER.info("Search title: " + searchTitle);
        LOGGER.info("Full text search: " + fullTextSearch);
        searchFieldMap.put("title", searchTitle);
        if (!searchDirector.isEmpty()) {
            searchFieldMap.put("director", searchDirector);
        }
        if (!searchStar.isEmpty()) {
            searchFieldMap.put("star", searchStar);
        }
        if (!searchYear.isEmpty()) {
            searchFieldMap.put("year", searchYear);
        }

        if (!fullTextSearch.isEmpty()) {
            searchByFullText(request, response, fullTextSearch);
        } else if (!genreId.isEmpty()) {
            browseByGenre(request, response, Integer.parseInt(genreId));
        } else if (!firstChar.isEmpty()) {
            browseByTitle(request, response, firstChar);
        } else if ((searchFieldMap.size() > 1) || !searchFieldMap.get("title").isEmpty()) {
            searchMovies(request, response, searchFieldMap);
        } else {
            getCachedResult(request, response);
        }
    }

    private void browseByGenre(HttpServletRequest request, HttpServletResponse response, int genreId) {

        int moviesPerPage = Integer.parseInt(request.getParameter("movies-per-page"));
        int pageNumber = Integer.parseInt(request.getParameter("page-number"));
        int offSet = moviesPerPage * (pageNumber - 1);
        String[] sortingOptions = request.getParameter("sort-by").split("-");
        StringBuilder orderByQuery = getOrderByQueryBuilder(sortingOptions);
        String query = browseByGenreQuery + orderByQuery + paginationQuery;

        LOGGER.info("Browse by Genre");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             PrintWriter out = response.getWriter()) {

            preparedStatement.setInt(1, genreId);
            preparedStatement.setInt(2, moviesPerPage);
            preparedStatement.setInt(3, offSet);


            HttpSession currentSession = request.getSession();

            currentSession.setAttribute("genreId", genreId);
            currentSession.setAttribute("moviePerPage", moviesPerPage);
            currentSession.setAttribute("pageNumber", pageNumber);
            currentSession.setAttribute("sortingOptions", sortingOptions);

            currentSession.removeAttribute("firstChar");
            currentSession.removeAttribute("searchTerms");
            currentSession.removeAttribute("full-text-search");

            getQueryResults(request, response, preparedStatement, out);

        } catch (Exception e) {

            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace()));
        }
    }

    private void browseByTitle(HttpServletRequest request, HttpServletResponse response, String firstChar) {

        int moviesPerPage = Integer.parseInt(request.getParameter("movies-per-page"));
        int pageNumber = Integer.parseInt(request.getParameter("page-number"));
        int offSet = moviesPerPage * (pageNumber - 1);
        String[] sortingOptions = request.getParameter("sort-by").split("-");
        StringBuilder orderByQuery = getOrderByQueryBuilder(sortingOptions);
        String query = browseByTitleQuery + orderByQuery + paginationQuery;

        LOGGER.info("Browse by title");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             PrintWriter out = response.getWriter()) {

            preparedStatement.setString(1, firstChar.equals("*") ?
                    "^[^a-zA-Z0-9]" : "^" + firstChar);
            preparedStatement.setInt(2, moviesPerPage);
            preparedStatement.setInt(3, offSet);

            HttpSession currentSession = request.getSession();

            currentSession.setAttribute("firstChar", firstChar);
            currentSession.setAttribute("moviePerPage", moviesPerPage);
            currentSession.setAttribute("pageNumber", pageNumber);
            currentSession.setAttribute("sortingOptions", sortingOptions);

            currentSession.removeAttribute("genreId");
            currentSession.removeAttribute("searchTerms");
            currentSession.removeAttribute("full-text-search");

            getQueryResults(request, response, preparedStatement, out);

        } catch (Exception e) {

            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace()));
        }
    }

    private void searchMovies(HttpServletRequest request, HttpServletResponse response, Map<String, String> searchFieldMap) {

        int moviesPerPage = Integer.parseInt(request.getParameter("movies-per-page"));
        int pageNumber = Integer.parseInt(request.getParameter("page-number"));
        int offSet = moviesPerPage * (pageNumber - 1);
        String[] sortingOptions = request.getParameter("sort-by").split("-");
        StringBuilder orderByQuery = getOrderByQueryBuilder(sortingOptions);
        String searchTermQueryPartOne = "";
        String searchTermQueryPartTwo = "";
        String query = "";

        if (searchFieldMap.containsKey("director")) {
            searchTermQueryPartOne += "AND m.director LIKE ?\n";
        }
        if (searchFieldMap.containsKey("star")) {
            searchTermQueryPartOne += "AND s.name LIKE ?\n";
        }
        if (searchFieldMap.containsKey("year")) {
            searchTermQueryPartTwo += "WHERE m.year = ?\n";
        }

        query = searchMoviesQueryPartOne + searchTermQueryPartOne +
                searchMoviesQueryPartTwo + searchTermQueryPartTwo +
                orderByQuery + paginationQuery;

        LOGGER.info("Search movies");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             PrintWriter out = response.getWriter()) {

            int index = 1;
            for (Map.Entry<String, String> entry : searchFieldMap.entrySet()) {

                if (entry.getKey().equals("year")) {
                    preparedStatement.setInt(index, Integer.parseInt(entry.getValue()));
                } else {
                    preparedStatement.setString(index, "%" + entry.getValue() + "%");
                }
                ++index;
            }

            preparedStatement.setInt(index, moviesPerPage);
            ++index;
            preparedStatement.setInt(index, offSet);

            HttpSession currentSession = request.getSession();

            currentSession.setAttribute("searchTerms", searchFieldMap);
            currentSession.setAttribute("moviePerPage", moviesPerPage);
            currentSession.setAttribute("pageNumber", pageNumber);
            currentSession.setAttribute("sortingOptions", sortingOptions);

            currentSession.removeAttribute("genreId");
            currentSession.removeAttribute("firstChar");
            currentSession.removeAttribute("full-text-search");

            getQueryResults(request, response, preparedStatement, out);

        } catch (Exception e) {

            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace()));
        }
    }

    private void searchByFullText (HttpServletRequest request, HttpServletResponse response, String title) {
        int moviesPerPage = Integer.parseInt(request.getParameter("movies-per-page"));
        int pageNumber = Integer.parseInt(request.getParameter("page-number"));
        int offSet = moviesPerPage * (pageNumber - 1);
        String[] sortingOptions = request.getParameter("sort-by").split("-");
        StringBuilder orderByQuery = getOrderByQueryBuilder(sortingOptions);
        String query = fullTextSearchQuery + orderByQuery + paginationQuery;
        String[] searchTerms = title.split(" ");
        String fullTextTitle = Arrays.stream(searchTerms).map(term -> term + "* ").collect(Collectors.joining(" "));

        LOGGER.info("Full text searching");
        LOGGER.info("Title: " + title);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             PrintWriter out = response.getWriter()) {

            preparedStatement.setString(1, fullTextTitle);
            preparedStatement.setString(2, title);
            preparedStatement.setInt(3, moviesPerPage);
            preparedStatement.setInt(4, offSet);

            HttpSession currentSession = request.getSession();

            currentSession.setAttribute("full-text-search", title);
            currentSession.setAttribute("moviePerPage", moviesPerPage);
            currentSession.setAttribute("pageNumber", pageNumber);
            currentSession.setAttribute("sortingOptions", sortingOptions);

            currentSession.removeAttribute("genreId");
            currentSession.removeAttribute("firstChar");
            currentSession.removeAttribute("searchTerms");

            LOGGER.info(preparedStatement.toString());

            getQueryResults(request, response, preparedStatement, out);

        } catch (Exception e) {

            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace()));
        }
    }

    private void getQueryResults(HttpServletRequest request, HttpServletResponse response, PreparedStatement preparedStatement, PrintWriter out) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {

            JsonArray moviesJsonArray = getMoviesJsonArray(resultSet);
            HttpSession currentSession = request.getSession();

            // Save the movie results into the current session
            currentSession.setAttribute("moviesResults", moviesJsonArray);

            out.write(moviesJsonArray.toString());

            // Set response status to 200 (OK)
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("error", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isPaginationUpdated(HttpServletRequest request) {

        int moviesPerPage = Integer.parseInt(request.getParameter("movies-per-page"));
        int pageNumber = Integer.parseInt(request.getParameter("page-number"));
        String[] sortingOptions = request.getParameter("sort-by").split("-");

        HttpSession currentSession = request.getSession();
        String[] cachedSortingOptions = ((String[]) currentSession.getAttribute("sortingOptions"));

//        for (String cachedSortingOption : cachedSortingOptions) {
//            LOGGER.info(cachedSortingOption);
//        }

        if (isInteger(currentSession.getAttribute("moviePerPage")) && moviesPerPage != (int) currentSession.getAttribute("moviePerPage")) {
            LOGGER.info("true");
            return true;
        }
        if (isInteger(currentSession.getAttribute("pageNumber")) && pageNumber != (int) currentSession.getAttribute("pageNumber")) {
//            LOGGER.info("true");
            return true;
        }
        for (int i = 0; i < sortingOptions.length; ++i) {
            LOGGER.info(sortingOptions[i]);
            LOGGER.info(cachedSortingOptions[i]);
            if (!sortingOptions[i].equals(cachedSortingOptions[i])) {
//                LOGGER.info("true");
                return true;
            }
        }
//        LOGGER.info("False");
        return false;
    }

    private void getCachedResult(HttpServletRequest request, HttpServletResponse response) {

        try (PrintWriter out = response.getWriter()) {

//            LOGGER.info(request.getParameter("movies-per-page"));
//            LOGGER.info(request.getParameter("page-number"));
//            LOGGER.info(request.getParameter("sort-by"));

            HttpSession currentSession = request.getSession();
            LOGGER.info("Get current session info");

            if (!isPaginationUpdated(request)) {
                LOGGER.info("Not updated, use cached results");
                out.write(currentSession.getAttribute("moviesResults").toString());
            } else if (currentSession.getAttribute("genreId") != null) {
                LOGGER.info("Browse by genre again");
                browseByGenre(request, response, (int) currentSession.getAttribute("genreId"));
                LOGGER.info("Browse by genre again done");
            } else if (currentSession.getAttribute("firstChar") != null) {
                LOGGER.info("Browse by title again");
                browseByTitle(request, response, (String) currentSession.getAttribute("firstChar"));
                LOGGER.info("Browse by title again done");
            } else if (currentSession.getAttribute("searchTerms") != null) {
                LOGGER.info("Search movies again");
                searchMovies(request, response, (Map<String, String>) currentSession.getAttribute("searchTerms"));
                LOGGER.info("Search movies again done");
            }

            LOGGER.info("Get cached result done");

        } catch (IOException e) {
            LOGGER.info(e.getMessage());
            LOGGER.info(String.valueOf(e.getStackTrace()));
        }
    }

    private JsonArray getMoviesJsonArray(ResultSet resultSet) throws SQLException {

        JsonArray movieJsonArray = new JsonArray();

        while (resultSet.next()) {

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

            movieJsonArray.add(movieJsonObject);
        }

        return movieJsonArray;
    }

    private static StringBuilder getOrderByQueryBuilder(String[] sortingOptions) {
        StringBuilder orderByQuery = new StringBuilder(
                "GROUP BY\n" +
                        "    m.id\n" +
                        "ORDER BY\n");

        for (int i = 0; i < sortingOptions.length; ++i) {

            switch (sortingOptions[i]) {
                case "title":
                    orderByQuery.append("m.title ");
                    break;
                case "rating":
                    orderByQuery.append("r.rating ");
                    break;
                case "asc":
                    orderByQuery.append("ASC ");
                    break;
                case "desc":
                    orderByQuery.append("DESC ");
                    break;
            }
            if (i == 1) {
                orderByQuery.append(", ");
            }
            if (i == sortingOptions.length - 1) {
                orderByQuery.append("\n");
            }
        }
        return orderByQuery;
    }

    private static boolean isInteger(Object obj) {

        return obj instanceof Integer;
//        try {
//            Integer.parseInt(str);
//            return true;
//        } catch (NumberFormatException e) {
//            return false;
//        }
    }
}
