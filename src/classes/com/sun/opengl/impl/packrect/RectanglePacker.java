/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl.packrect;

import java.util.*;

/** Packs rectangles supplied by the user (typically representing
    image regions) into a larger backing store rectangle (typically
    representing a large texture). Supports automatic compaction of
    the space on the backing store, and automatic expansion of the
    backing store, when necessary. */

public class RectanglePacker {
  private BackingStoreManager manager;
  private Object backingStore;
  private LevelSet levels;
  private float EXPANSION_FACTOR = 0.5f;

  static class RectHComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      Rect r1 = (Rect) o1;
      Rect r2 = (Rect) o2;
      return r2.h() - r1.h();
    }

    public boolean equals(Object obj) {
      return this == obj;
    }
  }
  private static final Comparator rectHComparator = new RectHComparator();

  public RectanglePacker(BackingStoreManager manager,
                         int initialWidth,
                         int initialHeight) {
    this.manager = manager;
    levels = new LevelSet(initialWidth, initialHeight);
  }

  public Object getBackingStore() {
    if (backingStore == null) {
      backingStore = manager.allocateBackingStore(levels.w(), levels.h());
    }

    return backingStore;
  }

  /** Decides upon an (x, y) position for the given rectangle (leaving
      its width and height unchanged) and places it on the backing
      store. May provoke re-layout of other Rects already added. */
  public void add(Rect rect) {
    // Allocate backing store if we don't have any yet
    if (backingStore == null)
      backingStore = manager.allocateBackingStore(levels.w(), levels.h());

    // Try to allocate
    if (levels.add(rect))
      return;

    // Try to allocate with compaction
    if (levels.compactAndAdd(rect, backingStore, manager))
      return;

    // Have to expand. Need to figure out what direction to go. Prefer
    // to expand vertically. Expand horizontally only if rectangle
    // being added is too wide. FIXME: may want to consider
    // rebalancing the width and height to be more equal if it turns
    // out we keep expanding in the vertical direction.
    boolean done = false;
    int newWidth = levels.w();
    int newHeight = levels.h();
    LevelSet nextLevelSet = null;
    while (!done) {
      if (rect.w() > newWidth) {
        newWidth = rect.w();
      } else {
        newHeight = (int) (newHeight * (1.0f + EXPANSION_FACTOR));
      }
      nextLevelSet = new LevelSet(newWidth, newHeight);
      
      // Make copies of all existing rectangles
      List/*<Rect>*/ newRects = new ArrayList/*<Rect>*/();
      for (Iterator i1 = levels.iterator(); i1.hasNext(); ) {
        Level level = (Level) i1.next();
        for (Iterator i2 = level.iterator(); i2.hasNext(); ) {
          Rect cur = (Rect) i2.next();
          Rect newRect = new Rect(0, 0, cur.w(), cur.h(), null);
          cur.setNextLocation(newRect);
          // Hook up the reverse mapping too for easier replacement
          newRect.setNextLocation(cur);
          newRects.add(newRect);
        }
      }
      // Sort them by decreasing height (note: this isn't really
      // guaranteed to improve the chances of a successful layout)
      Collections.sort(newRects, rectHComparator);
      // Try putting all of these rectangles into the new level set
      done = true;
      for (Iterator iter = newRects.iterator(); iter.hasNext(); ) {
        if (!nextLevelSet.add((Rect) iter.next())) {
          done = false;
          break;
        }
      }
    }
    // OK, now we have a new layout and a mapping from the old to the
    // new locations of rectangles on the backing store. Allocate a
    // new backing store, move the contents over and deallocate the
    // old one.
    Object newBackingStore = manager.allocateBackingStore(nextLevelSet.w(),
                                                          nextLevelSet.h());
    manager.beginMovement(backingStore, newBackingStore);
    for (Iterator i1 = levels.iterator(); i1.hasNext(); ) {
      Level level = (Level) i1.next();
      for (Iterator i2 = level.iterator(); i2.hasNext(); ) {
        Rect cur = (Rect) i2.next();
        manager.move(backingStore, cur,
                     newBackingStore, cur.getNextLocation());
      }
    }
    // Replace references to temporary rectangles with original ones
    nextLevelSet.updateRectangleReferences();
    manager.endMovement(backingStore, newBackingStore);
    // Now delete the old backing store
    manager.deleteBackingStore(backingStore);
    // Update to new versions of backing store and LevelSet
    backingStore = newBackingStore;
    levels = nextLevelSet;
    // Retry the addition of the incoming rectangle
    add(rect);
    // Done
  }

  /** Removes the given rectangle from this RectanglePacker. */
  public void remove(Rect rect) {
    levels.remove(rect);
  }

  /** Visits all Rects contained in this RectanglePacker. */
  public void visit(RectVisitor visitor) {
    levels.visit(visitor);
  }

  /** Disposes the backing store allocated by the
      BackingStoreManager. This RectanglePacker may no longer be used
      after calling this method. */
  public void dispose() {
    manager.deleteBackingStore(backingStore);
    backingStore = null;
    levels = null;
  }
}
