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

import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;


/** Provides a deterministic mechanism by which OpenGL contexts can share textures
    and display lists in the face of multithreading and asynchronous
    context creation. */

public class GLContextShareSet {
  private static final boolean DEBUG = GLContextImpl.DEBUG;

  // This class is implemented using a HashMap which maps from all shared contexts
  // to a share set, containing all shared contexts itself.

  private static final Map<GLContext, ShareSet> shareMap = new IdentityHashMap<GLContext, ShareSet>();

  private static class ShareSet {
    private final Map<GLContext, GLContext> createdShares   = new IdentityHashMap<GLContext, GLContext>();
    private final Map<GLContext, GLContext> destroyedShares = new IdentityHashMap<GLContext, GLContext>();

    public final void addNew(final GLContext slave, final GLContext master) {
        final GLContext preMaster;
        if ( slave.isCreated() ) {
            preMaster = createdShares.put(slave, master);
        } else {
            preMaster= destroyedShares.put(slave, master);
        }
        if( null != preMaster ) {
            throw new InternalError("State of ShareSet corrupted: Slave "+toHexString(slave.hashCode())+
                                    " is not new w/ master "+toHexString(preMaster.hashCode()));
        }
    }
    public final void addIfNew(final GLContext slave, final GLContext master) {
        final GLContext preMaster = getMaster(master);
        if( null == preMaster ) {
            addNew(slave, master);
        }
    }

    public final GLContext getMaster(final GLContext ctx) {
        final GLContext c = createdShares.get(ctx);
        return null != c ? c : destroyedShares.get(ctx);
    }

    public Set<GLContext> getCreatedShares() {
        return createdShares.keySet();
    }

    public Set<GLContext> getDestroyedShares() {
        return destroyedShares.keySet();
    }

    public void contextCreated(final GLContext ctx) {
      final GLContext ctxMaster = destroyedShares.remove(ctx);
      if( null == ctxMaster ) {
            throw new InternalError("State of ShareSet corrupted: Context "+toHexString(ctx.hashCode())+
                                    " should have been in destroyed-set");
      }
      final GLContext delMaster = createdShares.put(ctx, ctxMaster);
      if( null != delMaster ) {
            throw new InternalError("State of ShareSet corrupted: Context "+toHexString(ctx.hashCode())+
                                    " shouldn't have been in created-set");
      }
    }

    public void contextDestroyed(final GLContext ctx) {
      final GLContext ctxMaster = createdShares.remove(ctx);
      if( null == ctxMaster ) {
            throw new InternalError("State of ShareSet corrupted: Context "+toHexString(ctx.hashCode())+
                                    " should have been in created-set");
      }
      final GLContext delMaster = destroyedShares.put(ctx, ctxMaster);
      if( null != delMaster ) {
            throw new InternalError("State of ShareSet corrupted: Context "+toHexString(ctx.hashCode())+
                                    " shouldn't have been in destroyed-set");
      }
    }
  }

  /** Indicate that contexts <code>slave</code> and
      <code>master</code> will share textures and display lists. Both
      must be non-null. */
  public static synchronized void registerSharing(final GLContext slave, final GLContext master) {
      if (slave == null || master == null) {
          throw new IllegalArgumentException("Both slave and master must be non-null");
      }
      ShareSet share = entryFor(slave);
      if ( null == share ) {
          share = entryFor(master);
      }
      if ( null == share ) {
          share = new ShareSet();
      }
      share.addNew(slave, master);
      share.addIfNew(master, master); // this master could have a different master shared registered earlier!
      addEntry(slave, share);
      addEntry(master, share);
      if (DEBUG) {
          System.err.println("GLContextShareSet: registereSharing: 1: " +
                  toHexString(slave.hashCode()) + ", 2: " + toHexString(master.hashCode()));
      }
  }

  public static synchronized void unregisterSharing(final GLContext lastContext) {
    if (lastContext == null) {
      throw new IllegalArgumentException("Last context is null");
    }
    final ShareSet share = entryFor(lastContext);
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
              toHexString(lastContext.hashCode())+", entries: "+s.size());
    }
    for(final Iterator<GLContext> iter = s.iterator() ; iter.hasNext() ; ) {
        final GLContext ctx = iter.next();
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

  /**
   * Returns the shared master GLContext of the given <code>context</code> if shared, otherwise return <code>null</code>.
   * <p>
   * Returns the given <code>context</code>, if it is a shared master.
   * </p>
   */
  public static synchronized GLContext getSharedMaster(final GLContext context) {
    final ShareSet share = entryFor(context);
    if (share == null) {
      return null;
    }
    return share.getMaster(context);
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
  public static synchronized boolean hasCreatedSharedLeft(final GLContext context) {
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

  private static String toHexString(final long hex) {
    return "0x" + Long.toHexString(hex);
  }
}
