import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MovieSAXParser extends DefaultHandler {

    private final int MAX_BATCH_SIZE = 2500;

    private final String loginUser = "mytestuser";
    private final String loginPasswd = "mypassword";
    private final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

    private String director;
    private String chars;
    private Movie movie;
    private final Set<Movie> storedMovieSet;
    private final Set<Movie> newMovieSet;
    private final Set<Movie> parsedMovieSet;
    private final Set<String> storedGenreSet;
    private final Set<String> parsedGenreSet;
    private int totalParseCount;
    private int totalInsertCount;
    private int totalRejectedCount;
    private int newGenreCount;

    public MovieSAXParser() {

        storedMovieSet = new HashSet<>();
        newMovieSet = new HashSet<>();
        parsedMovieSet = new HashSet<>();
        storedGenreSet = new HashSet<>();
        parsedGenreSet = new HashSet<>();
        totalParseCount = 0;
        totalInsertCount = 0;
        totalRejectedCount = 0;
        newGenreCount = 0;

        final String getAllMovieQuery =
                "SELECT m.title AS m_title,\n" +
                        "m.year AS m_year,\n" +
                        "m.director AS m_director\n" +
                        "FROM movies m";
        final String getAllGenreQuery =
                "SELECT g.name AS g_name\n" +
                        "FROM genres g;";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(getAllMovieQuery);
                     ResultSet resultSet = preparedStatement.executeQuery()) {

                    while (resultSet.next()) {
                        storedMovieSet.add(
                                new Movie(
                                        resultSet.getString("m_title"),
                                        resultSet.getInt("m_year"),
                                        resultSet.getString("m_director")));
                    }
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement(getAllGenreQuery);
                     ResultSet resultSet = preparedStatement.executeQuery()) {

                    while (resultSet.next()) {
                        storedGenreSet.add(resultSet.getString("g_name"));
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void parseMovie() {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

            SAXParser saxParser = saxParserFactory.newSAXParser();

            System.out.println("Start parsing...");
            saxParser.parse("stanford-movies/mains243.xml", this);

            if (!newMovieSet.isEmpty()) {
                System.out.println("Start adding last batch...");
                addMovieBatch();
                System.out.println("Finish adding last batch...");
            }
            System.out.println("Start adding genre");
            addGenreBatch();
            System.out.println("Finish adding genre");

            System.out.println("Start linking genres to movies...");
            addGenreInMovieBatch();
            System.out.println("Finish linking genres to movies...");

            System.out.println("Finished parsing...");
            System.out.println("Total movie parsed count: " + totalParseCount);
            System.out.println("Total movie insert count: " + totalInsertCount);
            System.out.println("Total genre insert count: " + newGenreCount);
            System.out.println("Total rejected movies: " + totalRejectedCount);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        if (qName.equalsIgnoreCase("film")) {
            movie = new Movie();
            movie.setDirector(director);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        chars = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {

        if (qName.equalsIgnoreCase("film")) {

            if (movie.getTitle() != null && movie.getYear() != -1 && movie.getDirector() != null && !storedMovieSet.contains(movie)) {
                newMovieSet.add(movie);
                ++totalParseCount;
            } else {
                System.out.println("Rejected movie: " + movie);
                ++totalRejectedCount;
            }
        }

        if (newMovieSet.size() >= MAX_BATCH_SIZE) {
            addMovieBatch();
        }

        switch (qName.toLowerCase()) {
            case "dirname":
                director = chars;
                break;
            case "t":
                movie.setTitle(chars);
                break;
            case "cat":
                setGenre();
                break;
            case "year":
                movie.setYear(isInteger(chars) ? Integer.parseInt(chars) : -1);
                break;
        }
    }

    private void setGenre() {

        String[] genreArray = chars.toLowerCase().split(" ");

        for (String genre : genreArray) {

            switch (genre.trim()) {
                case "susp":
                    movie.addGenreNames("Thriller");
                    break;
                case "cnr":
                    movie.addGenreNames("Cop and Robbers");
                    break;
                case "dram":
                case "dram>":
                case "drama":
                    movie.addGenreNames("Drama");
                    break;
                case "west":
                    movie.addGenreNames("Western");
                    break;
                case "myst":
                    movie.addGenreNames("Mystery");
                    break;
                case "hor":
                case "horr":
                    movie.addGenreNames("Horror");
                    break;
                case "s.f.":
                case "scfi":
                case "scif":
                case "sxfi":
                    movie.addGenreNames("Science Fiction");
                    break;
                case "advt":
                    movie.addGenreNames("Adventure");
                    break;
                case "romt":
                case "romt.":
                    movie.addGenreNames("Romantic");
                    break;
                case "romtadvt":
                    movie.addGenreNames("Romantic");
                    movie.addGenreNames("Adventure");
                    break;
                case "comd":
                    movie.addGenreNames("Comedy");
                    break;
                case "faml":
                    movie.addGenreNames("Family");
                    break;
                case "musc":
                case "muscl":
                case "muusc":
                case "musical":
                    movie.addGenreNames("Musical");
                    break;
                case "docu":
                    movie.addGenreNames("Documentary");
                    break;
                case "crim":
                    movie.addGenreNames("Crime");
                    break;
                case "hist":
                    movie.addGenreNames("History");
                    break;
                case "porn":
                    movie.addGenreNames("Pornography");
                    break;
                case "noir":
                    movie.addGenreNames("Black");
                    break;
                case "road":
                    movie.addGenreNames("Road");
                case "bio":
                case "biop":
                    movie.addGenreNames("Biographical Picture");
                    break;
                case "tv":
                    movie.addGenreNames("TV Show");
                    break;
                case "tvs":
                    movie.addGenreNames("TV Series");
                    break;
                case "tvm":
                case "tvmini":
                    movie.addGenreNames("TV Miniseries");
                    break;
                case "cart":
                    movie.addGenreNames("Cartoon");
                    break;
                case "actn":
                    movie.addGenreNames("Action");
                    break;
                case "fant":
                    movie.addGenreNames("Fantasy");
                    break;
                case "anti-dram":
                    movie.addGenreNames("Anti-Drama");
                    break;
                case "surr":
                case "surl":
                case "surreal":
                    movie.addGenreNames("Surreal");
                    break;
                case "sports":
                    movie.addGenreNames("Sports");
                    break;
                case "epic":
                    movie.addGenreNames("Epic");
                    break;
                case "avant":
                case "avga":
                    movie.addGenreNames("Avant Grade");
                    break;
                case "camp":
                    movie.addGenreNames("Camp");
                    break;
                case "disa":
                    movie.addGenreNames("Disaster");
                    break;
                case "art":
                    movie.addGenreNames("Art");
                    break;
                case "sati":
                    movie.addGenreNames("Satire");
                    break;
                case "viol":
                    movie.addGenreNames("Violence");
                    break;
                case "ctxx":
                case "":
                    movie.addGenreNames("Unknown");
                    break;
                default:
                    movie.addGenreNames(chars.toUpperCase());
            }

            for (String aGenre : movie.getGenreNames()) {
                if (!storedGenreSet.contains(aGenre)) {
                    parsedGenreSet.add(aGenre);
                }
            }
        }
    }

    private void addMovieBatch() {

        String getNextMovieIDQuery =
                "CALL GET_NEXT_MOVIE_ID(?);";
        String insertMovieQuery =
                "INSERT INTO movies (id, title, year, director)\n" +
                        "VALUES (?, ?, NULLIF(?, -1), ?)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                 CallableStatement callableStatement = connection.prepareCall(getNextMovieIDQuery);
                 PreparedStatement preparedStatement = connection.prepareStatement(insertMovieQuery)) {

                connection.setAutoCommit(false);
                callableStatement.registerOutParameter(1, Types.VARCHAR);
                callableStatement.executeUpdate();
                String nextId = callableStatement.getString(1);
                int nextNumber = Integer.parseInt(nextId.substring(2));

                for (Movie movie : newMovieSet) {

                    movie.setID("tt" + nextNumber);
                    preparedStatement.setString(1, "tt" + nextNumber);
                    preparedStatement.setString(2, movie.getTitle());
                    preparedStatement.setInt(3, movie.getYear());
                    preparedStatement.setString(4, movie.getDirector());
                    ++nextNumber;

                    preparedStatement.addBatch();
                }
                int[] batchResults = preparedStatement.executeBatch();
                connection.commit();

                parsedMovieSet.addAll(newMovieSet);
                newMovieSet.clear();

                int successCount = Arrays.stream(batchResults).filter(statusValue -> statusValue == 1).sum();

                totalInsertCount += successCount;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addGenreBatch() {

        String insertGenreQuery =
                "INSERT INTO genres (name)\n" +
                        "VALUES (?)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                 PreparedStatement preparedStatement = connection.prepareStatement(insertGenreQuery)) {

                connection.setAutoCommit(false);

                for (String genre : parsedGenreSet) {

//                    System.out.print(genre + " ");
                    preparedStatement.setString(1, genre);
                    preparedStatement.addBatch();
                }
//                System.out.println();
                int[] batchResults = preparedStatement.executeBatch();
                connection.commit();

                newGenreCount = Arrays.stream(batchResults).filter(statusValue -> statusValue == 1).sum();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addGenreInMovieBatch() {

        String insertGenreMovieQuery =
                "INSERT INTO genres_in_movies (genreId, movieId)\n" +
                        "VALUES ((SELECT g.id FROM genres g WHERE g.name = ? LIMIT 1), ?);";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                 PreparedStatement preparedStatement = connection.prepareStatement(insertGenreMovieQuery)) {

                connection.setAutoCommit(false);

                for (Movie movie : parsedMovieSet) {
                    for (String genre : movie.getGenreNames()) {

                        preparedStatement.setString(1, genre);
                        preparedStatement.setString(2, movie.getID());

                        preparedStatement.addBatch();
                    }
                }

                int[] batchResults = preparedStatement.executeBatch();
                connection.commit();

                int successCount = Arrays.stream(batchResults).filter(statusValue -> statusValue == 1).sum();

                System.out.println("Successfully linked " + successCount + " genres to movies.");

            }
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean isInteger(Object object) {
        try {
            Integer.parseInt((String) object);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {

        MovieSAXParser movieSAXParser = new MovieSAXParser();

        movieSAXParser.parseMovie();
    }
}