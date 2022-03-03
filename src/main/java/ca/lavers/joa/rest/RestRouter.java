package ca.lavers.joa.rest;

import ca.lavers.joa.core.*;
import ca.lavers.joa.core.errors.MethodNotAllowedException;
import ca.lavers.joa.core.errors.NotFoundException;
import ca.lavers.joa.core.util.WrappedRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RestRouter implements Middleware {

    public static final String NS = "ca.lavers.joa.rest.RestRouter";
    public static final String ITEM_ID = "itemID";
    public static final String PARENT_IDS = "parentIDs";

    // TODO - chains null by default with MethodNotAllowed if they weren't configured?
    private MiddlewareChain getChain;
    private MiddlewareChain listChain;
    private MiddlewareChain createChain;
    private MiddlewareChain updateChain;
    private MiddlewareChain deleteChain;

    private final Map<String, SubResourceChain> subResources = new HashMap<>();

    public RestRouter get(Middleware... middlewares) {
        if(getChain == null) {
           getChain = new MiddlewareChain();
        }
        getChain.append(middlewares);
        return this;
    }

    public RestRouter list(Middleware... middlewares) {
        if(listChain == null) {
            listChain = new MiddlewareChain();
        }
        listChain.append(middlewares);
        return this;
    }

    public RestRouter create(Middleware... middlewares) {
        if(createChain == null) {
            createChain = new MiddlewareChain();
        }
        createChain.append(middlewares);
        return this;
    }

    public RestRouter update(Middleware... middlewares) {
        if(updateChain == null) {
            updateChain = new MiddlewareChain();
        }
        updateChain.append(middlewares);
        return this;
    }

    public RestRouter delete(Middleware... middlewares) {
        if(deleteChain == null) {
            deleteChain = new MiddlewareChain();
        }
        deleteChain.append(middlewares);
        return this;
    }

    public RestRouter subResource(String name, String parentIDName, Middleware... middlewares) {
        this.subResources.put(name, new SubResourceChain(parentIDName, middlewares));
        return this;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        PathParser path = new PathParser(ctx.request().path());

        if(path.isCollectionRequest()) {
            switch(ctx.request().method()) {
                case "GET":
                    if(listChain == null) throw new MethodNotAllowedException();
                    listChain.call(ctx);
                   return;
                case "POST":
                    if(createChain == null) throw new MethodNotAllowedException();
                    createChain.call(ctx);
                    return;
            }
        }
        else if(path.isItemRequest()) {
            ctx.put(NS, ITEM_ID, path.getItemID());
            switch (ctx.request().method()) {
                case "GET":
                    if(getChain == null) throw new MethodNotAllowedException();
                    getChain.call(ctx);
                    return;
                case "PUT":
                    if(updateChain == null) throw new MethodNotAllowedException();
                    updateChain.call(ctx);
                    return;
                case "DELETE":
                    if(deleteChain == null) throw new MethodNotAllowedException();
                    deleteChain.call(ctx);
                    return;
            }
        }
        else if(path.isSubResourceRequest()) {
           SubResourceChain chain = subResources.get(path.getSubResourceName());
            if(chain == null) {
                throw new NotFoundException();
            }

            Map<String, String> parentIDs;
            Optional<Map> maybe = ctx.get(NS, PARENT_IDS, Map.class);
            if(maybe.isPresent()) {
                parentIDs = (Map<String, String>) maybe.get();
            }
            else {
                parentIDs = new HashMap<>();
                ctx.put(NS, PARENT_IDS, parentIDs);
            }
            parentIDs.put(chain.parentName, path.getItemID());

            // Chop off parent ID from path
            Context newCtx = ctx.withAlternateRequest(new SubRequest(ctx.request(), path.getRemainingPath()));
            chain.chain.call(newCtx);

            return;
        }

        // Everything else
        throw new MethodNotAllowedException();
    }

    public static String getItemId(Context ctx) {
        return ctx.get(RestRouter.NS, RestRouter.ITEM_ID, String.class).orElse(null);
    }

    public static String getParentId(Context ctx, String name) {
        Optional<Map> o = ctx.get(RestRouter.NS, RestRouter.PARENT_IDS, Map.class);
        if(o.isPresent()) {
            return (String) o.get().get(name);
        }
        return null;
    }
}

class SubResourceChain {
    public String parentName;
    public MiddlewareChain chain;

    public SubResourceChain(String parentName, Middleware... chain) {
        this.parentName = parentName;
        this.chain = new MiddlewareChain(chain);
    }
}

class SubRequest extends WrappedRequest {
    private final String newPath;

    public SubRequest(Request wrapped, String newPath) {
        super(wrapped);
        this.newPath = newPath;
    }

    @Override
    public String path() {
        return newPath;
    }
}
