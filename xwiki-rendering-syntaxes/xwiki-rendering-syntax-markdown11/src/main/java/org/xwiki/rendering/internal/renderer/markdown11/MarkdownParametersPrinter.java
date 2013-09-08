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

import org.xwiki.rendering.internal.renderer.ParametersPrinter;

import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 * Generates syntax for a parameters group like macros and links.
 * Unlike {@link org.xwiki.rendering.internal.renderer.ParametersPrinter} this doesn't quote numeric parameters.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownParametersPrinter extends ParametersPrinter
{
    protected static final String QUOTE = "\"";


    @Override
    public String print(String parameterName, String parameterValue, char escapeChar)
    {
        // escape the escaping character
        String value = parameterValue.replace("" + escapeChar, "" + escapeChar + escapeChar);
        // escape quote
        value = value.replace(QUOTE, escapeChar + QUOTE);

        String result = parameterName + "=";
        if (isNumeric(value)) {
            return result += value;
        } else {
            return result += QUOTE + value + QUOTE;
        }
    }
}
