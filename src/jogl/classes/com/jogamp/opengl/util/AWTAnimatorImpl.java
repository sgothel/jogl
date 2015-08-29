/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.opengl.util;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import com.jogamp.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.AnimatorBase.UncaughtAnimatorException;

/** Abstraction to factor out AWT dependencies from the Animator's
    implementation in a way that still allows the FPSAnimator to pick
    up this behavior if desired. */

class AWTAnimatorImpl implements AnimatorBase.AnimatorImpl {
    // For efficient rendering of Swing components, in particular when
    // they overlap one another
    private final List<JComponent> lightweights = new ArrayList<JComponent>();
    private final Map<RepaintManager,RepaintManager> repaintManagers = new IdentityHashMap<RepaintManager,RepaintManager>();
    private final Map<JComponent,Rectangle>  dirtyRegions    = new IdentityHashMap<JComponent,Rectangle>();

    @Override
    public void display(final ArrayList<GLAutoDrawable> drawables,
                        final boolean ignoreExceptions,
                        final boolean printExceptions) throws UncaughtAnimatorException {
        boolean hasException = false;
        for (int i=0; !hasException && i<drawables.size(); i++) {
            GLAutoDrawable drawable = null;
            boolean catch1 = true;
            try {
                drawable = drawables.get(i);
                catch1 = false;
                if (drawable instanceof JComponent) {
                    // Lightweight components need a more efficient drawing
                    // scheme than simply forcing repainting of each one in
                    // turn since drawing one can force another one to be
                    // drawn in turn
                    lightweights.add((JComponent)drawable);
                } else {
                    drawable.display();
                }
            } catch (final Throwable t) {
                if( catch1 && t instanceof IndexOutOfBoundsException ) {
                    // concurrent pulling of GLAutoDrawables ..
                    hasException = true;
                } else if ( ignoreExceptions ) {
                    if ( printExceptions ) {
                        t.printStackTrace();
                    }
                } else {
                    throw new UncaughtAnimatorException(drawable, t);
                }
            }
        }
        if (lightweights.size() > 0) {
            try {
                SwingUtilities.invokeAndWait(drawWithRepaintManagerRunnable);
            } catch (final Throwable t) {
                t.printStackTrace();
            }
            lightweights.clear();
        }
    }

    // Uses RepaintManager APIs to implement more efficient redrawing of
    // the Swing widgets we're animating
    private final Runnable drawWithRepaintManagerRunnable = new Runnable() {
            @Override
            public void run() {
                for (final Iterator<JComponent> iter = lightweights.iterator(); iter.hasNext(); ) {
                    JComponent comp = iter.next();
                    RepaintManager rm = RepaintManager.currentManager(comp);
                    rm.markCompletelyDirty(comp);
                    repaintManagers.put(rm, rm);

                    // RepaintManagers don't currently optimize the case of
                    // overlapping sibling components. If we have two
                    // JInternalFrames in a JDesktopPane, the redraw of the
                    // bottom one will cause the top one to be redrawn as
                    // well. The top one will then be redrawn separately. In
                    // order to optimize this case we need to compute the union
                    // of all of the dirty regions on a particular JComponent if
                    // optimized drawing isn't enabled for it.

                    // Walk up the hierarchy trying to find a non-optimizable
                    // ancestor
                    final Rectangle visible = comp.getVisibleRect();
                    int x = visible.x;
                    int y = visible.y;
                    while (comp != null) {
                        x += comp.getX();
                        y += comp.getY();
                        final Component c = comp.getParent();
                        if ((c == null) || (!(c instanceof JComponent))) {
                            comp = null;
                        } else {
                            comp = (JComponent) c;
                            if (!comp.isOptimizedDrawingEnabled()) {
                                rm = RepaintManager.currentManager(comp);
                                repaintManagers.put(rm, rm);
                                // Need to dirty this region
                                Rectangle dirty = dirtyRegions.get(comp);
                                if (dirty == null) {
                                    dirty = new Rectangle(x, y, visible.width, visible.height);
                                    dirtyRegions.put(comp, dirty);
                                } else {
                                    // Compute union with already dirty region
                                    // Note we could compute multiple non-overlapping
                                    // regions: might want to do that in the future
                                    // (prob. need more complex algorithm -- dynamic
                                    // programming?)
                                    dirty.add(new Rectangle(x, y, visible.width, visible.height));
                                }
                            }
                        }
                    }
                }

                // Dirty any needed regions on non-optimizable components
                for (final Iterator<JComponent> iter = dirtyRegions.keySet().iterator(); iter.hasNext(); ) {
                    final JComponent comp = iter.next();
                    final Rectangle  rect = dirtyRegions.get(comp);
                    final RepaintManager rm = RepaintManager.currentManager(comp);
                    rm.addDirtyRegion(comp, rect.x, rect.y, rect.width, rect.height);
                }

                // Draw all dirty regions
                for (final Iterator<RepaintManager> iter = repaintManagers.keySet().iterator(); iter.hasNext(); ) {
                    iter.next().paintDirtyRegions();
                }
                dirtyRegions.clear();
                repaintManagers.clear();
            }
        };

    @Override
    public boolean blockUntilDone(final Thread thread) {
        return Thread.currentThread() != thread && !EventQueue.isDispatchThread();
    }
}
