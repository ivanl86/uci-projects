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

public class StarSAXParser extends DefaultHandler {

    private final int MAX_BATCH_SIZE = 2500;

    private String chars;
    private Actor currentActor;
    private final Set<Actor> parsedActorSet;
    private final Set<Actor> storedActorSet;
    private int totalParseCount;
    private int totalInsertCount;
    private int totalRejectedCount;

    public StarSAXParser() {
        parsedActorSet = new HashSet<>();
        storedActorSet = new HashSet<>();
        totalParseCount = 0;
        totalInsertCount = 0;
        totalRejectedCount = 0;

        final String loginUser = "mytestuser";
        final String loginPasswd = "mypassword";
        final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        final String getAllActorQuery = "SELECT name AS s_name, birthYear AS s_year FROM stars;";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                 PreparedStatement preparedStatement = connection.prepareStatement(getAllActorQuery);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    storedActorSet.add(
                            new Actor(
                                    resultSet.getString("s_name"),
                                    resultSet.getInt("s_year")));
                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void parseActors() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            System.out.println("Start parsing...");
            saxParser.parse("stanford-movies/actors63.xml", this);

            if (!parsedActorSet.isEmpty()) {
                System.out.println("Start adding last batch...");
                addActorBatch();
                System.out.println("Finish adding last batch...");
            }
            System.out.println("Finished parsing...");

            System.out.println("Total actors parsed: " + totalParseCount);
            System.out.println("Total actors inserted: " + totalInsertCount);
            System.out.println("Total rejected actors: " + totalRejectedCount);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        if (qName.equalsIgnoreCase("actor")) {
            currentActor = new Actor();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        chars = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {

        if (qName.equalsIgnoreCase("actor")) {

            if (currentActor.getName() != null && !storedActorSet.contains(currentActor)) {
                parsedActorSet.add(currentActor);
                ++totalParseCount;
            } else {
                System.out.println("Rejected star: " + currentActor);
                ++totalRejectedCount;
            }
        }

        if (parsedActorSet.size() >= MAX_BATCH_SIZE) {
            addActorBatch();
        }

        switch (qName.toLowerCase()) {
            case "stagename":
                currentActor.setName(chars);
                break;
            case "dob":
                currentActor.setBirthYear(isInteger(chars) ? Integer.parseInt(chars) : -1);
                break;
        }
    }


    private void addActorBatch() {
        final String loginUser = "mytestuser";
        final String loginPasswd = "mypassword";
        final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        String getNextActorIDQuery = "CALL GET_NEXT_STAR_ID(?);";
        String insertActorQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, NULLIF(?, -1));";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                 CallableStatement callableStatement = connection.prepareCall(getNextActorIDQuery);
                 PreparedStatement preparedStatement = connection.prepareStatement(insertActorQuery)) {

                connection.setAutoCommit(false);
                callableStatement.registerOutParameter(1, Types.VARCHAR);
                callableStatement.executeUpdate();
                String nextId = callableStatement.getString(1);
                int nextNumber = Integer.parseInt(nextId.substring(2));

                for (Actor actor : parsedActorSet) {

                    String actorId = "nm" + nextNumber;
                    preparedStatement.setString(1, actorId);
                    preparedStatement.setString(2, actor.getName());
                    preparedStatement.setObject(3, actor.getBirthYear(), Types.INTEGER);
                    ++nextNumber;

                    preparedStatement.addBatch();
                }
                int[] batchResults = preparedStatement.executeBatch();
                connection.commit();

                parsedActorSet.clear();

                int successCount = Arrays.stream(batchResults).filter(statusValue -> statusValue == 1).sum();

                totalInsertCount += successCount;
            }
        } catch (ClassNotFoundException | SQLException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
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

        StarSAXParser fabflixStarSAXParser = new StarSAXParser();

        fabflixStarSAXParser.parseActors();
    }
}
