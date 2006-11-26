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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.ref.*;
import java.util.*;
import javax.media.opengl.*;

/** Provides a mechanism by which OpenGL contexts can share textures
    and display lists in the face of multithreading and asynchronous
    context creation as is inherent in the AWT and Swing. */

public class GLContextShareSet {
  private static boolean forceTracking = Debug.isPropertyDefined("jogl.glcontext.forcetracking");
  private static final boolean DEBUG = Debug.debug("GLContextShareSet");

  // This class is implemented with a WeakHashMap that goes from the
  // contexts as keys to a complex data structure as value that tracks
  // context creation and deletion.

  private static Map/*<GLContext, ShareSet>*/ shareMap = new WeakHashMap();
  private static Object dummyValue = new Object();

  private static class ShareSet {
    private Map allShares       = new WeakHashMap();
    private Map createdShares   = new WeakHashMap();
    private Map destroyedShares = new WeakHashMap();

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
  }

  /** Indicate that contexts <code>share1</code> and
      <code>share2</code> will share textures and display lists. Both
      must be non-null. */
  public static synchronized void registerSharing(GLContext share1, GLContext share2) {
    if (share1 == null || share2 == null) {
      throw new IllegalArgumentException("Both share1 and share2 must be non-null");
    }
    ShareSet share = entryFor(share1);
    if (share == null) {
      share = entryFor(share2);
    }
    if (share == null) {
      share = new ShareSet();
    }
    share.add(share1);
    share.add(share2);
    addEntry(share1, share);
    addEntry(share2, share);
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

  /** Indicates that the two supplied contexts (which must be able to
      share textures and display lists) should be in the same
      namespace for tracking of server-side object creation and
      deletion. Because the sharing necessary behind the scenes is
      different than that requested at the user level, the two notions
      are different. This must be called immediately after the
      creation of the new context (which is the second argument)
      before any server-side OpenGL objects have been created in that
      context. */
  public static void registerForObjectTracking(GLContext olderContextOrNull,
                                               GLContext newContext,
                                               GLContext realShareContext) {
    if (isObjectTrackingEnabled() || isObjectTrackingDebuggingEnabled()) {
      GLContextImpl impl1 = null;      
      GLContextImpl impl2 = null;      
      GLObjectTracker tracker = null;

      synchronized (GLContextShareSet.class) {
        if (olderContextOrNull != null &&
            newContext != null) {
          if (entryFor(olderContextOrNull) != entryFor(newContext)) {
            throw new IllegalArgumentException("old and new contexts must be able to share textures and display lists");
          }
        }

        // FIXME: downcast to GLContextImpl undesirable
        impl1 = (GLContextImpl) olderContextOrNull;
        impl2 = (GLContextImpl) newContext;

        GLObjectTracker deletedObjectTracker = null;
        GLContextImpl shareImpl = (GLContextImpl) realShareContext;
        // Before we zap the "user-level" object trackers, make sure
        // that all contexts in the share set share the destroyed object
        // tracker
        if (shareImpl != null) {
          deletedObjectTracker = shareImpl.getDeletedObjectTracker();
        }
        if (deletedObjectTracker == null) {
          // Must create one and possibly set it up in the older context
          deletedObjectTracker = new GLObjectTracker();
          if (DEBUG) {
            System.err.println("Created deletedObjectTracker " + deletedObjectTracker + " because " +
                               ((shareImpl == null) ? "shareImpl was null" : "shareImpl's (" + shareImpl + ") deletedObjectTracker was null"));
          }

          if (shareImpl != null) {
            // FIXME: think should really assert in this case
            shareImpl.setDeletedObjectTracker(deletedObjectTracker);
            if (DEBUG) {
              System.err.println("Set deletedObjectTracker " + deletedObjectTracker + " in shareImpl context " + shareImpl);
            }
          }
        }
        impl2.setDeletedObjectTracker(deletedObjectTracker);
        if (DEBUG) {
          System.err.println("Set deletedObjectTracker " + deletedObjectTracker + " in impl2 context " + impl2);
        }
      }

      // Must not hold lock around this operation
      // Don't share object trackers with the primordial share context from Java2D
      if (Java2D.isOGLPipelineActive()) {
        // FIXME: probably need to do something different here
        // Need to be able to figure out the GraphicsDevice for the
        // older context if it's on-screen
        GraphicsConfiguration gc = GraphicsEnvironment.
          getLocalGraphicsEnvironment().
          getDefaultScreenDevice().
          getDefaultConfiguration();
        GLContext j2dShareContext = Java2D.getShareContext(gc);
        if (impl1 != null && impl1 == j2dShareContext) {
          impl1 = null;
        }
      }

      synchronized (GLContextShareSet.class) {
        if (impl1 != null) {
          tracker = impl1.getObjectTracker();
          assert (tracker != null)
            : "registerForObjectTracking was not called properly for the older context";
        }
        if (tracker == null) {
          tracker = new GLObjectTracker();
        }
        // Note that we don't assert that the tracker is non-null for
        // impl2 because the way we use this functionality we actually
        // overwrite the initially-set object tracker in the new context
        impl2.setObjectTracker(tracker);
      }
    }
  }

  /** In order to avoid glGet calls for buffer object checks related
      to glVertexPointer, etc. calls as well as glMapBuffer calls, we
      need to share the same GLBufferSizeTracker object between
      contexts sharing textures and display lists. For now we keep
      this mechanism orthogonal to the GLObjectTracker to hopefully
      keep things easier to understand. (The GLObjectTracker is
      currently only needed in a fairly esoteric case, when the
      Java2D/JOGL bridge is active, but the GLBufferSizeTracker
      mechanism is now always required.) */
  public static void registerForBufferObjectSharing(GLContext olderContextOrNull, GLContext newContext) {
    // FIXME: downcasts to GLContextImpl undesirable
    GLContextImpl older = (GLContextImpl) olderContextOrNull;
    GLContextImpl newer = (GLContextImpl) newContext;
    GLBufferSizeTracker tracker = null;
    if (older != null) {
      tracker = older.getBufferSizeTracker();
      assert (tracker != null)
        : "registerForBufferObjectSharing was not called properly for the older context, or has a bug in it";
    }
    if (tracker == null) {
      tracker = new GLBufferSizeTracker();
    }
    newer.setBufferSizeTracker(tracker);
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

  private static boolean isObjectTrackingEnabled() {
    return ((Java2D.isOGLPipelineActive() && Java2D.isFBOEnabled()) ||
            isObjectTrackingDebuggingEnabled());
  }

  private static boolean isObjectTrackingDebuggingEnabled() {
    return forceTracking;
  }
}
