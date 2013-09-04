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

import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;

/**
 * Provides convenient access to listeners in the chain used for
 * {@link org.xwiki.rendering.internal.renderer.markdown11.MarkdownChainingRenderer}.
 *
 * @version $Id $
 * @since 5.2M1
 */
public class MarkdownListenerChain extends ListenerChain
{
    /**
     * @return the stateful {@link org.xwiki.rendering.listener.chaining.LookaheadChainingListener} for this rendering session.
     */
    public LookaheadChainingListener getLookaheadChainingListener()
    {
        return (LookaheadChainingListener) getListener(LookaheadChainingListener.class);
    }

    /**
     * @return the stateful {@link MarkdownBlockStateChainingListener} for this rendering session.
     */
    public MarkdownBlockStateChainingListener getBlockStateChainingListener()
    {
        return (MarkdownBlockStateChainingListener) getListener(MarkdownBlockStateChainingListener.class);
    }
}
