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

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.StackableChainingListener;

/**
 * Extended {@linkplain BlockStateChainingListener} with some enhancements for Markdown.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownBlockStateChainingListener extends BlockStateChainingListener
{
    /**
     * Stack for {@link ListType} of each level of a (nested) list.
     */
    private Deque<ListType> listType = new LinkedList<ListType>();


    /**
     * @param listenerChain the listener chain of which this listener is part
     */
    public MarkdownBlockStateChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }


    @Override
    public StackableChainingListener createChainingListenerInstance()
    {
        return new MarkdownBlockStateChainingListener(getListenerChain());
    }

    /**
     * @return type of the current list block
     */
    public ListType getListType()
    {
        return this.listType.peek();
    }

    @Override
    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.listType.push(listType);

        super.beginList(listType, parameters);
    }

    @Override
    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.listType.pop();

        super.endList(listType, parameters);
    }
}
