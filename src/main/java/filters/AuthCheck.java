package filters;

import controllers.JSendResp;
import ninja.*;
import ninja.cache.NinjaCache;
import tools.TokenAuthority;

import javax.inject.Inject;

/**
 * Created by ganitzsh on 1/27/17.
 */
public class AuthCheck implements Filter {

    @Inject
    NinjaCache ninjaCache;

    @Override
    public Result filter(FilterChain filterChain, Context context) {
        Result result;

        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results
                    .unauthorized()
                    .json()
                    .render(new JSendResp(
                            401,
                            new Exception("Not authorized, are you logged in?")
                    ));
        }
        return filterChain.next(context);
    }
}
