package ca.lavers.joa.rest;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Sorting {

    private final List<SortField> sortFields;

    public Sorting(List<SortField> sortFields) {
        this.sortFields = sortFields;
    }

    public List<SortField> getSortFields() {
        return sortFields;
    }

    public <T> Comparator<T> toComparator(Function<String, Comparator<T>> fieldLookup) {
        Map<String, Comparator<T>> comparators = new HashMap<>();
        for (SortField sf : this.getSortFields()) {
            if(!comparators.containsKey(sf.getField())) {
                Comparator<T> c = fieldLookup.apply(sf.getField());
                if (c == null) {
                    c = (a, b) -> 0;
                }
                comparators.put(sf.getField(), c);
            }
        }

        return (a, b) -> {
            for (SortField sf : this.getSortFields()) {
                int result = comparators.get(sf.getField()).compare(a, b);
                if (sf.getDirection() == SortDirection.DESCENDING) {
                    result *= -1;
                }
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }

}
