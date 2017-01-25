package tools;

import java.math.BigInteger;
import java.security.SecureRandom;

public class SessionIdentifierGenerator {
    public static String nextSessionId() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

}
