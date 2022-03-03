package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.errors.InternalServerErrorException;
import ca.lavers.joa.core.errors.NotFoundException;

import java.io.IOException;

public class GetHandler<T> extends BaseHandler<T> {

    private final Gettable<T> gettable;

    public GetHandler(Class<T> itemClass, Gettable<T> gettable) {
        super(itemClass);
        this.gettable = gettable;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        String id = getItemId(ctx);
        T item = gettable.get(ctx, id);

        if(item == null) {
            throw new NotFoundException();
        }
        try {
            ctx.response().body(item);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
