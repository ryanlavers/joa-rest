package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;

public interface Listable<T> {
    ListResult<T> list(Context ctx, Paging page, Filtering filter, Sorting sort);
    default boolean supportsPaging() {
        return false;
    }
    default boolean supportsFiltering() {
        return false;
    }
    default boolean supportsSorting() {
        return false;
    }
}
