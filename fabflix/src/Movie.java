import java.util.Set;
import java.util.TreeSet;

public class Movie {

    private String id;
    private String title;
    private int year;
    private String director;
    private final Set<String> genreNames;

    public Movie() {

        this.id = "";
        this.title = "";
        this.year = -1;
        this.director = "";
        this.genreNames = new TreeSet<>();
    }

    public Movie(String aTitle, int aYear, String aDirector) {

        this.title = aTitle;
        this.year = aYear;
        this.director = aDirector;
        genreNames = new TreeSet<>();
    }

    public void setID(String aID) {
        this.id = aID;
    }

    public void setTitle(String aTitle) {
        this.title = aTitle;
    }


    public void setYear(int aYear) {
        this.year = aYear;
    }
    public void setDirector(String aDirector) {
        this.director = aDirector;
    }

    public void addGenreNames(String aGenre) {
        this.genreNames.add(aGenre);
    }

    public String getID() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public int getYear() {
        return this.year;
    }

    public String getDirector() {
        return this.director;
    }

    public String[] getGenreNames() {
        return this.genreNames.toArray(String[]::new);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Movie)) {
            return false;
        }
        Movie otherMovie = (Movie) obj;
        return this.title.equals(otherMovie.title)
                && this.year == otherMovie.year
                && this.director.equals(otherMovie.director);
    }

    @Override
    public String toString() {
        return "Movie title: " + this.title +
                ", Movie year: " + this.year +
                ", Movie director: " + this.director;
    }
}
