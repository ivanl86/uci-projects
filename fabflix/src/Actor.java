public class Actor {

    private String name;
    private int birthYear;

    public Actor() {
    }

    public Actor(String name, int birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Actor)) {
            return false;
        }
        Actor otherActor = (Actor) obj;
        return this.name.equals(otherActor.name);
    }

    @Override
    public String toString() {
        return name + "|" + (birthYear != -1 ? birthYear : "unknown");
    }
}