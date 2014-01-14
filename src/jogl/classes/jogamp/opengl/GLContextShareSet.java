/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
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

package jogamp.opengl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;


/** Provides a deterministic mechanism by which OpenGL contexts can share textures
    and display lists in the face of multithreading and asynchronous
    context creation. */

public class GLContextShareSet {
  private static final boolean DEBUG = GLContextImpl.DEBUG;

  // This class is implemented using a HashMap which maps from all shared contexts
  // to a share set, containing all shared contexts itself.

  private static final Map<GLContext, ShareSet> shareMap = new IdentityHashMap<GLContext, ShareSet>();
  private static final Object dummyValue = new Object();

  private static class ShareSet {
    private final Map<GLContext, Object> allShares       = new IdentityHashMap<GLContext, Object>();
    private final Map<GLContext, Object> createdShares   = new IdentityHashMap<GLContext, Object>();
    private final Map<GLContext, Object> destroyedShares = new IdentityHashMap<GLContext, Object>();

    public void add(final GLContext ctx) {
      if (allShares.put(ctx, dummyValue) == null) {
        if (ctx.isCreated()) {
          createdShares.put(ctx, dummyValue);
        } else {
          destroyedShares.put(ctx, dummyValue);
        }
      }
    }

    public Set<GLContext> getCreatedShares() {
        return createdShares.keySet();
    }

    public Set<GLContext> getDestroyedShares() {
        return destroyedShares.keySet();
    }

    public GLContext getCreatedShare(final GLContext ignore) {
      for (final Iterator<GLContext> iter = createdShares.keySet().iterator(); iter.hasNext(); ) {
        final GLContext ctx = iter.next();
        if (ctx != ignore) {
          return ctx;
        }
      }
      return null;
    }

    public void contextCreated(final GLContext ctx) {
      final Object res = destroyedShares.remove(ctx);
      assert res != null : "State of ShareSet corrupted; thought context " +
        ctx + " should have been in destroyed set but wasn't";
      final Object res2 = createdShares.put(ctx, dummyValue);
      assert res2 == null : "State of ShareSet corrupted; thought context " +
        ctx + " shouldn't have been in created set but was";
    }

    public void contextDestroyed(final GLContext ctx) {
      final Object res = createdShares.remove(ctx);
      assert res != null : "State of ShareSet corrupted; thought context " +
        ctx + " should have been in created set but wasn't";
      final Object res2 = destroyedShares.put(ctx, dummyValue);
      assert res2 == null : "State of ShareSet corrupted; thought context " +
        ctx + " shouldn't have been in destroyed set but was";
    }
  }

  /** Indicate that contexts <code>share1</code> and
      <code>share2</code> will share textures and display lists. Both
      must be non-null. */
  public static synchronized void registerSharing(final GLContext share1, final GLContext share2) {
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
    if (DEBUG) {
      System.err.println("GLContextShareSet: registereSharing: 1: " +
              toHexString(share1.getHandle()) + ", 2: " + toHexString(share2.getHandle()));
    }
  }

  public static synchronized void unregisterSharing(final GLContext lastContext) {
    if (lastContext == null) {
      throw new IllegalArgumentException("Last context is null");
    }
    ShareSet share = entryFor(lastContext);
    if (share == null) {
      throw new GLException("Last context is unknown: "+lastContext);
    }
    Set<GLContext> s = share.getCreatedShares();
    if(s.size()>0) {
        throw new GLException("Last context's share set contains "+s.size()+" non destroyed context");
    }
    s = share.getDestroyedShares();
    if(s.size()==0) {
        throw new GLException("Last context's share set contains no destroyed context");
    }
    if (DEBUG) {
      System.err.println("GLContextShareSet: unregisterSharing: " +
              toHexString(lastContext.getHandle())+", entries: "+s.size());
    }
    for(Iterator<GLContext> iter = s.iterator() ; iter.hasNext() ; ) {
        GLContext ctx = iter.next();
        if(null == removeEntry(ctx)) {
            throw new GLException("Removal of shareSet for context failed");
        }
    }
  }

  /** Returns true if the given GLContext is shared, otherwise false. */
  public static synchronized boolean isShared(final GLContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    final ShareSet share = entryFor(context);
    return share != null;
  }

  /** Returns one created GLContext shared with the given <code>context</code>, otherwise return <code>null</code>. */
  public static synchronized GLContext getCreatedShare(final GLContext context) {
    final ShareSet share = entryFor(context);
    if (share == null) {
      return null;
    }
    return share.getCreatedShare(context);
  }

  private static synchronized Set<GLContext> getCreatedSharesImpl(final GLContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    final ShareSet share = entryFor(context);
    if (share != null) {
        return share.getCreatedShares();
    }
    return null;
  }
  private static synchronized Set<GLContext> getDestroyedSharesImpl(final GLContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    final ShareSet share = entryFor(context);
    if (share != null) {
        return share.getDestroyedShares();
    }
    return null;
  }

  /** Returns true if the given GLContext has shared and created GLContext left including itself, otherwise false. */
  public static synchronized boolean hasCreatedSharedLeft(GLContext context) {
      final Set<GLContext> s = getCreatedSharesImpl(context);
      return null != s && s.size() > 0;
  }

  /** Returns a new array-list of created GLContext shared with the given GLContext. */
  public static synchronized ArrayList<GLContext> getCreatedShares(final GLContext context) {
      final ArrayList<GLContext> otherShares = new ArrayList<GLContext>();
      final Set<GLContext> createdShares = getCreatedSharesImpl(context);
      if( null != createdShares ) {
          for (final Iterator<GLContext> iter = createdShares.iterator(); iter.hasNext(); ) {
            final GLContext ctx = iter.next();
            if (ctx != context) {
                otherShares.add(ctx);
            }
          }
      }
      return otherShares;
  }

  /** Returns a new array-list of destroyed GLContext shared with the given GLContext. */
  public static synchronized ArrayList<GLContext> getDestroyedShares(final GLContext context) {
      final ArrayList<GLContext> otherShares = new ArrayList<GLContext>();
      final Set<GLContext> destroyedShares = getDestroyedSharesImpl(context);
      if( null != destroyedShares ) {
          for (final Iterator<GLContext> iter = destroyedShares.iterator(); iter.hasNext(); ) {
            final GLContext ctx = iter.next();
            if (ctx != context) {
                otherShares.add(ctx);
            }
          }
      }
      return otherShares;
  }

  /** Mark the given GLContext as being created. */
  public static synchronized boolean contextCreated(final GLContext context) {
    final ShareSet share = entryFor(context);
    if (share != null) {
      share.contextCreated(context);
      return true;
    }
    return false;
  }

  /** Mark the given GLContext as being destroyed. */
  public static synchronized boolean contextDestroyed(final GLContext context) {
    final ShareSet share = entryFor(context);
    if (share != null) {
      share.contextDestroyed(context);
      return true;
    }
    return false;
  }

  //----------------------------------------------------------------------
  // Internals only below this point


  private static ShareSet entryFor(final GLContext context) {
    return shareMap.get(context);
  }

  private static void addEntry(final GLContext context, final ShareSet share) {
    if (shareMap.get(context) == null) {
      shareMap.put(context, share);
    }
  }
  private static ShareSet removeEntry(final GLContext context) {
    return shareMap.remove(context);
  }

  private static String toHexString(long hex) {
    return "0x" + Long.toHexString(hex);
  }
}
