package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;

public interface Creatable<T> {
    T create(Context ctx, T item);
}
