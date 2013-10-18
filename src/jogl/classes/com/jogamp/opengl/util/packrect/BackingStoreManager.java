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

/** This interface must be implemented by the end user and is called
    in response to events like addition of rectangles into the
    RectanglePacker. It is used both when a full re-layout must be
    done as well as when the data in the backing store must be copied
    to a new one. */

public interface BackingStoreManager {
  public Object allocateBackingStore(int w, int h);
  public void   deleteBackingStore(Object backingStore);

  /** Indication whether this BackingStoreManager supports compaction;
      in other words, the allocation of a new backing store and
      movement of the contents of the backing store from the old to
      the new one. If it does not, then RectanglePacker.add() may
      throw an exception if additionFailed() can not make enough room
      available. If an implementation returns false, this also implies
      that the backing store can not grow, so that preExpand() will
      never be called. */
  public boolean canCompact();

  /** Notification that expansion of the backing store is about to be
      done due to addition of the given rectangle. Gives the manager a
      chance to do some compaction and potentially remove old entries
      from the backing store, if it acts like a least-recently-used
      cache. This method receives as argument the number of attempts
      so far to add the given rectangle. Manager should return true if
      the RectanglePacker should retry the addition (which may result
      in this method being called again, with an increased attempt
      number) or false if the RectanglePacker should just expand the
      backing store. The caller should not call RectanglePacker.add()
      in its preExpand() method. */
  public boolean preExpand(Rect cause, int attemptNumber);

  /** Notification that addition of the given Rect failed because a
      maximum size was set in the RectanglePacker and the backing
      store could not be expanded, or because compaction (and,
      therefore, implicitly expansion) was not supported. Should
      return false if the manager can do nothing more to handle the
      failed addition, which will cause a RuntimeException to be
      thrown from the RectanglePacker. */
  public boolean additionFailed(Rect cause, int attemptNumber);

  /** Notification that movement is starting. */
  public void beginMovement(Object oldBackingStore, Object newBackingStore);

  /** Tells the manager to move the contents of the given rect from
      the old location on the old backing store to the new location on
      the new backing store. The backing stores can be identical in
      the case of compacting the existing backing store instead of
      reallocating it. */
  public void move(Object oldBackingStore,
                   Rect   oldLocation,
                   Object newBackingStore,
                   Rect   newLocation);

  /** Notification that movement is ending. */
  public void endMovement(Object oldBackingStore, Object newBackingStore);
}
