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

package com.jogamp.opengl.util.packrect;

import java.util.*;

/** Manages a list of Levels; this is the core data structure
    contained within the RectanglePacker and encompasses the storage
    algorithm for the contained Rects. */

public class LevelSet {
  // Maintained in sorted order by increasing Y coordinate
  private final List<Level> levels = new ArrayList<Level>();
  private int nextAddY;
  private final int w;
  private int h;

  /** A LevelSet manages all of the backing store for a region of a
      specified width and height. */
  public LevelSet(final int w, final int h) {
    this.w = w;
    this.h = h;
  }

  public int w() { return w; }
  public int h() { return h; }

  /** Returns true if the given rectangle was successfully added to
      the LevelSet given its current dimensions, false if not. Caller
      is responsible for performing compaction, expansion, etc. as a
      consequence. */
  public boolean add(final Rect rect) {
    if (rect.w() > w)
      return false;

    // Go in reverse order through the levels seeing whether we can
    // trivially satisfy the allocation request
    for (int i = levels.size() - 1; i >= 0; --i) {
      final Level level = levels.get(i);
      if (level.add(rect))
        return true;
    }

    // See whether compaction could satisfy this allocation. This
    // increases the computational complexity of the addition process,
    // but prevents us from expanding unnecessarily.
    for (int i = levels.size() - 1; i >= 0; --i) {
      final Level level = levels.get(i);
      if (level.couldAllocateIfCompacted(rect))
        return false;
    }

    // OK, we need to either add a new Level or expand the backing
    // store. Try to add a new Level.
    if (nextAddY + rect.h() > h)
      return false;

    final Level newLevel = new Level(w, rect.h(), nextAddY, this);
    levels.add(newLevel);
    nextAddY += rect.h();
    final boolean res = newLevel.add(rect);
    if (!res)
      throw new RuntimeException("Unexpected failure in addition to new Level");
    return true;
  }

  /** Removes the given Rect from this LevelSet. */
  public boolean remove(final Rect rect) {
    for (int i = levels.size() - 1; i >= 0; --i) {
      final Level level = levels.get(i);
      if (level.remove(rect))
        return true;
    }

    return false;
  }

  /** Allocates the given Rectangle, performing compaction of a Level
      if necessary. This is the correct fallback path to {@link
      #add(Rect)} above. Returns true if allocated successfully, false
      otherwise (indicating the need to expand the backing store). */
  public boolean compactAndAdd(final Rect rect,
                               final Object backingStore,
                               final BackingStoreManager manager) {
    for (int i = levels.size() - 1; i >= 0; --i) {
      final Level level = levels.get(i);
      if (level.couldAllocateIfCompacted(rect)) {
        level.compact(backingStore, manager);
        final boolean res = level.add(rect);
        if (!res)
          throw new RuntimeException("Unexpected failure to add after compaction");
        return true;
      }
    }

    return false;
  }

  /** Indicates whether it's legal to trivially increase the height of
      the given Level. This is only possible if it's the last Level
      added and there's enough room in the backing store. */
  public boolean canExpand(final Level level, final int height) {
    if (levels.isEmpty())
      return false; // Should not happen
    if (levels.get(levels.size() - 1) == level &&
        (h - nextAddY >= height - level.h()))
      return true;
    return false;
  }

  public void expand(final Level level, final int oldHeight, final int newHeight) {
    nextAddY += (newHeight - oldHeight);
  }

  /** Gets the used height of the levels in this LevelSet. */
  public int getUsedHeight() {
    return nextAddY;
  }

  /** Sets the height of this LevelSet. It is only legal to reduce the
      height to greater than or equal to the currently used height. */
  public void setHeight(final int height) throws IllegalArgumentException {
    if (height < getUsedHeight()) {
      throw new IllegalArgumentException("May not reduce height below currently used height");
    }
    h = height;
  }

  /** Returns the vertical fragmentation ratio of this LevelSet. This
      is defined as the ratio of the sum of the heights of all
      completely empty Levels divided by the overall used height of
      the LevelSet. A high vertical fragmentation ratio indicates that
      it may be profitable to perform a compaction. */
  public float verticalFragmentationRatio() {
    int freeHeight = 0;
    final int usedHeight = getUsedHeight();
    if (usedHeight == 0)
      return 0.0f;
    for (final Iterator<Level> iter = iterator(); iter.hasNext(); ) {
      final Level level = iter.next();
      if (level.isEmpty()) {
        freeHeight += level.h();
      }
    }
    return (float) freeHeight / (float) usedHeight;
  }

  public Iterator<Level> iterator() {
    return levels.iterator();
  }

  /** Visits all Rects contained in this LevelSet. */
  public void visit(final RectVisitor visitor) {
    for (final Iterator<Level> iter = levels.iterator(); iter.hasNext(); ) {
      final Level level = iter.next();
      level.visit(visitor);
    }
  }

  /** Updates the references to the Rect objects in this LevelSet with
      the "next locations" of those Rects. This is actually used to
      update the new Rects in a newly laid-out LevelSet with the
      original Rects. */
  public void updateRectangleReferences() {
    for (final Iterator<Level> iter = levels.iterator(); iter.hasNext(); ) {
      final Level level = iter.next();
      level.updateRectangleReferences();
    }
  }

  /** Clears out all Levels stored in this LevelSet. */
  public void clear() {
    levels.clear();
    nextAddY = 0;
  }
}
