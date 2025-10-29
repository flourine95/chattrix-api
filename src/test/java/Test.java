import org.mindrot.jbcrypt.BCrypt;

public class Test {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("test", BCrypt.gensalt()));
    }
}
