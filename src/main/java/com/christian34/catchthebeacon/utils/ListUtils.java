package com.christian34.catchthebeacon.utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListUtils {

    /**
     * @param source list to transform
     * @param offset initial offset of the subarray.
     * @return a {@code List} that contains the characters of the specified subarray of the character array.
     */
    @NotNull
    public static <T> List<T> offsetList(List<T> source, int offset) {
        List<T> list = new ArrayList<>();
        if (offset >= source.size() || offset <= 0) {
            return Collections.emptyList();
        }
        for (int i = offset; i < source.size(); i++) {
            list.add(source.get(i));
        }
        return list;
    }

}
