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

import java.util.regex.Pattern;

import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;

import static org.xwiki.rendering.internal.renderer.markdown11.EscapeUtils.ESCAPE_CHAR;
import static org.xwiki.rendering.internal.renderer.markdown11.EscapeUtils.escapeAllMatchedSubsequences;
import static org.xwiki.rendering.internal.renderer.markdown11.EscapeUtils.escapeFirstMatchedCharacter;
import static org.xwiki.rendering.internal.renderer.markdown11.EscapeUtils.escapeWhenStartsWith;
import static org.xwiki.rendering.internal.renderer.markdown11.EscapeUtils.replaceAll;

/**
 * Escape characters that would be confused for Markdown wiki syntax if they were not escaped.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownEscapeHandler
{
    /**
     * Regex pattern with group capturing a non-escaped brackets.
     */
    private static final Pattern BRACKETS_PATTERN = Pattern.compile("[^\\\\](\\[|\\])");

    /**
     * Regex pattern with groups capturing en dash ('--') and em dash ('---').
     */
    private static final Pattern DASHES_PATTERN = Pattern.compile("(-)?(--)");

    /**
     * Regex pattern with group capturing character(s) around an inline code (`foo`, ``bar``).
     * It matches only pair occurrence of the character, not isolated.
     */
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("(`{1,2})[^`]+(?=`{1,2})");
    
    /**
     * Regex pattern with group capturing character(s) to open italic and bold format
     * ('*', '_', '**', '__' and combinations).
     */
    private static final Pattern ITALIC_BOLD_PATTERN = Pattern.compile("(?:^|\\s)([\\*_]{1,3})[^\\s\\*_]");

    /**
     * Regex pattern with group capturing character(s) to open link ('['), image ('![') and macro ('#[').
     */
    private static final Pattern LABEL_PATTERN = Pattern.compile("([#!]?\\[+)");

    /**
     * Regex pattern with group capturing horizontal line and setext-style header.
     */
    private static final Pattern LINE_AND_SETEXT_HEADER_PATTERN = Pattern.compile("^((?:[=\\-*] ?){3,})$");

    /**
     * Regex pattern with group capturing the bullet character ('*', '+' or '-') of an unordered list
     * and the description character (':' or '~') of a definition list item.
     */
    private static final Pattern LIST_PATTERN = Pattern.compile("\\s*([\\*+-]|[:~])\\s+");

    /**
     * Regex pattern with group capturing the period character of a ordered (numbered) list.
     */
    private static final Pattern NUMBERED_LIST_PATTERN = Pattern.compile("\\s*[0-9]+(\\.)\\s+");

    /**
     * Regex pattern with group capturing parenthesis.
     */
    private static final Pattern PARENTHESIS_PATTERN = Pattern.compile("([()])");

    /**
     * Regex pattern with group capturing character to open superscript ('^') and subscript ('~') format.
     */
    private static final Pattern SUPER_SUB_SCRIPT_PATTERN = Pattern.compile("(?:^|\\s)([~^])(?:\\S|\\\\\\s)+\\1");

    /**
     * The marker character a section header starts with.
     */
    private static final char ATX_HEADER_MARKER = '#';

    /**
     * The marker character a blockquote starts with.
     */
    private static final char BLOCKQUOTE_MARKER = '>';

    private boolean onNewLine = true;

    /**
     * If spaces should be escaped.
     */
    private boolean escapeSpaces;


    /**
     * Escapes content of a label (e.g. {@code [content], [[content]], ![content]}).
     * 
     * @param label text to be escaped
     * @return escaped label
     */
    public static String escapeLabel(String label)
    {
        if (label.contains("[") || label.contains("]")) {
            StringBuffer buffer = new StringBuffer(label);
            escapeAllMatchedSubsequences(buffer, BRACKETS_PATTERN);
            
            return buffer.toString();
        }
        return label;
    }

    /**
     * Escapes content of a reference (e.g. {@code [label](reference)})
     * 
     * @param reference text to be escaped
     * @return escaped reference
     */
    public static String escapeReference(String reference)
    {
        if (reference.contains("(") || reference.contains(")")) {
            StringBuffer buffer = new StringBuffer(reference);
            escapeAllMatchedSubsequences(buffer, PARENTHESIS_PATTERN);

            return buffer.toString();
        }
        return reference;
    }


    public void setOnNewLine(boolean onNewLine)
    {
        this.onNewLine = onNewLine;
    }

    public boolean isOnNewLine()
    {
        return this.onNewLine;
    }

    /**
     * @param escapeSpaces if spaces should be escaped
     */
    public void setEscapeSpaces(boolean escapeSpaces)
    {
        this.escapeSpaces = escapeSpaces;
    }

    /**
     * Escapes characters in the given {@code buffer} that can be confused for Markdown wiki syntax.
     * 
     * @param buffer the accumulated buffer to work with
     * @param blockState
     */
    public void escape(StringBuffer buffer, BlockStateChainingListener blockState)
    {
        // Escape backslash symbol (i.e. the escape character).
        // Note: This needs to be the first replacement since other replacements below also use the backslash symbol
        replaceAll(buffer, "" + ESCAPE_CHAR, "" + ESCAPE_CHAR + ESCAPE_CHAR);

        // Escape all space or tab characters if required
        if (escapeSpaces) {
            replaceAll(buffer, " ", ESCAPE_CHAR + " ");
            replaceAll(buffer, "\t", ESCAPE_CHAR + "\t");
        }

        // Escape smart en-dash and em-dash pattern
        // Note: This needs to be before LINE_AND_SETEXT_HEADER to avoid multiple escaping.
        if (blockState.isInLine() && containsAny(buffer, "--")
                && !LINE_AND_SETEXT_HEADER_PATTERN.matcher(buffer).matches()) {
            escapeAllMatchedSubsequences(buffer, DASHES_PATTERN);
        }

        // Escape begin of link, image and macro
        // Note: This needs to be before ATX_HEADER_MARKER to avoid multiple escaping.
        if (containsAny(buffer, "[")) {
            escapeAllMatchedSubsequences(buffer, LABEL_PATTERN);
        }

        // When in a paragraph we need to escape symbols that are at beginning of lines and that could be confused
        // with list items, headers or blockquotes.
        if (blockState.isInLine() && isOnNewLine()) {  // blank line? but x in lists, quotes...!

            // Look for bullet and definition list pattern at beginning of line and escape the first character only
            escapeFirstMatchedCharacter(buffer, LIST_PATTERN);

            // Look for numbered list pattern at beginning of line and escape the first character only
            escapeFirstMatchedCharacter(buffer, NUMBERED_LIST_PATTERN);

            // Look for horizontal line and setext-style header pattern at beginning of line and escape it.
            escapeFirstMatchedCharacter(buffer, LINE_AND_SETEXT_HEADER_PATTERN);

            // Look for header marker at beginning of line and escape the first character only
            escapeWhenStartsWith(buffer, ATX_HEADER_MARKER);

            // Look for blockquote marker at beginning of line and escape the first character only
            escapeWhenStartsWith(buffer, BLOCKQUOTE_MARKER);
        }

        // Escape table column's divider character
        if (blockState.isInTable()) {
            replaceAll(buffer, "|", ESCAPE_CHAR + "|");
        }

        // Escape italic and bold patterns
        if (containsAny(buffer, "*", "_")) {
            escapeAllMatchedSubsequences(buffer, ITALIC_BOLD_PATTERN);
        }

        // Escape superscript and subscript patterns
        if (containsAny(buffer, "^", "~")) {
            escapeAllMatchedSubsequences(buffer, SUPER_SUB_SCRIPT_PATTERN);
        }
        
        // Escape inline code patterns
        if (containsAny(buffer, "`")) {
            escapeAllMatchedSubsequences(buffer, INLINE_CODE_PATTERN);
        }

        // Escape ":" in "image:something", "attach:something" and "mailto:something"
        // Note: even though there are some restriction in the URI specification as to what character is valid after
        // the ":" character following the scheme we only check for characters greater than the space symbol for
        // simplicity.
        escapeURI(buffer, "image:");
        escapeURI(buffer, "attach:");
        escapeURI(buffer, "mailto:");
    }


    protected void escapeURI(StringBuffer buffer, String match)
    {
        int pos = buffer.indexOf(match);
        if (pos > -1) {
            // Escape the ":" symbol
            buffer.insert(pos + match.length() - 1, ESCAPE_CHAR);
        }
    }

    /**
     * Returns true if the buffer contains any of the given search sequences.
     *
     * <p>
     * This is used for quick check if the buffer contains something that should
     * be escaped, it's faster than regular expression matching.
     * </p>
     *
     * @param buffer the accumulated buffer to check
     * @param match the sequence(s) to search for
     * @return true if the given buffer contains any of the searched sequences
     */
    protected boolean containsAny(StringBuffer buffer, CharSequence... match)
    {
        String s = buffer.toString();

        for (CharSequence m : match) {
            if (s.contains(m)) {
                return true;
            }
        }
        return false;
    }
}
