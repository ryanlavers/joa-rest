package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;

public interface Gettable<T> {
    T get(Context ctx, String id);
}
