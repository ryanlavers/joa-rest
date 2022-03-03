package ca.lavers.joa.rest;

public class PathParser {

    private final String[] pathParts;

    public PathParser(String path) {
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        if(path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }

        this.pathParts = path.equals("") ? new String[]{} : path.split("/", 3); // so that "" -> [] and not [""]
    }

    public boolean isCollectionRequest() {
        return pathParts.length == 0;
    }

    public boolean isItemRequest() {
        return pathParts.length == 1;
    }

    public boolean isSubResourceRequest() {
        return pathParts.length > 1;
    }

    public String getItemID() {
        return pathParts[0];
    }

    public String getSubResourceName() {
        return pathParts[1];
    }

    public String getRemainingPath() {
        if(pathParts.length > 2) {
            return "/" + pathParts[2];
        }
        else {
            return "/";
        }
    }
}
