/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

import javax.media.opengl.*;
import com.jogamp.common.util.IntLongHashMap;

/**
 * Tracks as closely as possible the sizes of allocated OpenGL buffer
 * objects. When glMapBuffer or glMapBufferARB is called, in order to
 * turn the resulting base address into a java.nio.ByteBuffer, we need
 * to know the size in bytes of the allocated OpenGL buffer object.
 * Previously we would compute this size by using
 * glGetBufferParameterivARB with a pname of GL_BUFFER_SIZE, but
 * it appears doing so each time glMapBuffer is called is too costly
 * on at least Apple's new multithreaded OpenGL implementation. <P>
 *
 * Instead we now try to track the sizes of allocated buffer objects.
 * We watch calls to glBindBuffer to see which buffer is bound to
 * which target and to glBufferData to see how large the buffer's
 * allocated size is. When glMapBuffer is called, we consult our table
 * of buffer sizes to see if we can return an answer without a glGet
 * call. <P>
 *
 * We share the GLBufferSizeTracker objects among all GLContexts for
 * which sharing is enabled, because the namespace for buffer objects
 * is the same for these contexts. <P>
 *
 * Tracking the state of which buffer objects are bound is done in the
 * GLBufferStateTracker and is not completely trivial. In the face of
 * calls to glPushClientAttrib / glPopClientAttrib we currently punt
 * and re-fetch the bound buffer object for the state in question;
 * see, for example, glVertexPointer and the calls down to
 * GLBufferStateTracker.getBoundBufferObject(). Note that we currently
 * ignore new binding targets such as GL_TRANSFORM_FEEDBACK_BUFFER_NV;
 * the fact that new binding targets may be added in the future makes
 * it impossible to cache state for these new targets. <P>
 *
 * Ignoring new binding targets, the primary situation in which we may
 * not be able to return a cached answer is in the case of an error,
 * where glBindBuffer may not have been called before trying to call
 * glBufferData. Also, if external native code modifies a buffer
 * object, we may return an incorrect answer. (FIXME: this case
 * requires more thought, and perhaps stochastic and
 * exponential-fallback checking. However, note that it can only occur
 * in the face of external native code which requires that the
 * application be signed anyway, so there is no security risk in this
 * area.)
 */

public class GLBufferSizeTracker {
  // Map from buffer names to sizes.
  // Note: should probably have some way of shrinking this map, but
  // can't just make it a WeakHashMap because nobody holds on to the
  // keys; would have to always track creation and deletion of buffer
  // objects, which is probably sub-optimal. The expected usage
  // pattern of buffer objects indicates that the fact that this map
  // never shrinks is probably not that bad.
  private IntLongHashMap bufferSizeMap;

  protected static final boolean DEBUG = Debug.debug("GLStatusTracker");

  public GLBufferSizeTracker() {
      bufferSizeMap = new IntLongHashMap();
      bufferSizeMap.setKeyNotFoundValue(-1);
  }

  public void setBufferSize(GLBufferStateTracker bufferStateTracker,
                            int target, GL caller, long size) {
    // Need to do some similar queries to getBufferSize below
    int buffer = bufferStateTracker.getBoundBufferObject(target, caller);
    if (buffer != 0) {
      setDirectStateBufferSize(buffer, caller, size);
    }
    // We don't know the current buffer state. Note that the buffer
    // state tracker will have made the appropriate OpenGL query if it
    // didn't know what was going on, so at this point we have nothing
    // left to do except drop this piece of information on the floor.
  }

  public void setDirectStateBufferSize(int buffer, GL caller, long size) {
      bufferSizeMap.put(buffer, size);
  }

  public long getBufferSize(GLBufferStateTracker bufferStateTracker,
                           int target,
                           GL caller) {
    // See whether we know what buffer is currently bound to the given
    // state
    final int buffer = bufferStateTracker.getBoundBufferObject(target, caller);
    if (0 != buffer) {
      return getBufferSizeImpl(target, buffer, caller);
    }
    // We don't know what's going on in this case; query the GL for an answer
    // FIXME: both functions return 'int' types, which is not suitable,
    // since buffer lenght is 64bit ?
    int[] tmp = new int[1];
    caller.glGetBufferParameteriv(target, GL.GL_BUFFER_SIZE, tmp, 0);
    if (DEBUG) {
      System.err.println("GLBufferSizeTracker.getBufferSize(): no cached buffer information");
    }
    return (long) tmp[0];
  }

  public long getDirectStateBufferSize(int buffer, GL caller) {
      return getBufferSizeImpl(0, buffer, caller);
  }

  private long getBufferSizeImpl(int target, int buffer, GL caller) {
      // See whether we know the size of this buffer object; at this
      // point we almost certainly should if the application is
      // written correctly
      long sz = bufferSizeMap.get(buffer);
      if (0 > sz) {
        // For robustness, try to query this value from the GL as we used to
        // FIXME: both functions return 'int' types, which is not suitable,
        // since buffer lenght is 64bit ?
        int[] tmp = new int[1];
        if(0==target) {
            // DirectState ..
            if(caller.isFunctionAvailable("glGetNamedBufferParameterivEXT")) {
                caller.getGL2().glGetNamedBufferParameterivEXT(buffer, GL.GL_BUFFER_SIZE, tmp, 0);
            } else {
                throw new GLException("Error: getDirectStateBufferSize called with unknown state and GL function 'glGetNamedBufferParameterivEXT' n/a to query size");
            }
        } else {
            caller.glGetBufferParameteriv(target, GL.GL_BUFFER_SIZE, tmp, 0);
        }
        if (tmp[0] == 0) {
          // Assume something is wrong rather than silently going along
          throw new GLException("Error: buffer size returned by "+
                                ((0==target)?"glGetNamedBufferParameterivEXT":"glGetBufferParameteriv")+
                                " was zero; probably application error");
        }
        // Assume we just don't know what's happening
        sz = (long) tmp[0];
        bufferSizeMap.put(buffer, sz);
        if (DEBUG) {
          System.err.println("GLBufferSizeTracker.getBufferSize(): made slow query to cache size " +
                             sz +
                             " for buffer " +
                             buffer);
        }
      }
      return sz;
  }

  // This should be called on any major event where we might start
  // producing wrong answers, such as OpenGL context creation and
  // destruction if we don't know whether there are other currently-
  // created contexts that might be keeping the buffer objects alive
  // that we're dealing with
  public void clearCachedBufferSizes() {
    bufferSizeMap.clear();
  }
}
