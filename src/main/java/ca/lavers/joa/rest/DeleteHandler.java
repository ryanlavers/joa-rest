package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.errors.NotFoundException;

public class DeleteHandler<T> extends BaseHandler<T> {

    private final Deletable<T> deletable;

    public DeleteHandler(Class<T> itemClass, Deletable<T> deletable) {
        super(itemClass);
        this.deletable = deletable;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        String id = getItemId(ctx);
        boolean result = deletable.delete(ctx, id);
        if(result) {
            ctx.response().status(200);
        }
        else {
            throw new NotFoundException();
        }
    }
}
