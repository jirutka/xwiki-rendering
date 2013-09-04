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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.xwiki.rendering.renderer.PrintRenderer;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.util.Collections.nCopies;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.rightPad;

/**
 * Generates Markdown for a Table.
 * 
 * TODO should be improved
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownTableRenderer extends AbstractPrintRenderer
{
    private static final String ALIGN_PARAM = "align";

    private static final String COLSPAN_PARAM = "colspan";

    private static final int TABLE_WIDTH_STOP = 100;

    /**
     * Minimal padding around cell content (number of spaces).
     */
    private static final int MIN_PADDING = 1;

    private static final double REPEATED_WIDTH_BONUS = 0.8;

    /**
     * The table's content; first dimension is row, second is cell.
     */
    private List<List<String>> table = new ArrayList<List<String>>();

    /**
     * Helper matrix for computing column widths.
     * First dimension is column (1-based), second is width with rank.
     */
    private List<NavigableMap<Integer, Double>> rankedColumnWidths = new ArrayList<NavigableMap<Integer, Double>>();

    /**
     * Columns alignment map; key stands for the column number (0-based), value for its alignment.
     */
    private Map<Integer, Alignment> aligns = new HashMap<Integer, Alignment>();

    /**
     * Number of rows of the top header.
     */
    private int headerRows;


    /**
     * @param printRenderer the renderer to obtain an instance of
     *        {@link org.xwiki.rendering.renderer.printer.WikiPrinter}
     */
    public MarkdownTableRenderer(PrintRenderer printRenderer)
    {
        super(printRenderer);
    }

    /**
     * Prepares renderer for the next row. This method should be called on
     * {@link org.xwiki.rendering.listener.Listener#beginTableRow(java.util.Map) beginTableRow()} event.
     */
    public void beginTableRow()
    {
        int capacity = table.isEmpty() ? 4 : peekColumn();

        table.add(new ArrayList<String>(capacity));

        if (isHeaderRow(peekRow() -1)) {
            aligns.clear();
        }
    }

    /**
     * Adds new cell to the current row.
     *
     * @param content the cell content
     * @param isHeader {@literal true} if the cell is header (ie. {@code <th>} in HTML)
     * @param params a generic list of parameters for the table cell
     */
    public void addTableCell(String content, boolean isHeader, Map<String, String> params)
    {
        List<String> cells = table.get(peekRow());

        // Parse parameters
        Alignment align = Alignment.parse(params.get(ALIGN_PARAM));
        int colspan = params.containsKey(COLSPAN_PARAM)
                ? parseInt(params.get(COLSPAN_PARAM)) : 1;
        content = content.trim();

        updateMetadata(content, align, isHeader);

        cells.add(content);
        cells.addAll(nCopies(colspan -1, (String) null));  // add null for every spanned column
    }

    public void renderTable()
    {
        int[] widths = computeColumnsWidth(rankedColumnWidths);
        boolean isHeaderPrinted = false;

        // Print rows
        for (int row = 0; row < table.size(); row++) {
            List<String> cells = table.get(row);
            int overflow = 0;

            // Print header if not printed yet
            if (!isHeaderPrinted && !isHeaderRow(row)) {
                renderHeaderDivider(widths);
                isHeaderPrinted = true;
            }

            // Print the leading pipe
            print("|");

            // Print cells
            for (int col = 0; col < cells.size(); col++) {
                String cell = cells.get(col);
                int paddingSize = widths[col] - overflow;
                int colspan = 1;

                // Compute padding across spanned columns and increment col index
                for (; col +1 < cells.size() && cells.get(col +1) == null; col++) {
                    paddingSize += widths[col +1];
                    colspan++;                }

                // Add padding
                String content = repeat(' ', MIN_PADDING) + cell;
                content = rightPad(content, paddingSize - MIN_PADDING);
                content += repeat(' ', MIN_PADDING);

                print(content);
                print(repeat('|', colspan));

                overflow = max(cell.length() - paddingSize, 0);  // must not be negative
            }

            print("\n");
        }
    }

    public void clear()
    {
        table.clear();
        rankedColumnWidths.clear();
        aligns.clear();
        headerRows = 0;
    }

    private void updateMetadata(String content, Alignment align, boolean isHeaderCell)
    {
        int row = peekRow();
        int col = peekColumn();

        if (! aligns.containsKey(col)) {
            aligns.put(col, align);
        }

        // If the first cell in the row is a header, mark the row as a header.
        if (isHeaderCell && peekColumn() == 0) {
            headerRows = row +1;
        }
        // If the cell is *not* a header, but the row was marked as a header,
        // take it back (Pegdown/Markdown supports only horizontal header).
        if (!isHeaderCell && row +1 == headerRows) {
            headerRows -= 1;
        }

        // Increase list size if needed
        if (rankedColumnWidths.size() <= col) {
            rankedColumnWidths.add(new TreeMap<Integer, Double>());
        }
        NavigableMap<Integer, Double> rankedLengths = rankedColumnWidths.get(col);
        int length = content.length();

        double rank = length;
        if (rankedLengths.containsKey(length)) {
            rank = rankedLengths.get(length) * REPEATED_WIDTH_BONUS;
        }

        rankedLengths.put(length, rank);
    }

    /**
     * @return the current row number
     */
    private int peekRow()
    {
        return table.size() -1;
    }

    /**
     * @return the current column number
     */
    private int peekColumn()
    {
        return table.get(peekRow()).size();
    }

    /**
     * @param row the row number
     * @return {@literal true} if the given row number is header
     */
    private boolean isHeaderRow(int row)
    {
        return headerRows > row;
    }

    private int[] computeColumnsWidth(List<NavigableMap<Integer, Double>> data)
    {
        while (true) {
            int tableWidth = 0;
            int maxCol = -1;
            double maxRank = 0;

            for (int col = 0; col < data.size(); col++) {
                NavigableMap.Entry<Integer, Double> entry = data.get(col).lastEntry();

                tableWidth += entry.getKey();   // size
                double rank = entry.getValue(); // ranked size

                if (rank > maxRank) {
                    maxRank = rank;
                    maxCol = col;
                }
            }
            if (tableWidth > TABLE_WIDTH_STOP) {
                data.get(maxCol).pollLastEntry();
            } else {
                break;
            }
        }

        int[] sizes = new int[data.size()];
        for (int col = 0; col < sizes.length; col++) {
            sizes[col] = data.get(col).lastKey() + 2 * MIN_PADDING;
        }
        return sizes;
    }

    private void renderHeaderDivider(int[] columnsWidth)
    {
        print("|");  // leading pipe

        for (int col = 0; col < columnsWidth.length; col++) {
            Alignment align = aligns.get(col);
            char[] line = repeat('-', columnsWidth[col]).toCharArray();

            if (align == Alignment.LEFT || align == Alignment.CENTER) {
                line[0] = ':';
            }
            if (align == Alignment.RIGHT || align == Alignment.CENTER) {
                line[line.length -1] = ':';
            }
            print(new String(line));
            print("|");
        }
        print("\n");
    }


    static enum Alignment
    {
        LEFT, RIGHT, CENTER, DEFAULT;

        public static Alignment parse(String value)
        {
            for (Alignment type : EnumSet.allOf(Alignment.class)) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return DEFAULT;
        }
    }
}
