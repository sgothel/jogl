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

package com.sun.opengl.impl;

import java.lang.ref.*;
import java.util.*;
import javax.media.opengl.*;

/** Provides a mechanism by which OpenGL contexts can share textures
    and display lists in the face of multithreading and asynchronous
    context creation as is inherent in the AWT and Swing. */

public class GLContextShareSet {
  private static boolean forceTracking = Debug.isPropertyDefined("jogl.glcontext.forcetracking");

  // This class is implemented with a WeakHashMap that goes from the
  // contexts as keys to a complex data structure as value that tracks
  // context creation and deletion.

  private static Map/*<GLContext, ShareSet>*/ shareMap = new WeakHashMap();
  private static Object dummyValue = new Object();

  private static class ShareSet {
    private Map allShares       = new WeakHashMap();
    private Map createdShares   = new WeakHashMap();
    private Map destroyedShares = new WeakHashMap();

    // When the Java2D/OpenGL pipeline is active and using FBOs to
    // render, we need to track the creation and destruction of
    // server-side OpenGL objects among contexts sharing these objects
    private GLObjectTracker tracker;

    public ShareSet() {
      if (isObjectTrackingEnabled()) {
        tracker = new GLObjectTracker();
      }
    }

    public void add(GLContext ctx) {
      if (allShares.put(ctx, dummyValue) == null) {
        // FIXME: downcast to GLContextImpl undesirable
        if (((GLContextImpl) ctx).isCreated()) {
          createdShares.put(ctx, dummyValue);
        } else {
          destroyedShares.put(ctx, dummyValue);
        }
      }      
    }

    public GLContext getCreatedShare(GLContext ignore) {
      for (Iterator iter = createdShares.keySet().iterator(); iter.hasNext(); ) {
        GLContext ctx = (GLContext) iter.next();
        if (ctx != ignore) {
          return ctx;
        }
      }
      return null;
    }

    public void contextCreated(GLContext ctx) {
      Object res = destroyedShares.remove(ctx);
      assert res != null : "State of ShareSet corrupted; thought context " +
        ctx + " should have been in destroyed set but wasn't";
      res = createdShares.put(ctx, dummyValue);
      assert res == null : "State of ShareSet corrupted; thought context " +
        ctx + " shouldn't have been in created set but was";
    }

    public void contextDestroyed(GLContext ctx) {
      Object res = createdShares.remove(ctx);
      assert res != null : "State of ShareSet corrupted; thought context " +
        ctx + " should have been in created set but wasn't";
      res = destroyedShares.put(ctx, dummyValue);
      assert res == null : "State of ShareSet corrupted; thought context " +
        ctx + " shouldn't have been in destroyed set but was";
    }

    public GLObjectTracker getObjectTracker() {
      return tracker;
    }
  }

  private static boolean isObjectTrackingEnabled() {
    return (Java2D.isOGLPipelineActive() && Java2D.isFBOEnabled());
  }

  /** Indicates to callers whether sharing must be registered even for
      contexts which don't share textures and display lists with any
      others. */
  public static boolean isObjectTrackingDebuggingEnabled() {
    return forceTracking;
  }

  /** Indicate that contexts <code>share1</code> and
      <code>share2</code> will share textures and display lists. */
  public static synchronized void registerSharing(GLContext share1, GLContext share2) {
    ShareSet share = entryFor(share1);
    if (share == null && (share2 != null)) {
      share = entryFor(share2);
    }
    if (share == null) {
      share = new ShareSet();
    }
    share.add(share1);
    if (share2 != null) {
      share.add(share2);
    }
    addEntry(share1, share);
    if (share2 != null) {
      addEntry(share2, share);
    }
    GLObjectTracker tracker = share.getObjectTracker();
    if (tracker != null) {
      // FIXME: downcast to GLContextImpl undesirable
      GLContextImpl impl1 = (GLContextImpl) share1;
      GLContextImpl impl2 = (GLContextImpl) share2;
      if (impl1.getObjectTracker() == null) {
        impl1.setObjectTracker(tracker);
      }
      if ((impl2 != null) && (impl2.getObjectTracker() == null)) {
        impl2.setObjectTracker(tracker);
      }
      assert impl1.getObjectTracker() == tracker : "State of ShareSet corrupted; " +
        "got different-than-expected GLObjectTracker for context 1";
      assert (impl2 == null) || (impl2.getObjectTracker() == tracker) : "State of ShareSet corrupted; " +
        "got different-than-expected GLObjectTracker for context 2";
    }
  }

  public static synchronized GLContext getShareContext(GLContext contextToCreate) {
    ShareSet share = entryFor(contextToCreate);
    if (share == null) {
      return null;
    }
    return share.getCreatedShare(contextToCreate);
  }

  public static synchronized void contextCreated(GLContext context) {
    ShareSet share = entryFor(context);
    if (share != null) {
      share.contextCreated(context);
    }
  }

  public static synchronized void contextDestroyed(GLContext context) {
    ShareSet share = entryFor(context);
    if (share != null) {
      share.contextDestroyed(context);
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static ShareSet entryFor(GLContext context) {
    return (ShareSet) shareMap.get(context);
  }

  private static void addEntry(GLContext context, ShareSet share) {
    if (shareMap.get(context) == null) {
      shareMap.put(context, share);
    }
  }
}
