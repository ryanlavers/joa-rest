package ca.lavers.joa.rest;

import java.util.stream.Stream;

public class ListResult<T> {

    private final Stream<T> items;
    private Integer totalItems = null;

    public ListResult(Stream<T> items) {
        this.items = items;
    }

    public ListResult(Stream<T> items, Integer totalItems) {
        this.items = items;
        this.totalItems = totalItems;
    }

    public Stream<T> getItems() {
        return items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }
}
