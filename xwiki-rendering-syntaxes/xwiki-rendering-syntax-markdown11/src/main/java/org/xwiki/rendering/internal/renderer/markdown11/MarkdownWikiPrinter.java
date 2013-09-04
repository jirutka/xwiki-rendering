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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringTokenizer;

import org.xwiki.rendering.renderer.printer.LookaheadWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.stripStart;

/**
 * A Wiki printer that knows how to escape characters that would otherwise mean
 * something different in Markdown syntax. It also handles lines indentation
 * for lists etc.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownWikiPrinter extends LookaheadWikiPrinter
{
    private final MarkdownListenerChain listenerChain;

    private final MarkdownEscapeHandler escapeHandler;

    /**
     * Characters to be printed at the beginning of each new line.
     */
    private Deque<String> linePrefix = new ArrayDeque<String>();

    /**
     * Strip all new lines from the start of next printed text(s)?
     */
    private boolean suppressNewLines = true;

    /**
     * Is the page still blank? In other words, does this printer already
     * prints something?
     */
    private boolean isPageBlank = true;


    public MarkdownWikiPrinter(WikiPrinter printer, MarkdownListenerChain listenerChain)
    {
        super(printer);
        this.listenerChain = listenerChain;
        this.escapeHandler = new MarkdownEscapeHandler();
    }


    @Override
    protected void printInternal(String text)
    {
        if (suppressNewLines) {
            text = stripStart(text, "\n");
        }
        
        if (isPageBlank && isNotEmpty(text)) {
            printLinePrefix();
            isPageBlank = false;
        }

        if (text.contains("\n")) {
            StringTokenizer st = new StringTokenizer(text, "\n", true);

            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                super.printInternal(token);

                if (token.equals("\n")) {
                    printLinePrefix();
                }
            }
        } else {
            super.printInternal(text);
        }

        if (isNotEmpty(text)) {
            escapeHandler.setOnNewLine(text.charAt(text.length() - 1) == '\n');
            suppressNewLines = false;
        }
    }

    @Override
    protected void printlnInternal(String text)
    {
        this.printInternal(text + '\n');
    }

    /**
     * Prints accumulated {@link #linePrefix line prefix}.
     */
    protected void printLinePrefix()
    {
        for (String prefix : linePrefix) {
            super.printInternal(prefix);
        }
    }

    @Override
    public void flush()
    {
        if (getBuffer().length() > 0) {
            escapeHandler.escape(getBuffer(), listenerChain.getBlockStateChainingListener());
            super.flush();
        }
    }

    /**
     * Strips new lines from the start of next printed text(s) until any
     * non-newline character.
     */
    public void suppressLeadingNewLines()
    {
        suppressNewLines = true;
    }

    /**
     * @param onNewLine if the next printed text should be considered as on new line
     */
    public void setOnNewLine(boolean onNewLine)
    {
        escapeHandler.setOnNewLine(onNewLine);
    }

    /**
     * @return if the last printed character was a new line
     */
    public boolean isOnNewLine()
    {
        return escapeHandler.isOnNewLine();
    }

    /**
     * @param escapeSpaces if spaces should be escaped
     */
    public void setEscapeSpaces(boolean escapeSpaces)
    {
        escapeHandler.setEscapeSpaces(escapeSpaces);
    }

    /**
     * Pushes the given sequence to the characters stack that will be printed
     * at the beginning of each new line.
     *
     * @param prefix the prefix to push
     */
    public void pushLinePrefix(String prefix)
    {
        linePrefix.add(prefix);
    }

    /**
     * Removes line prefix from the top of the stack.
     *
     * @see #pushLinePrefix(String)
     */
    public void popLinePrefix()
    {
        linePrefix.removeLast();
    }
}
