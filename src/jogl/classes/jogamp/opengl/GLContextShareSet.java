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

import java.util.HashMap;
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

  private static final Map<GLContext, ShareSet> shareMap = new HashMap<GLContext, ShareSet>();
  private static final Object dummyValue = new Object();

  private static class ShareSet {
    private Map<GLContext, Object> allShares       = new HashMap<GLContext, Object>();
    private Map<GLContext, Object> createdShares   = new HashMap<GLContext, Object>();
    private Map<GLContext, Object> destroyedShares = new HashMap<GLContext, Object>();

    public void add(GLContext ctx) {
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
    
    public GLContext getCreatedShare(GLContext ignore) {
      for (Iterator<GLContext> iter = createdShares.keySet().iterator(); iter.hasNext(); ) {
        GLContext ctx = iter.next();
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
    if (DEBUG) {
      System.err.println("GLContextShareSet: registereSharing: 1: " + 
              toHexString(share1.getHandle()) + ", 2: " + toHexString(share2.getHandle()));
    }                  
  }

  public static synchronized void unregisterSharing(GLContext lastContext) {
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
  
  private static synchronized Set<GLContext> getCreatedSharedImpl(GLContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    final ShareSet share = entryFor(context);
    if (share != null) {
        return share.getCreatedShares();
    }
    return null;    
  }
  
  public static synchronized boolean isShared(GLContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    final ShareSet share = entryFor(context);
    return share != null;
  }
  
  public static synchronized boolean hasCreatedSharedLeft(GLContext context) {
      final Set<GLContext> s = getCreatedSharedImpl(context);
      return null != s && s.size()>0 ;
  }
  
  /** currently not used ..
  public static synchronized Set<GLContext> getCreatedShared(GLContext context) {
    final Set<GLContext> s = getCreatedSharedImpl(context);
    if (s == null) {
      throw new GLException("context is unknown: "+context);
    }
    return s;
  }
    
  public static synchronized Set<GLContext> getDestroyedShared(GLContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is null");
    }
    ShareSet share = entryFor(context);
    if (share == null) {
      throw new GLException("context is unknown: "+context);
    }
    return share.getDestroyedShares();
  } */
    
  public static synchronized GLContext getShareContext(GLContext contextToCreate) {
    ShareSet share = entryFor(contextToCreate);
    if (share == null) {
      return null;
    }
    return share.getCreatedShare(contextToCreate);
  }

  public static synchronized boolean contextCreated(GLContext context) {
    ShareSet share = entryFor(context);
    if (share != null) {
      share.contextCreated(context);
      return true;
    }
    return false;
  }

  public static synchronized boolean contextDestroyed(GLContext context) {
    ShareSet share = entryFor(context);
    if (share != null) {
      share.contextDestroyed(context);
      return true;
    }
    return false;
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
  public static void synchronizeBufferObjectSharing(GLContext olderContextOrNull, GLContext newContext) {
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
  

  private static ShareSet entryFor(GLContext context) {
    return (ShareSet) shareMap.get(context);
  }

  private static void addEntry(GLContext context, ShareSet share) {
    if (shareMap.get(context) == null) {
      shareMap.put(context, share);
    }
  }
  private static ShareSet removeEntry(GLContext context) {
    return (ShareSet) shareMap.remove(context);
  }
  
  protected static String toHexString(long hex) {
    return "0x" + Long.toHexString(hex);
  }  
}
