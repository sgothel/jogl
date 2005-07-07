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

package net.java.games.jogl.impl;

import java.lang.ref.*;
import java.util.*;

/** Provides a mechanism by which OpenGL contexts can share textures
    and display lists in the face of multithreading and asynchronous
    context creation as is inherent in the AWT and Swing. */

public class GLContextShareSet {
  // This class is implemented with a WeakHashMap that goes from the
  // contexts as keys to a complex data structure as value that tracks
  // context creation and deletion.

  private static Map/*<GLContext, WeakReference<ShareSet>>*/ shareMap = new WeakHashMap();
  private static Object dummyValue = new Object();

  private static class ShareSet {
    private Map allShares       = new WeakHashMap();
    private Map createdShares   = new WeakHashMap();
    private Map destroyedShares = new WeakHashMap();

    public void add(GLContext ctx) {
      if (allShares.put(ctx, dummyValue) == null) {
        if (ctx.isCreated()) {
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
      <code>share2</code> will share textures and display lists. */
  public static synchronized void registerSharing(GLContext share1, GLContext share2) {
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
