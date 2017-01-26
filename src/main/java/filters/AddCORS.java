package filters;

import ninja.*;

/**
 * Created by ganitzsh on 1/26/17.
 */
public class AddCORS implements Filter {
    @Override
    public Result filter(FilterChain filterChain, Context context) {
        Result result;

        result = filterChain.next(context);
        result.addHeader("Access-Control-Allow-Credentials", "true");
        result.addHeader("Access-Control-Allow-Origin", context.getHeader("Origin"));
        result.addHeader("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, PATCH, OPTIONS");
        result.addHeader("Access-Control-Max-Age", "50");
        result.addHeader("Access-Control-Allow-Headers", "Origin, Authorization, Cookie, Accept, Content-Type, X-Requested-With, Key");
        result.addHeader("Cache-Control", "");
        result.addHeader("Vary", "Origin");
        return result;
    }
}
