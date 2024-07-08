public class User {
    private final String username;
    private final int customerId;

    public User(String anUsername, int aCustomerId) {
        this.username = anUsername;
        this.customerId = aCustomerId;
    }

    public String getUsername() {
        return this.username;
    }

    public int getCustomerId() {
        return this.customerId;
    }
}
