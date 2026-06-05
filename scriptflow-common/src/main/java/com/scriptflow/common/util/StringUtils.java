package com.scriptflow.common.util;

import java.util.Collection;

/**
 * String utility methods.
 */
public class StringUtils {

    public static boolean isBlank(CharSequence cs) {
        return cs == null || cs.toString().trim().isEmpty();
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    public static String mask(String str, int start, int end) {
        if (isBlank(str)) return str;
        char[] chars = str.toCharArray();
        for (int i = start; i < end && i < chars.length; i++) {
            chars[i] = '*';
        }
        return new String(chars);
    }

    public static String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
