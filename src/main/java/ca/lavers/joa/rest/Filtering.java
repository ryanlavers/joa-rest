package ca.lavers.joa.rest;

import java.util.List;

public class Filtering {

    private final String name;
    private final List<Object> args;

    public Filtering(String name, List<Object> args) {
        this.name = name;
        this.args = args;
    }

    public String getFilterName() {
        return name;
    }

    public List<Object> getArgs() {
        return args;
    }

    public int getArgCount() {
        return args.size();
    }

    public String getStringArg(int index) {
        return (String) args.get(index);
    }

    public int getIntArg(int index) {
        Object arg = args.get(index);
        if(arg instanceof Integer) {
            return (int) arg;
        }

        return Integer.parseInt((String) arg);
    }

}
