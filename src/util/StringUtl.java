package util;

import bitap.Bitap;

public class StringUtl {
    /**
     * Clean the input string
     * - Trim
     * - Convert to lower case
     * - Remove chars which aren't included in the Bitap alphabet
     *
     * @param str input string
     * @return cleaned string
     */
    public static String clean(String str) {
        str = str.trim().toLowerCase();

        StringBuilder builder = new StringBuilder("");
        for (int i = 0; i < str.length(); ++i) {
            char currentChar = str.charAt(i);

            if (Bitap.Alphabet.contains(currentChar) && currentChar != '&') {
                builder.append(currentChar);
            }
        }

        return builder.toString();
    }
}
