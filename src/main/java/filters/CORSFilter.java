package filters;

import ninja.*;

public class CORSFilter  implements Filter {
    @Override
    public Result filter(FilterChain filterChain, Context context) {
        Result result;

        if( context.getMethod().equalsIgnoreCase("OPTIONS") ) {
            result = Results.text();
        } else {
            result = filterChain.next(context);
        }
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
