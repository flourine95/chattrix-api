import org.mindrot.jbcrypt.BCrypt;

public class HashPass {
    public static void main(String[] args) {
        System.out.println(hashPassword("password"));
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }
}