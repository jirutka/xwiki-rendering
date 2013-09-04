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

import org.xwiki.rendering.internal.renderer.ParametersPrinter;
import org.xwiki.rendering.renderer.PrintRenderer;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.xwiki.rendering.internal.renderer.markdown11.EscapeUtils.ESCAPE_CHAR;

/**
 * Generates Markdown for a Macro Block.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownMacroRenderer extends AbstractPrintRenderer
{
    private final ParametersPrinter parametersPrinter = new MarkdownParametersPrinter();

    /**
     * @param printRenderer the renderer to obtain an instance of
     *        {@link org.xwiki.rendering.renderer.printer.WikiPrinter}
     */
    public MarkdownMacroRenderer(PrintRenderer printRenderer)
    {
        super(printRenderer);
    }

    /**
     * Generates XWiki-style macro syntax.
     * 
     * <p>Examples:
     * <pre>
     *   {{mymacro /}}
     *   {{mymacro par1="val1" par2=2 /}}
     * 
     *   {{mymacro}}
     *   some content
     *   {{/mymacro}}
     * 
     *   {{mymacro par1="val1" par2=2}}
     *   some content
     *   {{/mymacro}}
     * </pre></p>
     *
     * @param id the macro id (eg "toc" for the TOC macro)
     * @param parameters the macro parameters
     * @param content the macro content
     */
    public void renderBlockMacro(String id, Map<String, String> parameters, String content)
    {
        print("{{" + id);  // begin start tag

        if (!parameters.isEmpty()) {
            print(" ");
            print(parametersPrinter.print(parameters, ESCAPE_CHAR));  // add parameters
        }

        if (isEmpty(content)) {
            print("/}}");  // end start tag as empty-element

        } else {
            print("}}");               // end start tag
            print("\n" + content);     // content
            print("{{/" + id + "}}");  // end tag
        }
    }

    /**
     * Generates Markdown-style macro syntax.
     * 
     * <p>Examples:
     * <pre>
     *   #[mymacro]
     *   #[mymacro](par1="val1" par2=2)
     *   #[mymacro](some content)
     *   #[mymacro](par1="val1" par2=2 "some content")
     * </pre></p>
     *
     * @param id the macro id (eg "toc" for the TOC macro)
     * @param parameters the macro parameters
     * @param content the macro content
     */
    public void renderMarkdownMacro(String id, Map<String, String> parameters, String content)
    {
        print("#[" + id + "]");  // macro label

        if (!parameters.isEmpty() || isNotEmpty(content)) {
            print("(");
            
            if (!parameters.isEmpty()) {
                print(parametersPrinter.print(parameters, ESCAPE_CHAR));
            }
            if (!parameters.isEmpty() && isNotEmpty(content)) {
                print(" ");
                
                print('"' + content + '"');
                
            } else if (parameters.isEmpty()) {
                print(content);
            }
            print(")");
        }
    }

    /**
     * Generates so called "Fenced code block" (like in GitHub's Flavoured Markdown).
     *
     * <p>Example:
     * <pre>
     *   ```java
     *   void hello() {
     *       print("Hello world!");
     *   }
     *   ```
     * </pre></p>
     *
     * @param content
     * @param lang language of the content (e.g. <tt>xml, ruby, java</tt>, ...),
     *             or {@code null} if undefined
     */
    public void renderBlockCodeMacro(String content, String lang)
    {
        print("```");
        if (isNotEmpty(lang)) {
            print(lang);
        }
        print("\n" + content + "\n");
        print("```");
    }

    /**
     * Examples:
     * <pre>
     *   Call the method `run()` to see some magic.
     *   Rampa ``Mc`Quack``
     * </pre>
     *
     * @param content
     */
    public void renderInlineCodeMacro(String content)
    {
        String marker = content.contains("`") ? "``" : "`";

        print(marker + content + marker);
    }
}
