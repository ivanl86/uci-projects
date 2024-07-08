public class Employee {

    private String email;
    private String fullName;

    public Employee(String anEmail, String aFullName) {

        this.email = anEmail;
        this.fullName = aFullName;
    }

    public String getEmail() {

        return this.email;
    }

    public String getFullName() {

        return this.fullName;
    }
}
