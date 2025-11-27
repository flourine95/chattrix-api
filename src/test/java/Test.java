import org.mindrot.jbcrypt.BCrypt;

public class Test {
    public static void main(String[] args) {
        String input = "test";

        String hashed = BCrypt.hashpw(input, BCrypt.gensalt());
        System.out.println(hashed);
    }
}
