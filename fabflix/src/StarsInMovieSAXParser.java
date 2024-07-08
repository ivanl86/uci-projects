import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

public class StarsInMovieSAXParser extends DefaultHandler {

    private final int MAX_BATCH_SIZE = 2500;

    private final String loginUser = "mytestuser";
    private final String loginPasswd = "mypassword";
    private final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

    private String director;
    private String chars;
    private Cast cast;
    private final Map<String, String> storedMovieMap;
    private final Map<String, String> storeStarMap;
    private final Set<Cast> parsedCastSet;
    private final Set<String> starMovieSet;

    private int totalParseCount;
    private int totalInsertCount;
    private int totalEmptyCastCount;
    private int totalEmptyTitleCount;

    public StarsInMovieSAXParser() {

        director = "";
        parsedCastSet = new HashSet<>();
        storedMovieMap = new HashMap<>();
        storeStarMap = new HashMap<>();
        starMovieSet = new HashSet<>();
        totalParseCount = 0;
        totalInsertCount = 0;
        totalEmptyCastCount = 0;
        totalEmptyTitleCount = 0;

        final String getAllMovieQuery =
                "SELECT m.id AS m_id,\n" +
                        "m.title AS m_title,\n" +
                        "m.director AS m_director\n" +
                        "FROM movies m";
        final String getAllStarQuery =
                "SELECT s.id AS s_id,\n" +
                        "s.name AS s_name\n" +
                        "FROM stars s";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(getAllMovieQuery);
                     ResultSet resultSet = preparedStatement.executeQuery()) {

                    while (resultSet.next()) {
                        storedMovieMap.put(
                                resultSet.getString("m_title"),
                                resultSet.getString("m_id")
                        );
                    }
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement(getAllStarQuery);
                     ResultSet resultSet = preparedStatement.executeQuery()) {

                    while (resultSet.next()) {
                        storeStarMap.put(
                                resultSet.getString("s_name"),
                                resultSet.getString("s_id")
                        );
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void parseCast() {

        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

            SAXParser saxParser = saxParserFactory.newSAXParser();

            System.out.println("Start parsing...");
            saxParser.parse("stanford-movies/casts124.xml", this);
//            saxParser.parse("stanford-movies/casts999.xml", this);
            if (!parsedCastSet.isEmpty()) {
                System.out.println("Start adding last batch...");
                addCastBatch();
                System.out.println("Finished adding last batch...");
            }
            System.out.println("Finished parsing...");
            System.out.println("Total parsed casts: " + totalParseCount);
            System.out.println("Total inserted casts: " + totalInsertCount);
            System.out.println("Total empty casts: " + totalEmptyCastCount);
            System.out.println("Total movies without title: " + totalEmptyTitleCount);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        if (qName.equalsIgnoreCase("dirfilms")) {
            cast = new Cast();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        chars = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {

        if (qName.equalsIgnoreCase("is") &&
                !(chars.trim().toLowerCase().contains("unyear") || chars.trim().toLowerCase().contains("unknown"))) {
            director = chars.trim();
        } else if (qName.equalsIgnoreCase("filmc")) {

            cast.setMovieDirector(director);

            if (cast.getCastSize() > 0) {
                parsedCastSet.add(cast);
                ++totalParseCount;
            } else {
                System.out.println("Rejected cast: " + cast);
                ++totalEmptyCastCount;
            }
            director = "";
            cast = new Cast();
        }

        if (parsedCastSet.size() >= MAX_BATCH_SIZE) {
            addCastBatch();
        }

        switch (qName.toLowerCase()) {
            case "t":
                cast.setMovieTitle(chars.trim());
                break;
            case "a":
                if (!chars.trim().equalsIgnoreCase("s a")) {
                    cast.addStarName(chars.trim());
                }
                break;
        }
    }

    public void addCastBatch() {

        final String addStarIntoMovieQuery =
                "INSERT INTO stars_in_movies (starId, movieId)\n" +
                        "VALUES (?, ?)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(addStarIntoMovieQuery)) {

                    connection.setAutoCommit(false);

                    for (Cast cast : parsedCastSet) {
                        String movieId = storedMovieMap.get(
                                cast.getMovieTitle()
                        );

                        if (movieId != null) {
                            for (String starName : cast.getStarName()) {
                                String starId = storeStarMap.get(starName);

                                if (starId != null) {

                                    starMovieSet.add(starId + movieId);
                                    preparedStatement.setString(1, starId);
                                    preparedStatement.setString(2, movieId);
                                    preparedStatement.addBatch();
                                }
                            }
                        } else {
                            ++totalEmptyTitleCount;
                        }

                    }
                    int[] batchResults = preparedStatement.executeBatch();
                    connection.commit();

                    parsedCastSet.clear();

                    totalInsertCount = Arrays.stream(batchResults).filter(statusValue -> statusValue == 1).sum();
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

    public static void main(String[] args) {

        StarsInMovieSAXParser starsInMovieSAXParser = new StarsInMovieSAXParser();

        starsInMovieSAXParser.parseCast();
    }
}
