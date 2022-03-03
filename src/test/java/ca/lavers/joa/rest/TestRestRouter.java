package ca.lavers.joa.rest;

import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.errors.MethodNotAllowedException;
import ca.lavers.joa.test.MockRequest;
import ca.lavers.joa.test.TestContext;
import ca.lavers.joa.test.TestMiddleware;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestRestRouter {

    @TestFactory
    Stream<DynamicTest> basicRouting() {
        return Stream.of(
                //                        method    path     expected
                constructBasicRoutingTest("GET",    "/",     "list"),
                constructBasicRoutingTest("GET",    "/foo",  "get"),
                constructBasicRoutingTest("GET",    "/foo/", "get"),
                constructBasicRoutingTest("POST",   "/",     "create"),
                constructBasicRoutingTest("PUT",    "/foo",  "update"),
                constructBasicRoutingTest("DELETE", "/foo",  "delete")
        );
    }

    DynamicTest constructBasicRoutingTest(String method, String path, String expectedResult) {
        return DynamicTest.dynamicTest(method + " " + path + " -> " + expectedResult, () -> {

            // Middleware that should or should not be run, respectively
            TestMiddleware doRun = new TestMiddleware();
            TestMiddleware dontRun = new TestMiddleware();

            // Checks that the Item ID context parameter is set correctly; will only be
            // put on the correct chains (get, update, delete) below
            Middleware attrChecker = (ctx, next) -> {
                assertEquals("foo", ctx.get(RestRouter.NS, RestRouter.ITEM_ID, String.class).get());
                next.run();
            };

            // Set up a router with 'doRun' only on the correct chain
            RestRouter router = new RestRouter()
                    .list(               "list".equals(expectedResult)   ? doRun : dontRun)
                    .get(attrChecker,    "get".equals(expectedResult)    ? doRun : dontRun)
                    .create(             "create".equals(expectedResult) ? doRun : dontRun)
                    .update(attrChecker, "update".equals(expectedResult) ? doRun : dontRun)
                    .delete(attrChecker, "delete".equals(expectedResult) ? doRun : dontRun);

            new MockRequest(method, path).run(router);

            assertTrue(doRun.ran());
            assertFalse(dontRun.ran());
        });
    }

    // Test that invalid requests throw a MethodNotAllowedException.
    // Note: Using PATCH here as an invalid method because it's not currently implemented.
    @TestFactory
    Stream<DynamicTest> invalidRequests() {
        return Stream.of(
                constructInvalidRequestsTest("POST", "/foo"),
                constructInvalidRequestsTest("PUT", "/"),
                constructInvalidRequestsTest("DELETE", "/"),
                constructInvalidRequestsTest("PATCH", "/"),
                constructInvalidRequestsTest("PATCH", "/foo")
        );
    }

    DynamicTest constructInvalidRequestsTest(String method, String path) {
        return DynamicTest.dynamicTest(method + " " + path, () -> {
            TestMiddleware m = new TestMiddleware();
            RestRouter router = new RestRouter()
                    .list(m)
                    .get(m)
                    .create(m)
                    .update(m)
                    .delete(m);

            TestContext ctx = new MockRequest(method, path).run(router);

            assertTrue(ctx.getThrownException() instanceof MethodNotAllowedException);
            assertFalse(m.ran());
        });
    }

    @Test
    void subRequests() {
        TestMiddleware m = new TestMiddleware();
        RestRouter router = new RestRouter()
                .subResource("bar", "fooID", m, (ctx, next) -> {
                    assertEquals("/", ctx.request().path());
                    Map<String, String> parentIDs = ctx.get(RestRouter.NS, RestRouter.PARENT_IDS, Map.class).get();
                    assertEquals("37", parentIDs.get("fooID"));
                });

        MockRequest.get("/37/bar").run(router);

        // Ensure that the middleware chain was actually called
        assertTrue(m.ran());
    }

    @Test
    void subRequestInception() {
        TestMiddleware m = new TestMiddleware();
        RestRouter router = new RestRouter()
                .subResource("bar", "fooID", new RestRouter()
                    .subResource("baz", "barID", m, (ctx, next) -> {
                        assertEquals("/", ctx.request().path());
                        Map<String, String> parentIDs = ctx.get(RestRouter.NS, RestRouter.PARENT_IDS, Map.class).get();
                        assertEquals("37", parentIDs.get("fooID"));
                        assertEquals("42", parentIDs.get("barID"));
                    })
                );

        MockRequest.get("/37/bar/42/baz").run(router);

        assertTrue(m.ran());
    }

    @Test
    void methodNotAllowedIfNotConfigured() {
        RestRouter router = new RestRouter();

        TestContext ctx = MockRequest.get("/").run(router);

        assertTrue(ctx.getThrownException() instanceof MethodNotAllowedException);
    }

}
