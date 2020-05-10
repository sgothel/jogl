/*
 * Copyright (c) 2020 Business Operation Systems GmbH. All Rights Reserved.
 */
package jogamp.graph.font.typecast.ot;

/**
 * Formatting utilities.
 *
 * @author <a href="mailto:bhu@top-logic.com">Bernhard Haumacher</a>
 */
public class Fmt {

    private static final String PADDING = "                ";

    /**
     * Left aligned number in a field of the given number of digits.
     */
    public static String pad(int digits, int value) {
        String result = Integer.toString(value);
        if (result.length() >= digits) {
            return result;
        }
        return PADDING.substring(0, digits - result.length()) + result;
    }
    
}
