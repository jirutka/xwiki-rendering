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

import org.xwiki.rendering.renderer.PrintRenderer;

/**
 * Base class for renderers called from {@link MarkdownChainingRenderer}.
 *
 * @version $Id $
 * @since 5.2M1
 */
public abstract class AbstractPrintRenderer
{
    private final PrintRenderer printRenderer;

    /**
     * @param printRenderer the renderer to obtain an instance of
     *                      {@link org.xwiki.rendering.renderer.printer.WikiPrinter}
     */
    public AbstractPrintRenderer(PrintRenderer printRenderer)
    {
        this.printRenderer = printRenderer;
    }

    /**
     * @param text print the given string via {@link org.xwiki.rendering.renderer.printer.WikiPrinter}
     *             provided by the {@link org.xwiki.rendering.renderer.PrintRenderer}
     */
    protected void print(String text)
    {
        printRenderer.getPrinter().print(text);
    }
}
