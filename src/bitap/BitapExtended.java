package bitap;

import java.util.*;

/**
 * Based on the masonmlai's implementation
 * The main drawback of the original implementation (BitapLong) is that the pattern length is limited to 63
 * This implementation use BitSet instead of long to holds the bit mask, therefore eliminate this limitation
 * Pattern's length can be arbitrary, but to limit the processing time, it is set at 255
 *
 * A note on the DEFAULT_MAX_LENGTH:
 * The original author includes a '&' character at the end of the strings as a "sentinel value"
 * Therefore, '&' must be excluded from the inputs and the DEFAULT_MAX_LENGTH must be +1 to make room for it
 */

class BitapExtended {
    private static final int DEFAULT_MAX_LENGTH = 255 + 1;

    private static final BitSet ONE = getOne();
    private static final BitSet ZERO = getZero();

    private static BitSet getNegatedZero() {
        BitSet negated_zero = new BitSet(DEFAULT_MAX_LENGTH);
        negated_zero.set(0, DEFAULT_MAX_LENGTH);
        return negated_zero;
    }

    private static BitSet getNegatedOne() {
        BitSet negated_one = new BitSet(DEFAULT_MAX_LENGTH);
        negated_one.set(0, DEFAULT_MAX_LENGTH - 1);
        return negated_one;
    }

    private static BitSet getOne() {
        BitSet one = new BitSet(DEFAULT_MAX_LENGTH);
        one.set(DEFAULT_MAX_LENGTH - 1);
        return one;
    }

    private static BitSet getZero() {
        return new BitSet(DEFAULT_MAX_LENGTH);
    }

    /**
     * Initialize the alphabet masks, one for each character of the alphabet.
     * Each alphabet mask is the length of the needle, plus a zero at the
     * right-most position. Aside from this zero, other zeroes mark locations
     * where the corresponding letter appears. For example, if the needle were
     * "Mississippi", the alphabet masks would be:
     *
     *		M i s s i s s i p p i
     *
     *  M : 0 1 1 1 1 1 1 1 1 1 1 0
     *  i : 1 0 1 1 0 1 1 0 1 1 0 0
     *  s : 1 1 0 0 1 0 0 1 1 1 1 0
     *  p : 1 1 1 1 1 1 1 1 0 0 1 0
     *
     */
    private static Map<Character, BitSet> generateAlphabetMasks(String needle) {
        Map<Character, BitSet> masks = new HashMap<>();
        int len = needle.length() - 1;

        for (Character letter : Bitap.Alphabet) {
            BitSet mask = getNegatedZero();
            for (int pos = 0; pos < needle.length(); pos++) {
                if (letter.equals(needle.charAt(len - pos))) {
//                    mask &= ~(1L << pos);
                    BitSet temp = shiftLeft(ONE, pos);
                    temp.flip(0, DEFAULT_MAX_LENGTH);
                    mask.and(temp);
                }
            }
            masks.put(letter, shiftLeft(mask, 1));
        }

        return masks;
    }

    /**
     * The starting bit array, commonly denoted as 'R' in the literature.
     * Commonly thought of as a 2D matrix with dimensions
     * max-Levenshtein-distance by needle-length, with the top-most row
     * corresponding to a Levenshtein distance of 0, and the bottom-most
     * corresponding to a distance of k. Initialized as all-ones, except
     * for the right-most column, which is all-zeroes. This array is
     * updated dynamically as the algorithm progresses through the search
     * corpus. This implementation is just a 1D array of longs, where each
     * long, in binary, functions as a row of the matrix. Also, since
     * longs are 64-bit, the extraneous columns on the left are just all-
     * ones.
     *
     * An example with a max-Levenshtein distance of two:
     *
     *  1 1 1 1 1 1 1 1 1 1 ... 1 1 1 1 1 1 1 1 1 0
     *  1 1 1 1 1 1 1 1 1 1 ... 1 1 1 1 1 1 1 1 1 0
     *  1 1 1 1 1 1 1 1 1 1 ... 1 1 1 1 1 1 1 1 1 0
     * |<---------------- 64-bits ---------------->|
     *
     * @param lev - the maximum Levenshtein distance for a substring match
     * @return the starting bit array
     */
    private static BitSet[] generateBitArray(int lev) {
        BitSet[] bitArray = new BitSet[lev + 1];

        for (int k = 0; k <= lev; k++) {
//            bitArray[k] = ~1;
            bitArray[k] = getNegatedOne();
        }

        return bitArray;
    }

	/* Wu-Manber implementation notes
	 *
	 * Most implementations online start from the beginning of the haystack
	 * and proceed towards the end. When a zero bubbles up to the end of the
	 * bit array, that position (the end position) is recorded, and the
	 * needle length subtracted to get the start position. Due to indels,
	 * this start position is often incorrect. As a consequence, this
	 * implementation starts from the end of the haystack, with a reversed
	 * needle, and proceeds to the start of the haystack. The "end position"
	 * that this implementation finds is really the start position, since
	 * the string search is being done with all text "flipped".
	 */

    /**
     * Wu-Manber algorithm. Finds all approximate (within a given
     * Levenshtein distance) matches of a needle within a haystack.
     *
     * @param lev - the maximum Levenshtein distance for a substring match
     * @return true if the haystack "contains" the needle
     */
    public static boolean fuzzyContains(String haystack, String needle, int lev) {
        int position = haystack.indexOf(needle);
        if (position >= 0) {
            return true;
        }

        position = -1;
        haystack = haystack + "&";  // sentinel value

        BitSet[] bitArray = generateBitArray(lev);
        Map<Character, BitSet> alphabetMasks = generateAlphabetMasks(needle);

        for (int i = haystack.length() - 1; i >= 0; --i) {
            BitSet[] old = bitArray.clone();
//            bitArray[0] = (old[0] << 1) | alphabetMasks.get(haystack.charAt(i));
            BitSet temp = shiftLeft(old[0], 1);

            if (alphabetMasks.get(haystack.charAt(i)) == null) {
                System.out.println(haystack.charAt(i));
            }
            temp.or(alphabetMasks.get(haystack.charAt(i)));
            bitArray[0] = temp;

            if (lev > 0) {
                for (int k = 1; k <= lev; ++k) {
//                    long ins = old[k - 1];
                    BitSet ins = old[k - 1];

//                    long sub = ins << 1;
                    BitSet sub = shiftLeft(ins, 1);

//                    long del = bitArray[k - 1] << 1;
                    BitSet del = shiftLeft(bitArray[k - 1], 1);

//                    long match = (old[k] << 1) | alphabetMasks.get(haystack.charAt(i));
                    temp = shiftLeft(old[k], 1);
                    temp.or(alphabetMasks.get(haystack.charAt(i)));
                    BitSet match = temp;

//                    bitArray[k] = ins & del & sub & match;
                    // Note: bitwise operators MODIFY the BitSet
                    // Therefore ins.or(...) is wrong
                    sub.and(ins);
                    sub.and(del);
                    sub.and(match);
                    bitArray[k] = sub;
                }
            }

//            if (0 == (bitArray[lev] & (1 << needle.length()))) {
//                position = i;
//            }
            temp = shiftLeft(ONE, needle.length());
            temp.or(bitArray[lev]);
            if (isEqual(temp, ZERO)) {
                position = i;
            }
        }
        
        return position != -1;
    }

    private static BitSet shiftLeft(BitSet input, int shift) {
        BitSet shifted = new BitSet(DEFAULT_MAX_LENGTH);
        int j = 0;
        for (int i = shift; i < DEFAULT_MAX_LENGTH; ++i) {
            shifted.set(j++, input.get(i));
        }
        return shifted;
    }

    private static boolean isEqual(BitSet a, BitSet b) {
        for (int i = 0; i < DEFAULT_MAX_LENGTH; ++i) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }
}
