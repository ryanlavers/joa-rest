package ca.lavers.joa.rest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestPathParser {

    @Test
    void testRoot() {
        PathParser parser = new PathParser("/");

        assertTrue(parser.isCollectionRequest());
        assertFalse(parser.isItemRequest());
        assertFalse(parser.isSubResourceRequest());
    }

    @Test
    void testItem() {
        PathParser parser = new PathParser("/foo");

        assertFalse(parser.isCollectionRequest());
        assertTrue(parser.isItemRequest());
        assertFalse(parser.isSubResourceRequest());

        assertEquals("foo", parser.getItemID());
    }

    @Test
    void testItemTrailingSlash() {
        PathParser parser = new PathParser("/foo/");

        assertFalse(parser.isCollectionRequest());
        assertTrue(parser.isItemRequest());
        assertFalse(parser.isSubResourceRequest());

        assertEquals("foo", parser.getItemID());
    }

    @Test
    void testSubResource() {
        PathParser parser = new PathParser("/foo/bar");

        assertFalse(parser.isCollectionRequest());
        assertFalse(parser.isItemRequest());
        assertTrue(parser.isSubResourceRequest());

        assertEquals("foo", parser.getItemID());
        assertEquals("bar", parser.getSubResourceName());
        assertEquals("/", parser.getRemainingPath());
    }

    @Test
    void testSubResourceLonger() {
        PathParser parser = new PathParser("/foo/bar/baz/buz");

        assertFalse(parser.isCollectionRequest());
        assertFalse(parser.isItemRequest());
        assertTrue(parser.isSubResourceRequest());

        assertEquals("foo", parser.getItemID());
        assertEquals("bar", parser.getSubResourceName());
        assertEquals("/baz/buz", parser.getRemainingPath());
    }

}
