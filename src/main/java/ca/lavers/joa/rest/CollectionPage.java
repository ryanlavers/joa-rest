package ca.lavers.joa.rest;

import java.util.List;

public class CollectionPage<T> {

    private Integer page;
    private Integer pageSize;
    private final List<T> items;
    private Integer totalItems = null;

    public CollectionPage(List<T> items) {
        this.items = items;
    }

    public CollectionPage(int page, int pageSize, List<T> items) {
        this.page = page;
        this.pageSize = pageSize;
        this.items = items;
    }

    public CollectionPage(int page, int pageSize, List<T> items, Integer totalItems) {
        this.page = page;
        this.pageSize = pageSize;
        this.items = items;
        this.totalItems = totalItems;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public List<T> getItems() {
        return items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }
}
