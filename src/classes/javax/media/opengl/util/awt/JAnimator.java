/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package javax.media.opengl.util.awt;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.util.*;
import javax.swing.*;

import javax.media.opengl.*;
import javax.media.opengl.util.*;

/** <P> An Animator can be attached to one or more {@link
    GLAutoDrawable}s to drive their display() methods in a loop. </P>

    <P> The Animator class creates a background thread in which the
    calls to <code>display()</code> are performed. After each drawable
    has been redrawn, a brief pause is performed to avoid swamping the
    CPU, unless {@link #setRunAsFastAsPossible} has been called.  </P>
*/

public class JAnimator extends Animator {
  // For efficient rendering of Swing components, in particular when
  // they overlap one another
  private List lightweights    = new ArrayList();
  private Map  repaintManagers = new IdentityHashMap();
  private Map  dirtyRegions    = new IdentityHashMap();

  /** Creates a new, empty Animator. */
  public JAnimator() {
    super();
  }

  /** Creates a new Animator for a particular drawable. */
  public JAnimator(GLAutoDrawable drawable) {
    super(drawable);
  }

  /** Called every frame to cause redrawing of all of the
      GLAutoDrawables this Animator manages. Subclasses should call
      this to get the most optimized painting behavior for the set of
      components this Animator manages, in particular when multiple
      lightweight widgets are continually being redrawn. */
  protected void display() {
    Iterator iter = drawableIterator();
    while (iter.hasNext()) {
      GLAutoDrawable drawable = (GLAutoDrawable) iter.next();
      if (drawable instanceof JComponent) {
        // Lightweight components need a more efficient drawing
        // scheme than simply forcing repainting of each one in
        // turn since drawing one can force another one to be
        // drawn in turn
        lightweights.add(drawable);
      } else {
        try {
          drawable.display();
        } catch (RuntimeException e) {
          if (ignoreExceptions) {
            if (printExceptions) {
              e.printStackTrace();
            }
          } else {
            throw(e);
          }
        }
      }
    }
    if (lightweights.size() > 0) {
      try {
        SwingUtilities.invokeAndWait(drawWithRepaintManagerRunnable);
      } catch (Exception e) {
        e.printStackTrace();
      }
      lightweights.clear();
    }
  }

  /** Stops this animator. In most situations this method blocks until
      completion, except when called from the animation thread itself
      or in some cases from an implementation-internal thread like the
      AWT event queue thread. */
  public synchronized void stop() {
    shouldStop = true;
    notifyAll();
    // It's hard to tell whether the thread which calls stop() has
    // dependencies on the Animator's internal thread. Currently we
    // use a couple of heuristics to determine whether we should do
    // the blocking wait().
    if ((Thread.currentThread() == thread) || EventQueue.isDispatchThread()) {
      return;
    }
    while (shouldStop && thread != null) {
      try {
        wait();
      } catch (InterruptedException ie) {
      }
    }
  }

  // Uses RepaintManager APIs to implement more efficient redrawing of
  // the Swing widgets we're animating
  private Runnable drawWithRepaintManagerRunnable = new Runnable() {
      public void run() {
        for (Iterator iter = lightweights.iterator(); iter.hasNext(); ) {
          JComponent comp = (JComponent) iter.next();
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
          Rectangle visible = comp.getVisibleRect();
          int x = visible.x;
          int y = visible.y;
          while (comp != null) {
            x += comp.getX();
            y += comp.getY();
            Component c = comp.getParent();
            if ((c == null) || (!(c instanceof JComponent))) {
              comp = null;
            } else {
              comp = (JComponent) c;
              if (!comp.isOptimizedDrawingEnabled()) {
                rm = RepaintManager.currentManager(comp);
                repaintManagers.put(rm, rm);
                // Need to dirty this region
                Rectangle dirty = (Rectangle) dirtyRegions.get(comp);
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
        for (Iterator iter = dirtyRegions.keySet().iterator(); iter.hasNext(); ) {
          JComponent comp = (JComponent) iter.next();
          Rectangle  rect = (Rectangle) dirtyRegions.get(comp);
          RepaintManager rm = RepaintManager.currentManager(comp);
          rm.addDirtyRegion(comp, rect.x, rect.y, rect.width, rect.height);
        }

        // Draw all dirty regions
        for (Iterator iter = repaintManagers.keySet().iterator(); iter.hasNext(); ) {
          ((RepaintManager) iter.next()).paintDirtyRegions();
        }
        dirtyRegions.clear();
        repaintManagers.clear();
      }
    };
}
