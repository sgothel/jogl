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

import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.common.util.IntLongHashMap;

/**
 * <b>Buffer Target Mapping (Binding)</b>
 * <p>
 * Tracks as closely as possible which OpenGL buffer object is bound
 * to which binding target in the current OpenGL context.
 * GLBufferStateTracker objects are allocated on a per-OpenGL-context basis.
 * This class is used to verify that e.g. the vertex
 * buffer object extension is in use when the glVertexPointer variant
 * taking a long as argument is called. <P>
 *
 * Note that because the enumerated value used for the binding of a
 * buffer object (e.g. GL_ARRAY_BUFFER) is different than that used to
 * query the binding using glGetIntegerv (e.g.
 * GL_ARRAY_BUFFER_BINDING), then in the face of new binding targets
 * being added to the GL (e.g. GL_TRANSFORM_FEEDBACK_BUFFER_NV) it is
 * impossible to set up a query of the buffer object currently bound
 * to a particular state. It turns out that for some uses, such as
 * finding the size of the currently bound buffer, this doesn't
 * matter, though of course without knowing the buffer object we can't
 * re-associate the queried size with the buffer object ID. <P>
 *
 * Because the namespace of buffer objects is the unsigned integers
 * with 0 reserved by the GL, and because we have to be able to return
 * both 0 and other integers as valid answers from
 * getBoundBufferObject(), we need a second query, which is to ask
 * whether we know the state of the binding for a given target. For
 * "unknown" targets such as GL_TRANSFORM_FEEDBACK_BUFFER_NV we return
 * false from this, but we also clear the valid bit and later refresh
 * the binding state if glPushClientAttrib / glPopClientAttrib are
 * called, since we don't want the complexity of tracking stacks of
 * these attributes.
 * </p>
 *
 * <b>Buffer Size Mapping</b>
 * <p>
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
 * </p>
 */
public class GLBufferStateTracker {
  protected static final boolean DEBUG;

  static {
      Debug.initSingleton();
      DEBUG = Debug.isPropertyDefined("jogl.debug.GLBufferStateTracker", true);
  }

  // Maps binding targets to buffer objects. A null value indicates
  // that the binding is unknown. A zero value indicates that it is
  // known that no buffer is bound to the target, according to the
  // OpenGL specifications.
  // http://www.opengl.org/sdk/docs/man/xhtml/glBindBuffer.xml
  private final IntIntHashMap bufferBindingMap;
  private final int bufferNotFound = 0xFFFFFFFF;

  // Map from buffer names to sizes.
  // Note: should probably have some way of shrinking this map, but
  // can't just make it a WeakHashMap because nobody holds on to the
  // keys; would have to always track creation and deletion of buffer
  // objects, which is probably sub-optimal. The expected usage
  // pattern of buffer objects indicates that the fact that this map
  // never shrinks is probably not that bad.
  private final IntLongHashMap bufferSizeMap;
  private final long sizeNotFound = 0xFFFFFFFFFFFFFFFFL;

  private final int[] bufTmp = new int[1];

  public GLBufferStateTracker() {
    bufferBindingMap = new IntIntHashMap();
    bufferBindingMap.setKeyNotFoundValue(bufferNotFound);

    // Start with known unbound targets for known keys
    // setBoundBufferObject(GL2ES3.GL_VERTEX_ARRAY_BINDING, 0); // not using default VAO (removed in GL3 core) - only explicit
    setBoundBufferObject(GL.GL_ARRAY_BUFFER,         0);
    setBoundBufferObject(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    setBoundBufferObject(GL2.GL_PIXEL_PACK_BUFFER,   0);
    setBoundBufferObject(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
    setBoundBufferObject(GL4.GL_DRAW_INDIRECT_BUFFER, 0);

    bufferSizeMap = new IntLongHashMap();
    bufferSizeMap.setKeyNotFoundValue(sizeNotFound);
  }

  /**
   * Clears all states, i.e. issues {@link #clearBufferObjectState()}
   * and {@link #clearCachedBufferSizes()}.
   */
  public final void clear() {
      clearBufferObjectState();
      clearCachedBufferSizes();
  }

  //
  // Buffer target mapping (binding)
  //

  public final void setBoundBufferObject(int target, int value) {
    bufferBindingMap.put(target, value);
    /***
     * Test for clearing bound buffer states when unbinding VAO,
     * Bug 692 Comment 5 is invalid, i.e. <https://jogamp.org/bugzilla/show_bug.cgi?id=692#c5>.
     * However spec doesn't mention such behavior, and rendering w/ CPU sourced data
     * after unbinding a VAO w/o unbinding the VBOs resulted to no visible image.
     * Leaving code in here for discussion - in case I am wrong.
     *
    final int pre = bindingMap.put(target, value);
    if( GL2ES3.GL_VERTEX_ARRAY_BINDING == target && keyNotFound != pre && 0 == value ) {
        // Unbinding a previous bound VAO leads to unbinding of all buffers!
        bindingMap.put(GL.GL_ARRAY_BUFFER,         0);
        bindingMap.put(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
        bindingMap.put(GL2.GL_PIXEL_PACK_BUFFER,   0);
        bindingMap.put(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        bindingMap.put(GL4.GL_DRAW_INDIRECT_BUFFER, 0);
    } */
    if (DEBUG) {
      System.err.println("GLBufferStateTracker.setBoundBufferObject() target 0x" +
                         Integer.toHexString(target) + " -> mapped bound buffer 0x" +
                         Integer.toHexString(value));
      // Thread.dumpStack();
    }
  }

  /** Note: returns an unspecified value if the binding for the
      specified target (e.g. GL_ARRAY_BUFFER) is currently unknown.
      You must use isBoundBufferObjectKnown() to see whether the
      return value is valid. */
  public final int getBoundBufferObject(int target, GL caller) {
    int value = bufferBindingMap.get(target);
    if (bufferNotFound == value) {
      // User probably either called glPushClientAttrib /
      // glPopClientAttrib or is querying an unknown target. See
      // whether we know how to fetch this state.
      boolean gotQueryTarget = true;
      int queryTarget;
      switch (target) {
        case GL2ES3.GL_VERTEX_ARRAY_BINDING: queryTarget = GL2ES3.GL_VERTEX_ARRAY_BINDING;  break;
        case GL.GL_ARRAY_BUFFER:             queryTarget = GL.GL_ARRAY_BUFFER_BINDING;         break;
        case GL.GL_ELEMENT_ARRAY_BUFFER:     queryTarget = GL.GL_ELEMENT_ARRAY_BUFFER_BINDING; break;
        case GL2ES3.GL_PIXEL_PACK_BUFFER:    queryTarget = GL2.GL_PIXEL_PACK_BUFFER_BINDING;    break;
        case GL2ES3.GL_PIXEL_UNPACK_BUFFER:  queryTarget = GL2.GL_PIXEL_UNPACK_BUFFER_BINDING;  break;
        case GL4.GL_DRAW_INDIRECT_BUFFER:    queryTarget = GL4.GL_DRAW_INDIRECT_BUFFER_BINDING;  break;
        default:                             queryTarget = 0; gotQueryTarget = false; break;
      }
      if (gotQueryTarget) {
        final int glerrPre = caller.glGetError(); // clear
        caller.glGetIntegerv(queryTarget, bufTmp, 0);
        final int glerrPost = caller.glGetError(); // be safe, e.g. GL '3.0 Mesa 8.0.4' may produce an error querying GL_PIXEL_UNPACK_BUFFER_BINDING, ignore value
        if(GL.GL_NO_ERROR == glerrPost) {
            value = bufTmp[0];
        } else {
            value = 0;
        }
        if (DEBUG) {
          System.err.println("GLBufferStateTracker.getBoundBufferObject() glerr[pre 0x"+Integer.toHexString(glerrPre)+", post 0x"+Integer.toHexString(glerrPost)+"], [queried value]: target 0x" +
                             Integer.toHexString(target) + " / query 0x"+Integer.toHexString(queryTarget)+
                             " -> mapped bound buffer 0x" + Integer.toHexString(value));
        }
        setBoundBufferObject(target, value);
        return value;
      }
      return 0;
    }
    if (DEBUG) {
      System.err.println("GLBufferStateTracker.getBoundBufferObject() [mapped value]: target 0x" +
                         Integer.toHexString(target) + " -> mapped bound buffer 0x" +
                         Integer.toHexString(value));
    }
    return value;
  }

  /** Clears out the known/unknown state of the various buffer object
      binding states. These will be refreshed later on an as-needed
      basis. This is called by the implementations of
      glPushClientAttrib / glPopClientAttrib. Might want to call this
      from GLContext.makeCurrent() in the future to possibly increase
      the robustness of these caches in the face of external native
      code manipulating OpenGL state. */
  public final void clearBufferObjectState() {
    bufferBindingMap.clear();
        if (DEBUG) {
          System.err.println("GLBufferStateTracker.clearBufferObjectState()");
          //Thread.dumpStack();
        }
  }

  //
  // Buffer size mapping
  //

  public final void setBufferSize(int target, GL caller, long size) {
    // Need to do some similar queries to getBufferSize below
    int buffer = getBoundBufferObject(target, caller);
    if (buffer != 0) {
      setDirectStateBufferSize(buffer, caller, size);
    }
    // We don't know the current buffer state. Note that the buffer
    // state tracker will have made the appropriate OpenGL query if it
    // didn't know what was going on, so at this point we have nothing
    // left to do except drop this piece of information on the floor.
  }

  public final void setDirectStateBufferSize(int buffer, GL caller, long size) {
      bufferSizeMap.put(buffer, size);
  }

  public final long getBufferSize(int target, GL caller) {
    // See whether we know what buffer is currently bound to the given
    // state
    final int buffer = getBoundBufferObject(target, caller);
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
    return tmp[0];
  }

  public final long getDirectStateBufferSize(int buffer, GL caller) {
      return getBufferSizeImpl(0, buffer, caller);
  }

  private final long getBufferSizeImpl(int target, int buffer, GL caller) {
      // See whether we know the size of this buffer object; at this
      // point we almost certainly should if the application is
      // written correctly
      long sz = bufferSizeMap.get(buffer);
      if (sizeNotFound == sz) {
        // For robustness, try to query this value from the GL as we used to
        // FIXME: both functions return 'int' types, which is not suitable,
        // since buffer length is 64bit ?
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
        sz = tmp[0];
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
  public final void clearCachedBufferSizes() {
    bufferSizeMap.clear();
  }

}
