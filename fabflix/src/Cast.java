import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Cast {

    private final Movie movie;
    private final List<String> starNames;

    public Cast() {
        movie = new Movie();
        starNames = new ArrayList<>();
    }

    public void setMovieTitle(String aTitle) {
        this.movie.setTitle(aTitle);
    }

    public void setMovieDirector(String aDirector) {
        this.movie.setDirector(aDirector);
    }

    public String getMovieTitle() {
        return this.movie.getTitle();
    }

    public String getMovieDirector() {
        return this.movie.getDirector();
    }

    public void addStarName(String aStar) {
        this.starNames.add(aStar);
    }

    public String[] getStarName() {
        return this.starNames.toArray(String[]::new);
    }

    public int getCastSize() {
        return this.starNames.size();
    }

    @Override
    public String toString() {
        return "Movie title: " + this.movie.getTitle() +
                ", Movie director: " + this.movie.getDirector() + " " +
                "Casts: " + Arrays.toString(this.getStarName());
    }
}
