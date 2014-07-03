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

public class Level {
  private final int width;
  private int height;
  private final int yPos;
  private final LevelSet holder;

  private final List<Rect> rects = new ArrayList<Rect>();
  private List<Rect> freeList;
  private int nextAddX;

  static class RectXComparator implements Comparator<Rect> {
    @Override
    public int compare(final Rect r1, final Rect r2) {
      return r1.x() - r2.x();
    }

    @Override
    public boolean equals(final Object obj) {
      return this == obj;
    }
  }
  private static final Comparator<Rect> rectXComparator = new RectXComparator();

  public Level(final int width, final int height, final int yPos, final LevelSet holder) {
    this.width = width;
    this.height = height;
    this.yPos = yPos;
    this.holder = holder;
  }

  public int w()    { return width;  }
  public int h()    { return height; }
  public int yPos() { return yPos;   }

  /** Tries to add the given rectangle to this level only allowing
      non-disruptive changes like trivial expansion of the last level
      in the RectanglePacker and allocation from the free list. More
      disruptive changes like compaction of the level must be
      requested explicitly. */
  public boolean add(final Rect rect) {
    if (rect.h() > height) {
      // See whether it's worth trying to expand vertically
      if (nextAddX + rect.w() > width) {
        return false;
      }

      // See whether we're the last level and can expand
      if (!holder.canExpand(this, rect.h())) {
        return false;
      }

      // Trivially expand and try the allocation
      holder.expand(this, height, rect.h());
      height = rect.h();
    }

    // See whether we can add at the end
    if (nextAddX + rect.w() <= width) {
      rect.setPosition(nextAddX, yPos);
      rects.add(rect);
      nextAddX += rect.w();
      return true;
    }

    // See whether we can add from the free list
    if (freeList != null) {
      Rect candidate = null;
      for (final Iterator<Rect> iter = freeList.iterator(); iter.hasNext(); ) {
        final Rect cur = iter.next();
        if (cur.canContain(rect)) {
          candidate = cur;
          break;
        }
      }

      if (candidate != null) {
        // Remove the candidate from the free list
        freeList.remove(candidate);
        // Set up and add the real rect
        rect.setPosition(candidate.x(), candidate.y());
        rects.add(rect);
        // Re-add any remaining free space
        if (candidate.w() > rect.w()) {
          candidate.setPosition(candidate.x() + rect.w(), candidate.y());
          candidate.setSize(candidate.w() - rect.w(), height);
          freeList.add(candidate);
        }

        coalesceFreeList();

        return true;
      }
    }

    return false;
  }

  /** Removes the given Rect from this Level. */
  public boolean remove(final Rect rect) {
    if (!rects.remove(rect))
      return false;

    // If this is the rightmost rectangle, instead of adding its space
    // to the free list, we can just decrease the nextAddX
    if (rect.maxX() + 1 == nextAddX) {
      nextAddX -= rect.w();
    } else {
      if (freeList == null) {
        freeList = new ArrayList<Rect>();
      }
      freeList.add(new Rect(rect.x(), rect.y(), rect.w(), height, null));
      coalesceFreeList();
    }

    return true;
  }

  /** Indicates whether this Level contains no rectangles. */
  public boolean isEmpty() {
    return rects.isEmpty();
  }

  /** Indicates whether this Level could satisfy an allocation request
      if it were compacted. */
  public boolean couldAllocateIfCompacted(final Rect rect) {
    if (rect.h() > height)
      return false;
    if (freeList == null)
      return false;
    int freeListWidth = 0;
    for (final Iterator<Rect> iter = freeList.iterator(); iter.hasNext(); ) {
      final Rect cur = iter.next();
      freeListWidth += cur.w();
    }
    // Add on the remaining space at the end
    freeListWidth += (width - nextAddX);
    return (freeListWidth >= rect.w());
  }

  public void compact(final Object backingStore, final BackingStoreManager manager) {
    Collections.sort(rects, rectXComparator);
    int nextCompactionDest = 0;
    manager.beginMovement(backingStore, backingStore);
    for (final Iterator<Rect> iter = rects.iterator(); iter.hasNext(); ) {
      final Rect cur = iter.next();
      if (cur.x() != nextCompactionDest) {
        manager.move(backingStore, cur,
                     backingStore, new Rect(nextCompactionDest, cur.y(), cur.w(), cur.h(), null));
        cur.setPosition(nextCompactionDest, cur.y());
      }
      nextCompactionDest += cur.w();
    }
    nextAddX = nextCompactionDest;
    freeList.clear();
    manager.endMovement(backingStore, backingStore);
  }

  public Iterator<Rect> iterator() {
    return rects.iterator();
  }

  /** Visits all Rects contained in this Level. */
  public void visit(final RectVisitor visitor) {
    for (final Iterator<Rect> iter = rects.iterator(); iter.hasNext(); ) {
      final Rect rect = iter.next();
      visitor.visit(rect);
    }
  }

  /** Updates the references to the Rect objects in this Level with
      the "next locations" of those Rects. This is actually used to
      update the new Rects in a newly laid-out LevelSet with the
      original Rects. */
  public void updateRectangleReferences() {
    for (int i = 0; i < rects.size(); i++) {
      final Rect cur = rects.get(i);
      final Rect next = cur.getNextLocation();
      next.setPosition(cur.x(), cur.y());
      if (cur.w() != next.w() || cur.h() != next.h())
        throw new RuntimeException("Unexpected disparity in rectangle sizes during updateRectangleReferences");
      rects.set(i, next);
    }
  }

  private void coalesceFreeList() {
    if (freeList == null)
      return;
    if (freeList.isEmpty())
      return;

    // Try to coalesce adjacent free blocks in the free list
    Collections.sort(freeList, rectXComparator);
    int i = 0;
    while (i < freeList.size() - 1) {
      final Rect r1 = freeList.get(i);
      final Rect r2 = freeList.get(i+1);
      if (r1.maxX() + 1 == r2.x()) {
        // Coalesce r1 and r2 into one block
        freeList.remove(i+1);
        r1.setSize(r1.w() + r2.w(), r1.h());
      } else {
        ++i;
      }
    }
    // See whether the last block bumps up against the addition point
    final Rect last = freeList.get(freeList.size() - 1);
    if (last.maxX() + 1 == nextAddX) {
      nextAddX -= last.w();
      freeList.remove(freeList.size() - 1);
    }
    if (freeList.isEmpty()) {
      freeList = null;
    }
  }

  //----------------------------------------------------------------------
  // Debugging functionality
  //

  public void dumpFreeSpace() {
    int freeListWidth = 0;
    for (final Iterator<Rect> iter = freeList.iterator(); iter.hasNext(); ) {
      final Rect cur = iter.next();
      System.err.println(" Free rectangle at " + cur);
      freeListWidth += cur.w();
    }
    // Add on the remaining space at the end
    System.err.println(" Remaining free space " + (width - nextAddX));
    freeListWidth += (width - nextAddX);
    System.err.println(" Total free space " + freeListWidth);
  }
}
