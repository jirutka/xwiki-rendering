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
package org.xwiki.rendering.internal.renderer.markdown11.reference;

import java.util.Map;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.internal.renderer.markdown11.AbstractPrintRenderer;
import org.xwiki.rendering.internal.renderer.markdown11.MarkdownEscapeHandler;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;
import org.xwiki.rendering.renderer.reference.link.URILabelGenerator;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Logic to render a Resource Reference into Markdown.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownResourceRenderer extends AbstractPrintRenderer
{
    private static final String TITLE_PARAM = "title";

    private static final String ALT_PARAM = "alt";

    private ResourceReferenceSerializer referenceSerializer;

    private ComponentManager componentManager;


    public MarkdownResourceRenderer(PrintRenderer printRenderer,
            ResourceReferenceSerializer referenceSerializer, ComponentManager componentManager)
    {
        super(printRenderer);
        this.referenceSerializer = referenceSerializer;
        this.componentManager = componentManager;
    }


    public String serialize(ResourceReference reference)
    {
        return referenceSerializer.serialize(reference);
    }

    public void renderLinkReference(ResourceReference reference, String label, Map<String, String> parameters)
    {
        // Standard inline link
        if (isNotEmpty(label)) {
            String title = parameters.get(TITLE_PARAM);
            String escapedLabel = MarkdownEscapeHandler.escapeLabel(label);
            String escapedRef = MarkdownEscapeHandler.escapeReference(serialize(reference));

            // Print label
            print("[" + escapedLabel + "]");

            // Print reference
            print("(" + escapedRef);
            if (isNotBlank(title)) {
                print(" \"" + title + "\"");
            }
            print(")");

        // WikiLink
        } else {
            String escapedRef = MarkdownEscapeHandler.escapeLabel(serialize(reference));
            print("[[" + escapedRef + "]]");
        }
    }

    public void renderFreeStandingURI(ResourceReference reference)
    {
        print(serialize(reference));
    }

    public void renderImageReference(ResourceReference reference, Map<String, String> parameters)
    {
        String altText = parameters.get(ALT_PARAM);
        if (isBlank(altText)) {
            altText = computeAltAttributeValue(reference);
        }
        String title = parameters.get(TITLE_PARAM);
        String escapedLabel = MarkdownEscapeHandler.escapeLabel(altText);
        String escapedRef = MarkdownEscapeHandler.escapeReference(serialize(reference));

        // Print label
        print("![" + escapedLabel + "]");

        // Print reference
        print("(" + escapedRef);
        if (isNotBlank(title)) {
            print(" \"" + title + "\"");
        }
        print(")");
    }

    /**
     * @param reference the reference for which to compute the alt attribute value
     * @return the generated alt attribute value
     */
    private String computeAltAttributeValue(ResourceReference reference)
    {
        String label;
        try {
            URILabelGenerator uriLabelGenerator = componentManager.getInstance(URILabelGenerator.class,
                    reference.getType().getScheme());
            label = uriLabelGenerator.generateLabel(reference);

        } catch (ComponentLookupException ex) {
            label = reference.getReference();
        }
        return label;
    }
}
