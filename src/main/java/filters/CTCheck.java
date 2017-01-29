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
        if (!context.isRequestJson()) {
            return Results.unauthorized().json().render(new JSendResp(401, new SimpleError("Check your Content-Type, only application/json or application/xml allowed")));
        }
        return filterChain.next(context);
    }

    public class SimpleError {
        public String message;

        public SimpleError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
