import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class CastSAXParser extends DefaultHandler {
    private final int MAX_BATCH_SIZE = 500;

    private String chars;
    private Cast currentCast;
    private final Set<Cast> storedCastSet;
    private final Set<Cast> parsedCastSet;
    private int totalParseCount;
    private int totalInsertCount;

    public CastSAXParser() {
        storedCastSet = new HashSet<>();
        parsedCastSet = new HashSet<>();
        totalParseCount = 0;
        totalInsertCount = 0;

        final String loginUser = "mytestuser";
        final String loginPasswd = "mypassword";
        final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        final String getAllCastQuery = "SELECT starId AS s_id, movieId AS m_id FROM stars_in_movies;";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                 PreparedStatement preparedStatement = connection.prepareStatement(getAllCastQuery);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
//                while (resultSet.next()) {
//                    storedCastSet.add(
//                            new Cast(
//                                    resultSet.getString("s_id"),
//                                    resultSet.getString("m_id")));
//                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void parseCasts() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            System.out.println("Start parsing casts...");
            saxParser.parse("stanford-movies/casts124.xml", this);
            System.out.println("Finished parsing casts...");

            if (!parsedCastSet.isEmpty()) {
                System.out.println("Start adding last batch of star-movie links...");
                addStarToMovieBatch();
                System.out.println("Finish adding last batch...");
            }
            System.out.println("Finish adding stars in movies...");

            System.out.println("Total cast links parsed: " + totalParseCount);
            System.out.println("Total cast links inserted: " + totalInsertCount);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        if (qName.equalsIgnoreCase("casts")) {
            currentCast = new Cast();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        chars = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("m")) {
//            if (currentCast.getFilmId() != null && currentCast.getStarId() != null && !storedCastSet.contains(currentCast)) {
//                parsedCastSet.add(currentCast);
//                ++totalParseCount;
//            }
        }

        if (parsedCastSet.size() >= MAX_BATCH_SIZE) {
            System.out.println("Start adding star-movie links in batch...");
            addStarToMovieBatch();
            System.out.println("Finish adding star-movie links in batch...");
        }

        if(qName.equalsIgnoreCase("a")) {
            String starId = resolveStarId(chars);
//            currentCast.setStarId(starId);
        } else if (qName.equalsIgnoreCase("f")) {
//            currentCast.setFilmId(chars);
        }
    }

    private String resolveStarId(String actorName) {
        final String loginUser = "mytestuser";
        final String loginPasswd = "mypassword";
        final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        String starId = null;
        String query = "SELECT id FROM stars WHERE name = ?";

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, actorName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    starId = resultSet.getString("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error resolving star ID for actor: " + actorName);
            e.printStackTrace();
        }
        return starId;
    }


    private void addStarToMovieBatch() {
        final String loginUser = "mytestuser";
        final String loginPasswd = "mypassword";
        final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
        String procedureCall = "{CALL ADD_STAR_TO_MOVIE(?, ?)}";

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
             CallableStatement callableStatement = connection.prepareCall(procedureCall)) {

            connection.setAutoCommit(false);
            for (Cast cast : parsedCastSet) {
//                callableStatement.setString(1, cast.getStarId());
//                callableStatement.setString(2, cast.getFilmId());
                callableStatement.addBatch();
            }

            int[] batchResults = callableStatement.executeBatch();
            connection.commit();

            parsedCastSet.clear();
            totalInsertCount += Arrays.stream(batchResults).filter(statusValue -> statusValue >= 0).sum();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        CastSAXParser fabflixCastSAXParser = new CastSAXParser();

        fabflixCastSAXParser.parseCasts();
    }
}
