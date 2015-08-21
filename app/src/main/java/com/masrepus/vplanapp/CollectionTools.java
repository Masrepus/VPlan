package com.masrepus.vplanapp;

import com.masrepus.vplanapp.vplan.Row;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by samuel on 17.08.15.
 */
public class CollectionTools {

    public static <T> Collection<T> union(Collection<T> coll1, Collection<T> coll2) {
        Set<T> union = new HashSet<>(coll1);
        union.addAll(new HashSet<>(coll2));
        return union;
    }

    public static <T> Collection<T> intersect(Collection<T> coll1, Collection<T> coll2) {
        Set<T> intersection = new HashSet<>(coll1);
        intersection.retainAll(new HashSet<>(coll2));

        return intersection;
    }

    public static <T> Collection<T> nonOverLap(Collection<T> coll1, Collection<T> coll2) {
        Collection<T> result = union(coll1, coll2);
        result.removeAll(intersect(coll1, coll2));
        return result;
    }
}
