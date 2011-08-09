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

/**
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
 *
 */

public class GLBufferStateTracker {
  protected static final boolean DEBUG = Debug.isPropertyDefined("jogl.debug.GLBufferStateTracker", true);
  // Maps binding targets to buffer objects. A null value indicates
  // that the binding is unknown. A zero value indicates that it is
  // known that no buffer is bound to the target, according to the 
  // OpenGL specifications. 
  // http://www.opengl.org/sdk/docs/man/xhtml/glBindBuffer.xml
  private IntIntHashMap bindingMap;

  private int[] bufTmp = new int[1];

  public GLBufferStateTracker() {
    bindingMap = new IntIntHashMap();
    bindingMap.setKeyNotFoundValue(0xFFFFFFFF);

    // Start with known unbound targets for known keys
    setBoundBufferObject(GL.GL_ARRAY_BUFFER,         0);
    setBoundBufferObject(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    setBoundBufferObject(GL2.GL_PIXEL_PACK_BUFFER,   0);
    setBoundBufferObject(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
  }

  public final void setBoundBufferObject(int target, int value) {
    bindingMap.put(target, value);
    if (DEBUG) {
      System.err.println();
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
    int value = bindingMap.get(target);
    if (0xFFFFFFFF == value) {
      // User probably either called glPushClientAttrib /
      // glPopClientAttrib or is querying an unknown target. See
      // whether we know how to fetch this state.
      boolean gotQueryTarget = true;
      int queryTarget = 0;
      switch (target) {
        case GL.GL_ARRAY_BUFFER:          queryTarget = GL.GL_ARRAY_BUFFER_BINDING;         break;
        case GL.GL_ELEMENT_ARRAY_BUFFER:  queryTarget = GL.GL_ELEMENT_ARRAY_BUFFER_BINDING; break;
        case GL2.GL_PIXEL_PACK_BUFFER:    queryTarget = GL2.GL_PIXEL_PACK_BUFFER_BINDING;    break;
        case GL2.GL_PIXEL_UNPACK_BUFFER:  queryTarget = GL2.GL_PIXEL_UNPACK_BUFFER_BINDING;  break;
        default:                          gotQueryTarget = false; break;
      }
      if (gotQueryTarget) {
        caller.glGetIntegerv(queryTarget, bufTmp, 0);
        value = bufTmp[0];
        if (DEBUG) {
          System.err.println();
          System.err.println("GLBufferStateTracker.getBoundBufferObject() [queried value]: target 0x" + 
                             Integer.toHexString(target) + " / query 0x"+Integer.toHexString(queryTarget)+
                             " -> mapped bound buffer 0x" + Integer.toHexString(value));
        }
        setBoundBufferObject(target, value);
        return value;
      }
      return 0;
    }
    if (DEBUG) {
      System.err.println();
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
    bindingMap.clear();
        if (DEBUG) {
          System.err.println();
          System.err.println("GLBufferStateTracker.clearBufferObjectState()");
          //Thread.dumpStack();
        }
  }
}
