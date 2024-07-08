import org.jasypt.util.password.StrongPasswordEncryptor;

public class EncryptedPasswordVerifyUtils {

    private EncryptedPasswordVerifyUtils() {
    }

    public static boolean verifyPassword(String password, String encryptedPassword) {

        return new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
    }
}
