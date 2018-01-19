package bitap;

import java.util.HashSet;
import java.util.Set;

/**
 * Fuzzy string match
 * Could be REALLY slow
 * This implementation use Wu-Manber modification to the original algorithm
 * Therefore the fuzziness is measured with Levenshtein instead of Hamming
 * i.e. better matching and slower performance
 */
public class Bitap {

    // The Alphabet only contains the following characters:
    // - Printable characters in ASCII
    // - Vietnamese characters in lower case (as inputs are guaranteed to be lower case)
    public static Set<Character> Alphabet;
    static {
        Alphabet = new HashSet<>();

        for (int i = 32; i < 127; ++i) {
            Alphabet.add((char) i);
        }

        char[] Vietnamese = {'á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ',
                'đ', 'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ', 'í', 'ì', 'ỉ', 'ĩ', 'ị',
                'ó', 'ò', 'ỏ','õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ',
                'ú', 'ù', 'ủ', 'ũ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự', 'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ'};
        for (char vietnamese : Vietnamese) {
            Alphabet.add(vietnamese);
        }
    }

    /**
     * Check if the text "contains" the pattern
     * Errors measured by Levenshtein metric are allowed (hence "fuzzy")
     * Note that the lev parameter is the maximum allowed
     * Actual difference can be smaller than lev
     *
     * @param text to be searched in
     * @param pattern to be searched
     * @param lev the maximum difference measured by Levenshtein
     * @return true if the text contains pattern
     */
    public static boolean fuzzyContains(String text, String pattern, int lev) {
        if (pattern.length() < 64) {
            return BitapLong.fuzzyContains(text, pattern, lev);
        } else {
            if (pattern.length() > 255) {
                pattern = pattern.substring(0, 255);
            }
            return BitapExtended.fuzzyContains(text, pattern, lev);
        }
    }
}
