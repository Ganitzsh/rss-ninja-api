package tools;

import ninja.cache.NinjaCache;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by ganitzsh on 1/24/17.
 */
@Singleton
public class TokenAuthority {
    public static Boolean isValid(String token, NinjaCache ninjaCache) {
        if (token == null) {
            return false;
        }
        if (ninjaCache.get(token) == null) {
            return false;
        }
        return true;
    }
}
