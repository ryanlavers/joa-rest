# Joa Rest

RESTful collection APIs for the [Joa Framework](https://github.com/ryanlavers/joa-core/)

## RestRouter

RestRouter simply determines the type of incoming request and routes it to the appropriate middleware chain,
if provided:

```java
server.use(new RestRouter()
    .list( /* middleware */ )
    .get( /* middleware */ )
    .create( /* middleware */ )
    .update( /* middleware */ )
    .delete( /* middleware */ )
);
```

HTTP requests are mapped to the above "actions" based on the method and path:

| Method | Path    | Action Name |
|--------|---------|-------------|
| GET    | /       | list        |
| POST   | /       | create      |
| GET    | /\<id\> | get         |
| PUT    | /\<id\> | update      |
| DELETE | /\<id\> | delete      |



If the request is for a specific item (i.e. a get, update, or delete request) then the item
ID can be retrieved from the Context as follows:

```java
String resourceId = RestRouter.getItemId(ctx);
```

Fetching this value yourself is unnecessary however if you use the various Handler helper classes
described below, which among other things will provide this value directly to you as a method parameter.

## Handler Helpers

To further ease handling REST collection requests, use the `ListHandler`, `GetHandler`, `CreateHandler`,
`UpdateHandler`, and `DeleteHandler` middlewares. These take care of the nitty-gritty of the requests themselves,
leaving you to just implement your application-specific logic on domain objects:

```java
public class UserUpdater implements Updatable<User> {
    @Override
    public User update(Context ctx, String id, User updated) {
        User existing = someUserStore.loadUserByID(id);
        if(existing != null) {
            existing.updateFrom(updated);
            someUserStore.save(existing);
            // The updated existing user will be sent back as the response to the client 
            return existing;
        }
        else {
            // Indicates specified user not found, and will result in a 404 Not Found response
            return null; 
        }
    }
}

/* ... in main server setup ... */

server.use(new RestRouter()
    .update(new UpdateHandler<>(User.class, new UserUpdater()))
);
```

You might also choose to write a single class which implements all (or just some) of the required 
collection interfaces. In addition, keep in mind that RestRouter accepts plain old Middleware, so you can
insert your own such as permission checking, validation, etc. before any of these handlers:

```java
UsersCollection usersCollection = new UsersCollection();

// Using Router from joa-middleware to mount under /users, so that will be the root of our collection
router.mount("/users", new RestRouter()
    // Only need to register those handlers the /users endpoint should support
    .list(new ListHandler<>(usersCollection))
    .get(new GetHandler<>(User.class, usersCollection))
    .create(permissions.require("staff"), new CreateHandler<>(User.class, usersCollection))
    .update(permissions.require("admin"), new UpdateHandler<>(User.class, usersCollection))
    .delete(permissions.require("admin"), new DeleteHandler<>(User.class, usersCollection))
);
```

### ListHandler

The ListHandler is a bit more complex than the other handlers, so we'll go into more detail here. The
ListHandler interprets list requests (GET requests to the root path of your collection) and helps you
manage common list functionality such as paging, sorting, and filtering.

Implementing the Listable interface will look something like this:

```java
public class UserLister implements Listable<User> {
    @Override
    public ListResult<User> list(Context ctx, Paging paging, Sorting sorting, Filtering filtering) {
        return new ListResult<>(
            Stream.of(someUserStore.listUsers())
        );
    }
}
```

Don't worry about the paging, sorting, and filtering parameters for now; they are all null unless enabled
individually (covered later).

Your list method must return a ListResult object containing a Stream of the items you want to be
returned to the client. The serialized JSON response will look like this (depending, of course, on what your
item class actually looks like):

```json
{
    "items": [
        {
            "username": "alice",
            "role": "admin"
        },
        {
            "username": "bob",
            "role": "user"
        }
    ]
}
```

#### Paging

You can support paging in your collections by overriding the `Listable.supportsPaging()` method to return `true`.
Now the client can request a specific page (zero-indexed!) of results with the appropriate query parameters
(e.g. `GET /users?page=3&pageSize=20`) and these values will be provided to you as the `Paging` parameter. To save
you needing to consider the special case, if no paging was included with the request, this parameter will default 
to the first page (0) and a page size of 10.

If paging is enabled, you must ensure that the stream of results you return only contains those items that belong
on the requested page; the ListHandler will return the whole stream to the client as a single page. If you also
provide a count of the total number of items in the collection, this will be included in the response.

```java
public class UserLister implements Listable<User> {
    @Override
    public ListResult<User> list(Context ctx, Paging paging, Sorting sorting, Filtering filtering) {
        List<User> users = someUserStore.listUsers();
        Stream<User> stream = Stream.of(users);
        if(paging != null) {
            // Inefficient as we're loading all users from the store for every request; a realistic
            // implementation might use the paging parameters to e.g. build an SQL LIMIT clause
            stream = stream.skip(page.getPage() * page.getPageSize())
                           .limit(page.getPageSize());
        }
        return new ListResult<>(
            stream, users.size()
        );
    }
    
    // Enable paging
    @Override
    public boolean supportsPaging() {
        return true;
    }
}
```

The response now includes the paging information, as well as the total count of items:

```json
{
    "page": 0,
    "pageSize": 10,
    "items": [
        {
            "username": "alice",
            "role": "admin"
        },
        {
            "username": "bob",
            "role": "user"
        }
    ],
    "totalItems": 2
}
```

### Sorting

If enabled by overriding the `Listable.supportsSorting()` method to return `true`, clients can supply a `sortBy`
query parameter to indicate what field(s) they want to sort by as well as sort direction using a syntax similar
to SQL:

`GET /users?sortBy=username ASC, role DESC`

This sorting specifier, if present, will be parsed and provided to your list method as the `Sorting` parameter. Note
that unlike paging, which has default values, the sorting parameter **will be null** if no sortBy was included in
the request. The `Sorting` object just contains a list of `SortField`, each consisting of the requested field name
and sort direction. Because sorting can be combined with paging and filtering, and your collection may not be held
entirely in memory, it is up to you to determine how to use these values to query your datastore for the right
results. In the case that it is useful however, the Sorting class does provide a Comparator implementing its sort rules
which can be used to e.g. sort a Stream. If you do this, be careful to apply sorting *before* paging so that you sort
the entire collection, not just the current page:

```java
@Override
public ListResult<User> list(Context ctx, Paging page, Filtering filter, Sorting sort) {
    List<User> users = someUserStore.listUsers();
    Stream<User> stream = users.stream();
    int count = users.size();

    if(sort != null) {
        // toComparator takes a function mapping a String field name to a Comparator comparing
        // items on that field (ascending). It will handle reversing the order if a descending
        // sort was requested. 
        stream = stream.sorted(sort.toComparator(field -> switch (field) {
            case "username" -> Comparator.comparing(User::getUsername);
            case "email" -> Comparator.comparing(User::getEmail);
            default -> throw new BadRequestException("Unrecognized sort field");
        }));
    }

    stream = stream.skip(page.getPage() * page.getPageSize())
                   .limit(page.getPageSize());

    return new ListResult<>(stream, count);
}

```

### Filtering

ListHandler supports a basic filtering syntax consisting of a filter name and zero or more parameters:

`GET /users?filter=isActive`

`GET /users?filter=hasRole("staff")`

`GET /users?filter=loggedInBetween(2022-01-01, 2022-02-28)`

If enabled by overriding the `Listable.supportsFiltering()` method to return `true`, and filtering was requested by the
client, the parsed filter name and parameters will be provided as the `Filtering` parameter to your list method 
(otherwise this parameter will be null).

**Note:** The only parameter types currently supported are strings (enclosed in double quotes), positive integers
(unquoted; will be provided as Integer objects) and dates (format `YYYY-MM-DD` only; unquoted; provided as Strings). 

## Sub-resources

When a request path includes one or more segments after the resource ID (e.g. `/<id>/widgets`), this is considered a 
sub-resource request, and can be handled by registering a sub-resource handler. You can install another RestRouter on 
a subResource chain to support a full set of REST actions on the sub-resource as well:

```java
server.use(new RestRouter()
    .list( /* middleware */ )
    .get( /* middleware */ )
    // First parameter is the sub-resource name, second is what the parent item's ID should be
    // called when fetching it from within handler code (see next example)
    .subResource("widgets", "userID", new RestRouter()
        .list( /* middleware */)
        .get( /* middleware */)
            // etc
    )
);
```

In the handlers for sub-resource requests, you'll likely need access to the parent resource's ID, so this
is available from the `RestRouter.getParentID` utility function by passing it the parent ID name you defined when
declaring the sub-resource:

```java
public class WidgetCollection implements Gettable<Widget> {
    @Override
    public Widget get(Context ctx, String widgetID) {
        String userId = RestRouter.getParentId(ctx, "userID");
        User user = someUserStore.loadUserById(userId);
        if (user != null) {
            return user.getWidgetById(widgetID);
        } else {
            return null;
        }
    }
}
```

You can nest sub-resources in sub-resources as deep as you like, just make sure to keep your parent IDs straight:

```java
router.mount("/users", new RestRouter()
    .subResource("apps", "userID", new RestRouter()
        .subResource("panels", "appID", new RestRouter()
            .subResource("widgets", "panelID", new RestRouter()
                .list(new ListHandler<>(new WidgetLister())
            )
        )
    )
);
```

Here, a GET request to `/users/alice/apps/37/panels/42/widgets` (note we mounted the whole thing under `/users`) will
invoke the WidgetLister; it will be able to access its parent IDs as follows:

```java
RestRouter.getParentId(ctx, "userID");  // alice
RestRouter.getParentId(ctx, "appID");   // 37
RestRouter.getParentId(ctx, "panelID"); // 42
```