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

import java.util.Map;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.internal.renderer.markdown11.reference.MarkdownResourceRenderer;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener.Event;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.StackableChainingListener;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;

import static org.apache.commons.lang3.StringUtils.containsNone;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.rightPad;

/**
 * Convert listener events to Markdown 1.1 output.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownChainingRenderer extends AbstractChainingPrintRenderer implements StackableChainingListener
{
    /**
     * Identifier of the macro to be rendered as a code block.
     */
    private static final String CODE_MACRO_ID = "code";

    /**
     * Indentation sequence for list items.
     */
    private static final String LIST_INDENT = repeat(' ', 4);

    private ResourceReferenceSerializer linkReferenceSerializer;

    private MarkdownResourceRenderer resourceRenderer;

    private MarkdownMacroRenderer macroRenderer;

    private MarkdownTableRenderer tableRenderer;

    private ComponentManager componentManager;

    /**
     * This state is used for list items rendering to decide whether to print a
     * empty line after the previous item when it contains paragraphs.
     */
    private boolean isParagraphPreceded;


    /**
     * @param listenerChain the listener chain of which this listener is part
     * @param linkReferenceSerializer
     * @param componentManager
     */
    public MarkdownChainingRenderer(ListenerChain listenerChain,
            ResourceReferenceSerializer linkReferenceSerializer, ComponentManager componentManager)
    {
        setListenerChain(listenerChain);

        this.linkReferenceSerializer = linkReferenceSerializer;
        this.componentManager = componentManager;
        this.resourceRenderer = new MarkdownResourceRenderer(this, linkReferenceSerializer, componentManager);
        this.macroRenderer = new MarkdownMacroRenderer(this);
        this.tableRenderer = new MarkdownTableRenderer(this);
    }

    @Override
    public StackableChainingListener createChainingListenerInstance()
    {
        MarkdownChainingRenderer renderer
            = new MarkdownChainingRenderer(getListenerChain(), linkReferenceSerializer, componentManager);
        renderer.setPrinter(getPrinter());

        return renderer;
    }


    //////// Document ////////

    @Override
    public void endDocument(MetaData metaData)
    {
        // Ensure that all data in the escape printer have been flushed
        getPrinter().flush();
    }


    //////// Header ////////

    @Override
    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        // Print 2 empty lines before H1-H3, one empty line for others
        printEmptyLines(level.getAsInt() < 4 ? 2 : 1);

        print(repeat("#", level.getAsInt()) + " ");
    }


    //////// Paragraph ////////

    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        printEmptyLine();
        isParagraphPreceded = true;
    }

    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getPrinter().flush();
    }


    //////// Format ////////

    @Override
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                print("**");
                break;
            case ITALIC:
                print("_");
                break;
            case MONOSPACE:
                print("`");
                break;
            case SUPERSCRIPT:
                print("^");
                getPrinter().setEscapeSpaces(true);
                break;
            case SUBSCRIPT:
                print("~");
                getPrinter().setEscapeSpaces(true);
                break;
            case STRIKEDOUT:
                print("<del>");
                break;
            // Underline should be used only for hyperlinks (typography)! Print bold instead.
            case UNDERLINED:
                print("**");
                break;
            case NONE:
                break;
            default:
                break;
        }
    }

    @Override
    public void endFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case SUPERSCRIPT:
                print("^");
                getPrinter().setEscapeSpaces(false);
                break;
            case SUBSCRIPT:
                print("~");
                getPrinter().setEscapeSpaces(false);
                break;
            case STRIKEDOUT:
                print("</del>");
                break;
            default:
                // Handle other formats in same way as a start of the block 
                beginFormat(format, parameters);
        }
    }


    //////// List ////////

    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        if (getBlockState().getListDepth() == 1) {
            printEmptyLine();
        } else {
            printNewLine();
        }
        // Reset state
        isParagraphPreceded = false;
    }

    @Override
    public void endList(ListType listType, Map <String, String> parameters)
    {
        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getPrinter().flush();
    }

    @Override
    public void beginListItem()
    {
        if (getBlockState().getListItemIndex() > 0) {
            printNewLine();

            // If the previous item includes paragraph, print empty line after it
            if (isParagraphPreceded) {
                printNewLine();
            }
        }

        if (getBlockState().getListType() == ListType.BULLETED) {
            print(rightPad("*", LIST_INDENT.length()));
            getPrinter().pushLinePrefix(LIST_INDENT);

        } else {
            print(rightPad("1.", LIST_INDENT.length()));
            getPrinter().pushLinePrefix(LIST_INDENT);
        }

        // Reset state
        isParagraphPreceded = false;
        getPrinter().suppressLeadingNewLines();
    }

    @Override
    public void endListItem()
    {
        getPrinter().popLinePrefix();
    }

    @Override
    public void beginDefinitionTerm()
    {
        printEmptyLine();
    }

    @Override
    public void endDefinitionTerm()
    {
        printNewLine();
    }

    @Override
    public void beginDefinitionDescription()
    {
        print(rightPad(":", LIST_INDENT.length()));
        getPrinter().pushLinePrefix(LIST_INDENT);
    }

    @Override
    public void endDefinitionDescription()
    {
        // Ensure that any not printed characters are flushed.
        // TODO: Fix this better by introducing a state listener to handle escapes
        getPrinter().flush();
        getPrinter().popLinePrefix();
    }


    //////// Table ////////

    @Override
    public void beginTable(Map<String, String> parameters)
    {
        // TODO
        getPrinter().setOnNewLine(false);
    }

    @Override
    public void endTable(Map<String, String> parameters)
    {
        tableRenderer.renderTable();
        tableRenderer.clear();
    }

    @Override
    public void beginTableRow(Map<String, String> parameters)
    {
        tableRenderer.beginTableRow();
    }

    @Override
    public void beginTableCell(Map <String, String> parameters)
    {
        // Defer printing the cell content since we need to gather all nested elements
        pushNewMarkdownPrinter();
    }

    @Override
    public void endTableCell(Map<String, String> parameters)
    {
        String cellContent = popMarkdownPrinter().toString();
        tableRenderer.addTableCell(cellContent, false, parameters);
    }

    @Override
    public void beginTableHeadCell(Map<String, String> parameters)
    {
        // Defer printing the cell content since we need to gather all nested elements
        pushNewMarkdownPrinter();
    }

    @Override
    public void endTableHeadCell(Map<String, String> parameters)
    {
        String cellContent = popMarkdownPrinter().toString();
        tableRenderer.addTableCell(cellContent, true, parameters);
    }


    //////// Macro ////////

    @Override
    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        //TODO
    }

    @Override
    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        //TODO
    }

    @Override
    public void onMacro(String id, Map<String, String> parameters, String content, boolean isInline)
    {
        if (!isInline) {
            printEmptyLine();
        }

        // Markdown has special syntax for code blocks
        if (CODE_MACRO_ID.equals(id)) {
            if (isInline) {
                macroRenderer.renderInlineCodeMacro(content);
            } else {
                macroRenderer.renderBlockCodeMacro(content, parameters.get("language"));
            }

        } else {
            if (isEmpty(content) || containsNone(content, '\n', ')')) {
                macroRenderer.renderMarkdownMacro(id, parameters, content);
            } else {
                macroRenderer.renderBlockMacro(id, parameters, content);
            }
        }
    }


    //////// Quotation ////////

    @Override
    public void beginQuotation(Map<String, String> parameters)
    {
        printNewLine();
        getPrinter().pushLinePrefix("> ");
        printNewLine();

        getPrinter().suppressLeadingNewLines();
    }

    @Override
    public void beginQuotationLine()
    {
        if (getBlockState().getPreviousEvent() != Event.QUOTATION) {
            printEmptyLine();
        }
    }

    @Override
    public void endQuotationLine()
    {
        getPrinter().flush();
    }

    @Override
    public void endQuotation(Map<String, String> parameters)
    {
        getPrinter().popLinePrefix();
    }

    //////// Link ////////

    @Override
    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        getPrinter().flush();

        // If we are at a depth of 2 or greater it means we're in a link inside a link and in this case we
        // shouldn't output the nested link as a link
        if (getBlockState().getLinkDepth() < 2 && !isFreeStandingURI) {
            // Defer printing the link content since we need to gather all nested elements
            pushNewMarkdownPrinter();
        }
    }

    @Override
    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        if (isFreeStandingURI) {
            resourceRenderer.renderFreeStandingURI(reference);
            
        // The links in a top level link label are not rendered as link (only the label is printed)
        } else if (getBlockState().getLinkDepth() == 1) {
            String label = popMarkdownPrinter().toString();
            resourceRenderer.renderLinkReference(reference, label, parameters);
        }
    }

    @Override
    public void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        resourceRenderer.renderImageReference(reference, parameters);
    }

    @Override
    public void onNewLine()
    {
        if (getBlockState().isInLine()) {
            print("  \n"); // TODO delayed?
        } else {
            printNewLine();
        }
    }

    @Override
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        if (isInline) {
            macroRenderer.renderInlineCodeMacro(protectedString);
        } else {
            macroRenderer.renderBlockCodeMacro(protectedString, parameters.get("language"));
        }
    }

    @Override
    public void onRawText(String text, Syntax syntax)
    {
        if (!getBlockState().isInLine()) {
            printEmptyLine();
        }
        print(text);
    }

    @Override
    public void onHorizontalLine(Map<String, String> parameters)
    {
        printEmptyLine();
        print("---");
    }

    @Override
    public void onEmptyLines(int count)
    {
        print(repeat('\n', count));
    }

    @Override
    public void onWord(String word)
    {
        printDelayed(word);
    }

    @Override
    public void onSpace()
    {
        printDelayed(" ");
    }

    @Override
    public void onSpecialSymbol(char symbol)
    {
        printDelayed("" + symbol);
    }

    @Override
    public void onId(String name)
    {
        //TODO ?
    }


    //////// Print methods ////////

    /**
     * Prints one empty line.
     */
    protected void printEmptyLine()
    {
        printEmptyLines(1);
    }

    /**
     * Prints {@code count} of empty lines.
     *
     * @param count number of empty lines to print
     */
    protected void printEmptyLines(int count)
    {
        print(repeat('\n', count + 1));
    }

    /**
     * Prints a new line.
     */
    protected void printNewLine()
    {
        print("\n");
    }

    /**
     * Appends the given text to printer's buffer so it can be reviewed and escaped before actually
     * printed. To flush the buffer call {@link MarkdownWikiPrinter#flush() getPrinter().flush()}.
     *
     * @param text string to be printed
     */
    protected void printDelayed(String text)
    {
        print(text, true);
    }

    /**
     * Prints the given text "directly", i.e. without reviewing and escaping.
     *
     * @param text string to be printed
     */
    protected void print(String text)
    {
        print(text, false);
    }

    /**
     * @param text string to be printed
     * @param isDelayed see {@link #printDelayed(String)}
     */
    private void print(String text, boolean isDelayed)
    {
        if (isDelayed) {
/*            if (getListenerChain().getMarkdownEscapeChainingListener().isReadyToFlush()) {
                getPrinter().flush();
            }*/
            getPrinter().printDelayed(text);
            //getListenerChain().getMarkdownEscapeChainingListener().reset();
        } else {
            getPrinter().print(text);
        }
    }


    @Override
    public MarkdownWikiPrinter getPrinter()
    {
        return (MarkdownWikiPrinter) super.getPrinter();
    }

    @Override
    public void setPrinter(WikiPrinter printer)
    {
        // If the printer is already a Markdown printer don't wrap it again. This case happens when
        // the createChainingListenerInstance() method is called, ie when this renderer's state is stacked
        // (for example when a Group event is being handled).
        if (printer instanceof MarkdownWikiPrinter) {
            super.setPrinter(printer);
        } else {
            super.setPrinter(new MarkdownWikiPrinter(printer, getListenerChain()));
        }
    }

    @Override
    protected void popPrinter()
    {
        // Ensure that any not printed characters are flushed
        getPrinter().flush();

        super.popPrinter();
    }

    /**
     * Push new instance of {@link MarkdownWikiPrinter} onto the top of the
     * {@linkplain org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#printers printers stack}.
     */
    protected void pushNewMarkdownPrinter()
    {
        MarkdownWikiPrinter newPrinter
            = new MarkdownWikiPrinter(new DefaultWikiPrinter(), getListenerChain());

        // Make sure the escape handler knows there are already characters before
        newPrinter.setOnNewLine(getPrinter().isOnNewLine());

        pushPrinter(newPrinter);
    }

    /**
     * Removes the current {@link MarkdownWikiPrinter} from the top of the
     * {@linkplain org.xwiki.rendering.renderer.AbstractChainingPrintRenderer#printers printers stack}
     * (will be flushed before) and instead sets the previous printer as active.
     *
     * @return the removed printer
     */
    protected MarkdownWikiPrinter popMarkdownPrinter()
    {
        getPrinter().flush();
        MarkdownWikiPrinter printer = getPrinter();
        popPrinter();

        return printer;
    }

    @Override
    public MarkdownListenerChain getListenerChain()
    {
        return (MarkdownListenerChain) super.getListenerChain();
    }

    /**
     * @return the stateful {@link MarkdownBlockStateChainingListener} for this rendering session
     */
    protected MarkdownBlockStateChainingListener getBlockState()
    {
        return getListenerChain().getBlockStateChainingListener();
    }

    /**
     * @return the {@link org.xwiki.rendering.listener.chaining.EventType} of the next event
     *
     * @see org.xwiki.rendering.listener.chaining.LookaheadChainingListener
     */
    protected EventType peekNextEventType()
    {
        return getListenerChain().getLookaheadChainingListener().getNextEvent().eventType;
    }
}
