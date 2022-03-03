package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.errors.InternalServerErrorException;
import ca.lavers.joa.core.errors.NotFoundException;

import java.io.IOException;

public class UpdateHandler<T> extends BaseHandler<T> {

    private final Updatable<T> updatable;

    public UpdateHandler(Class<T> itemClass, Updatable<T> updatable) {
        super(itemClass);
        this.updatable = updatable;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        try {
            String id = getItemId(ctx);
            T item = getRequestBody(ctx);
            T updated = updatable.update(ctx, id, item);
            if(updated == null) {
                throw new NotFoundException();
            }
            ctx.response().body(updated);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
