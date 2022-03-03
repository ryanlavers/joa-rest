package ca.lavers.joa.rest;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.errors.BadRequestException;
import ca.lavers.joa.core.errors.InternalServerErrorException;
import ca.lavers.jstatemachine.StateMachineException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListHandler<T> implements Middleware {

    public static final int DEFAULT_PAGE_SIZE = 10;
    private int pageSize = DEFAULT_PAGE_SIZE;

    private final Listable<T> listable;

    public ListHandler(Listable<T> listable) {
        this.listable = listable;
    }

    public ListHandler<T> defaultPageSize(int size) {
        this.pageSize = size;
        return this;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {

        Paging requestedPaging = getRequestedPaging(ctx);
        Paging paging;

        if(requestedPaging != null) {
            if(!listable.supportsPaging()) {
                throw new BadRequestException("This collection does not support paging");
            }
            paging = requestedPaging;
        }
        else {
            paging = new Paging(0, pageSize);   // TODO -- set to null if paging not supported, to be consistent with sorting and filtering
        }

        Sorting sorting = null;
        try {
            sorting = getRequestedSorting(ctx);
        } catch (StateMachineException e) {
            throw new BadRequestException("Error parsing sorting request: " + e.getMessage());
        }
        if(sorting != null && !listable.supportsSorting()) {
            throw new BadRequestException("This collection does not support sorting");
        }

        Filtering filtering = null;
        try {
            filtering = getRequestedFiltering(ctx);
        } catch (StateMachineException e) {
            throw new BadRequestException("Error parsing filter: " + e.getMessage());
        }
        if(filtering != null && !listable.supportsFiltering()) {
            throw new BadRequestException("This collection does not support filtering");
        }

        ListResult<T> result = listable.list(ctx, paging, filtering, sorting);

        List<T> items = result.getItems().collect(Collectors.toList());
        result.getItems().close();

        // TODO - warn if collection returns more than pageSize items, or if totalItems makes no sense

        CollectionPage<T> response = listable.supportsPaging()
                ? new CollectionPage<T>(paging.getPage(), paging.getPageSize(), items, result.getTotalItems())
                : new CollectionPage<>(items);

        try {
            ctx.response().body(response);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private Paging getRequestedPaging(Context ctx) {
        // TODO - Wrap in a convenience accessor object. Maybe goes in core?
        final Map<String, String> params = ctx.request().queryParams();

        // TODO - Validate page and pageSize aren't stupid

        boolean requestedPaging = false;
        int page = 0;
        if(params.containsKey("page")) {
            try {
                page = Integer.parseInt(params.get("page"));
                requestedPaging = true;
            } catch (NumberFormatException ignored) { }
        }

        int pageSize = this.pageSize;
        if(params.containsKey("pageSize")) {
            try {
                pageSize = Integer.parseInt(params.get("pageSize"));
                requestedPaging = true;
            } catch (NumberFormatException ignored) { }
        }

        if(requestedPaging) {
            return new Paging(page, pageSize);
        }

        return null;
    }

    private Sorting getRequestedSorting(Context ctx) {
        final Map<String, String> params = ctx.request().queryParams();
        final String sortspec = params.get("sortBy");
        if(sortspec != null) {
            return SortParser.parse(sortspec);
        }
        return null;
    }

    private Filtering getRequestedFiltering(Context ctx) {
        final Map<String, String> params = ctx.request().queryParams();
        final String filterspec = params.get("filter");
        if(filterspec != null) {
            return FilterParser.parse(filterspec);
        }
        return null;
    }
}
