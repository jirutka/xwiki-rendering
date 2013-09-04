/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.internal.renderer.markdown11;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods used for escaping wiki syntax.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class EscapeUtils
{
    /**
     * Character used to escape sequences that might be confused with wiki syntax.
     */
    public static final char ESCAPE_CHAR = '\\';
    
    
    /**
     * Look for the pattern and escape the first character of the first matched group.
     *
     * @param buffer the accumulated buffer to check and escape
     * @param pattern the regular expression with exactly one capturing group
     */
    public static void escapeFirstMatchedCharacter(StringBuffer buffer, Pattern pattern)
    {
        Matcher matcher = pattern.matcher(buffer);
        if (matcher.lookingAt()) {
            buffer.insert(matcher.start(1), ESCAPE_CHAR);
        }
    }

    /**
     * Look for the pattern and escape the first character of every captured group
     * of all matched subsequences.
     *
     * @param buffer the accumulated buffer to check and escape
     * @param pattern the regular expression with at least one capturing group
     */
    public static void escapeAllMatchedSubsequences(StringBuffer buffer, Pattern pattern)
    {
        Matcher matcher = pattern.matcher(buffer.toString());
        int groupCount = matcher.groupCount();
        int offset = 0;

        if (groupCount == 1) {
            while (matcher.find()) {
                buffer.insert(matcher.start(1) + offset++, ESCAPE_CHAR);
            }

        } else if (groupCount > 1) {
            while (matcher.find()) {
                for (int group = 1; group <= groupCount; group++) {
                    if (matcher.start(group) != -1) {
                        buffer.insert(matcher.start(group) + offset++, ESCAPE_CHAR);
                    }
                }
            }

        } else {
            throw new IllegalArgumentException("Pattern must contain at least one capturing group");
        }
    }

    /**
     * Look for the character at the beginning of the buffer and escape it if found.
     *
     * @param buffer the accumulated buffer to check and escape
     * @param ch the character to search for
     */
    public static void escapeWhenStartsWith(StringBuffer buffer, char ch)
    {
        if (buffer.charAt(0) == ch) {
            buffer.insert(0, ESCAPE_CHAR);
        }
    }
    
    /**
     * Replaces each substring of the given buffer that matches the match string
     * with the given replacement.
     *
     * @param buffer the accumulated buffer to check and escape
     * @param match the string to be matched
     * @param replacement the string to be substituted for each match
     */
    public static void replaceAll(StringBuffer buffer, String match, String replacement)
    {
        int pos = -replacement.length();
        while ((pos + replacement.length() < buffer.length())
                && ((pos = buffer.indexOf(match, pos + replacement.length())) != -1)) {

            buffer.replace(pos, pos + match.length(), replacement);
        }
    }
}
