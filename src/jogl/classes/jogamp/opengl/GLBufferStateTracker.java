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

import com.jogamp.opengl.*;
import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.common.util.PropertyAccess;

/**
 * Tracks as closely as possible which OpenGL buffer object is bound
 * to which binding target in the current OpenGL context.
 * GLBufferStateTracker objects are allocated on a per-OpenGL-context basis.
 * This class is used to verify that e.g. the vertex
 * buffer object extension is in use when the glVertexPointer variant
 * taking a long as argument is called.
 * <p>
 * The buffer binding state is local to it's OpenGL context,
 * i.e. not shared across multiple OpenGL context.
 * Hence this code is thread safe due to no multithreading usage.
 * </p>
 * <p>
 * Note that because the enumerated value used for the binding of a
 * buffer object (e.g. GL_ARRAY_BUFFER) is different than that used to
 * query the binding using glGetIntegerv (e.g.
 * GL_ARRAY_BUFFER_BINDING), then in the face of new binding targets
 * being added to the GL (e.g. GL_TRANSFORM_FEEDBACK_BUFFER_NV) it is
 * impossible to set up a query of the buffer object currently bound
 * to a particular state. It turns out that for some uses, such as
 * finding the size of the currently bound buffer, this doesn't
 * matter, though of course without knowing the buffer object we can't
 * re-associate the queried size with the buffer object ID.
 * </p>
 * <p>
 * For <i>unknown</i> targets such as GL_TRANSFORM_FEEDBACK_BUFFER_NV we return
 * false from this, but we also clear the valid bit and later refresh
 * the binding state if glPushClientAttrib / glPopClientAttrib are
 * called, since we don't want the complexity of tracking stacks of
 * these attributes.
 * </p>
 */

public class GLBufferStateTracker {
  protected static final boolean DEBUG;

  static {
      Debug.initSingleton();
      DEBUG = PropertyAccess.isPropertyDefined("jogl.debug.GLBufferStateTracker", true);
  }

  // Maps binding targets to buffer objects. A null value indicates
  // that the binding is unknown. A zero value indicates that it is
  // known that no buffer is bound to the target, according to the
  // OpenGL specifications.
  // http://www.opengl.org/sdk/docs/man/xhtml/glBindBuffer.xml
  private final IntIntHashMap bindingMap;
  private static final int bindingNotFound = 0xFFFFFFFF;

  private final int[] bufTmp = new int[1];

  public GLBufferStateTracker() {
    bindingMap = new IntIntHashMap();
    bindingMap.setKeyNotFoundValue(bindingNotFound);

    // Start with known unbound targets for known keys
    // setBoundBufferObject(GL2ES3.GL_VERTEX_ARRAY_BINDING, 0); // not using default VAO (removed in GL3 core) - only explicit
    setBoundBufferObject(GL.GL_ARRAY_BUFFER,         0);
    setBoundBufferObject(GL3ES3.GL_DRAW_INDIRECT_BUFFER, 0);
    setBoundBufferObject(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    setBoundBufferObject(GL2ES3.GL_PIXEL_PACK_BUFFER,   0);
    setBoundBufferObject(GL2ES3.GL_PIXEL_UNPACK_BUFFER, 0);
  }


  /**
   *  GL_ARRAY_BUFFER​,
   *  GL_ATOMIC_COUNTER_BUFFER​,
   *  GL_COPY_READ_BUFFER​,
   *  GL_COPY_WRITE_BUFFER​,
   *  GL_DRAW_INDIRECT_BUFFER​,
   *  GL_DISPATCH_INDIRECT_BUFFER​,
   *  GL_ELEMENT_ARRAY_BUFFER​,
   *  GL_PIXEL_PACK_BUFFER​,
   *  GL_PIXEL_UNPACK_BUFFER​,
   *  GL_SHADER_STORAGE_BUFFER​,
   *  GL_TEXTURE_BUFFER​,
   *  GL_TRANSFORM_FEEDBACK_BUFFER​ or
   *  GL_UNIFORM_BUFFER​.
   *
   *  GL_VERTEX_ARRAY_BINDING
   *
   */
  private static final int getQueryName(final int target) {
      switch (target) {
        case GL.GL_ARRAY_BUFFER:                  return GL.GL_ARRAY_BUFFER_BINDING;
        case GL2ES3.GL_ATOMIC_COUNTER_BUFFER:     return GL2ES3.GL_ATOMIC_COUNTER_BUFFER_BINDING;
        case GL2ES3.GL_COPY_READ_BUFFER:          return GL2ES3.GL_COPY_READ_BUFFER_BINDING;
        case GL2ES3.GL_COPY_WRITE_BUFFER:         return GL2ES3.GL_COPY_WRITE_BUFFER_BINDING;
        case GL3ES3.GL_DRAW_INDIRECT_BUFFER:      return GL3ES3.GL_DRAW_INDIRECT_BUFFER_BINDING;
        case GL3ES3.GL_DISPATCH_INDIRECT_BUFFER:  return GL3ES3.GL_DISPATCH_INDIRECT_BUFFER_BINDING;
        case GL.GL_ELEMENT_ARRAY_BUFFER:          return GL.GL_ELEMENT_ARRAY_BUFFER_BINDING;
        case GL2ES3.GL_PIXEL_PACK_BUFFER:         return GL2ES3.GL_PIXEL_PACK_BUFFER_BINDING;
        case GL2ES3.GL_PIXEL_UNPACK_BUFFER:       return GL2ES3.GL_PIXEL_UNPACK_BUFFER_BINDING;
        case GL4.GL_QUERY_BUFFER:                 return GL4.GL_QUERY_BUFFER_BINDING;
        case GL3ES3.GL_SHADER_STORAGE_BUFFER:     return GL3ES3.GL_SHADER_STORAGE_BUFFER_BINDING;
        case GL2GL3.GL_TEXTURE_BUFFER:            return GL2GL3.GL_TEXTURE_BINDING_BUFFER;
        case GL2ES3.GL_TRANSFORM_FEEDBACK_BUFFER: return GL2ES3.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING;
        case GL2ES3.GL_UNIFORM_BUFFER:            return GL2ES3.GL_UNIFORM_BUFFER_BINDING;

        case GL2ES3.GL_VERTEX_ARRAY_BINDING:      return GL2ES3.GL_VERTEX_ARRAY_BINDING;

        default:
            throw new GLException(String.format("GL_INVALID_ENUM​: Invalid binding target 0x%X", target));
      }
  }
  private static final void checkTargetName(final int target) {
      switch (target) {
        case GL.GL_ARRAY_BUFFER:
        case GL2ES3.GL_ATOMIC_COUNTER_BUFFER:
        case GL2ES3.GL_COPY_READ_BUFFER:
        case GL2ES3.GL_COPY_WRITE_BUFFER:
        case GL3ES3.GL_DRAW_INDIRECT_BUFFER:
        case GL3ES3.GL_DISPATCH_INDIRECT_BUFFER:
        case GL.GL_ELEMENT_ARRAY_BUFFER:
        case GL2ES3.GL_PIXEL_PACK_BUFFER:
        case GL2ES3.GL_PIXEL_UNPACK_BUFFER:
        case GL4.GL_QUERY_BUFFER:
        case GL3ES3.GL_SHADER_STORAGE_BUFFER:
        case GL2GL3.GL_TEXTURE_BUFFER:
        case GL2ES3.GL_TRANSFORM_FEEDBACK_BUFFER:
        case GL2ES3.GL_UNIFORM_BUFFER:

        case GL2ES3.GL_VERTEX_ARRAY_BINDING:
            return;

        default:
            throw new GLException(String.format("GL_INVALID_ENUM​: Invalid binding target 0x%X", target));
      }
  }

  /**
   * Must be called when binding a buffer, e.g.:
   * <ul>
   *   <li><code>glBindBuffer</code></li>
   *   <li><code>glBindBufferBase</code></li>
   *   <li><code>glBindBufferRange</code></li>
   * </ul>
   * @param target
   * @param bufferName
   */
  public final void setBoundBufferObject(final int target, final int bufferName) {
    checkTargetName(target);
    final int oldBufferName = bindingMap.put(target, bufferName);
    /***
     * Test for clearing bound buffer states when unbinding VAO,
     * Bug 692 Comment 5 is invalid, i.e. <https://jogamp.org/bugzilla/show_bug.cgi?id=692#c5>.
     * However spec doesn't mention such behavior, and rendering w/ CPU sourced data
     * after unbinding a VAO w/o unbinding the VBOs resulted to no visible image.
     * Leaving code in here for discussion - in case I am wrong.
     *
    final int pre = bindingMap.put(target, bufferName);
    if( GL2ES3.GL_VERTEX_ARRAY_BINDING == target && keyNotFound != pre && 0 == bufferName ) {
        // Unbinding a previous bound VAO leads to unbinding of all buffers!
        bindingMap.put(GL.GL_ARRAY_BUFFER,         0);
        bindingMap.put(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
        bindingMap.put(GL2.GL_PIXEL_PACK_BUFFER,   0);
        bindingMap.put(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        bindingMap.put(GL4.GL_DRAW_INDIRECT_BUFFER, 0);
    } */
    if (DEBUG) {
      System.err.println("GLBufferStateTracker.setBoundBufferObject() target " +
                         toHexString(target) + ": " + toHexString(oldBufferName) + " -> " + toHexString(bufferName));
      // Thread.dumpStack();
    }
  }

  /** Note: returns an unspecified value if the binding for the
      specified target (e.g. GL_ARRAY_BUFFER) is currently unknown.
      You must use isBoundBufferObjectKnown() to see whether the
      return value is valid. */
  public final int getBoundBufferObject(final int target, final GL caller) {
    int value = bindingMap.get(target);
    if (bindingNotFound == value) {
      // User probably either called glPushClientAttrib /
      // glPopClientAttrib or is querying an unknown target. See
      // whether we know how to fetch this state.
      final int queryTarget = getQueryName(target);
      if ( 0 != queryTarget ) {
        final int glerrPre = caller.glGetError(); // clear
        caller.glGetIntegerv(queryTarget, bufTmp, 0);
        final int glerrPost = caller.glGetError(); // be safe, e.g. GL '3.0 Mesa 8.0.4' may produce an error querying GL_PIXEL_UNPACK_BUFFER_BINDING, ignore value
        if(GL.GL_NO_ERROR == glerrPost) {
            value = bufTmp[0];
        } else {
            value = 0;
        }
        if (DEBUG) {
          System.err.println("GLBufferStateTracker.getBoundBufferObject() glerr[pre "+toHexString(glerrPre)+", post "+toHexString(glerrPost)+"], [queried value]: target " +
                             toHexString(target) + " / query "+toHexString(queryTarget)+
                             " -> mapped bound buffer " + toHexString(value));
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
  public final void clear() {
    if (DEBUG) {
      System.err.println("GLBufferStateTracker.clear() - Thread "+Thread.currentThread().getName());
      // Thread.dumpStack();
    }
    bindingMap.clear();
  }
  private final String toHexString(final int i) { return Integer.toHexString(i); }
}
