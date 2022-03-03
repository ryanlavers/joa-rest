package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.errors.InternalServerErrorException;

import java.io.IOException;

public class CreateHandler<T> extends BaseHandler<T> {

    private final Creatable<T> creatable;
    private boolean defer = false;

    public CreateHandler(Class<T> itemClass, Creatable<T> creatable) {
        super(itemClass);
        this.creatable = creatable;
    }

    public CreateHandler<T> respondAccepted() {
        this.defer = true;
        return this;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        try {
            T item = getRequestBody(ctx);
            T created = creatable.create(ctx, item);
            if(defer) {
                ctx.response().body(created == null ? "Accepted" : created);    // TODO
                ctx.response().status(202);
            }
            else {
                ctx.response().body(created);
                ctx.response().status(201);
            }
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
