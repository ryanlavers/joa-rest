package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;

public interface Deletable<T> {
    boolean delete(Context ctx, String id);
}
