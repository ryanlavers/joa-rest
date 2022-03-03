package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.errors.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class BaseHandler<T> implements Middleware {

    private static final Logger log = LoggerFactory.getLogger(BaseHandler.class);

    private final Class<T> itemClass;

    public BaseHandler(Class<T> itemClass) {
        this.itemClass = itemClass;
    }

    private String requestBodyCtxNamespace;
    private String requestBodyCtxAttribute;

    protected String getItemId(Context ctx) {
        return ctx.get(RestRouter.NS, RestRouter.ITEM_ID, String.class)
                .orElseThrow(() -> new IllegalStateException("RestRouter itemID value not found in Context attributes"));
    }

    protected void setRequestBodyLocation(String ns, String attribute) {
        this.requestBodyCtxNamespace = ns;
        this.requestBodyCtxAttribute = attribute;
    }

    protected T getRequestBody(Context ctx) {
        if(requestBodyCtxNamespace != null && requestBodyCtxAttribute != null) {
            return ctx.get(requestBodyCtxNamespace, requestBodyCtxAttribute, itemClass)
                    .orElseThrow(() -> new IllegalStateException("Request body not found in Context attributes"));
        }

        try {
            return ctx.request().parseBody(itemClass);
        } catch (IOException e) {
            log.debug("Error deserializing request body", e);
            throw new BadRequestException();
        }
    }
}
