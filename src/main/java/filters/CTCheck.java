package filters;

import controllers.JSendResp;
import ninja.*;
import ninja.cache.NinjaCache;
import tools.TokenAuthority;

import javax.inject.Inject;

/**
 * Created by ganitzsh on 1/27/17.
 */
public class CTCheck implements Filter {


    @Override
    public Result filter(FilterChain filterChain, Context context) {
        Result result;

        if (!context.isRequestJson()) {
            return Results.unauthorized().json().render(new JSendResp(401, new Exception("Check your Content-Type, only application/json or application/xml allowed")));
        }
        return filterChain.next(context);
    }
}
