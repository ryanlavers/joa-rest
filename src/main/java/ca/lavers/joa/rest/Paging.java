package ca.lavers.joa.rest;

public class Paging {
    private final int page;
    private final int pageSize;

    public Paging(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }
}
