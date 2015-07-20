package com.maogm.wanquribao.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by maoguangming on 7/21/15.
 */
public class StringUtil {
    public static String serializeStringList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    public static List<String> unserializeStringList(String str) {
        return new ArrayList<>(Arrays.asList(str.split(",")));
    }
}
