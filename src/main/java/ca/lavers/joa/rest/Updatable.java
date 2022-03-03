package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;

public interface Updatable<T> {
    T update(Context ctx, String id, T item);
}
