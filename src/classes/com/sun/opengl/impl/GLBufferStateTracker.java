/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

import java.util.*;
import javax.media.opengl.*;

/**
 * Tracks as closely as possible which OpenGL buffer object is bound
 * to which binding target in the current OpenGL context.
 * GLBufferStateTracker objects are allocated on a per-GLImpl basis,
 * which is basically identical to a per-OpenGL-context basis
 * (assuming correct usage of the GLImpl objects, which is checked by
 * the DebugGL). This class is used to verify that e.g. the vertex
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
  private static final boolean DEBUG = Debug.debug("GLBufferStateTracker");

  private static final Integer arrayBufferEnum        = new Integer(GL.GL_ARRAY_BUFFER);
  private static final Integer elementArrayBufferEnum = new Integer(GL.GL_ELEMENT_ARRAY_BUFFER);
  private static final Integer pixelPackBufferEnum    = new Integer(GL.GL_PIXEL_PACK_BUFFER);
  private static final Integer pixelUnpackBufferEnum  = new Integer(GL.GL_PIXEL_UNPACK_BUFFER);
  private static final Integer zero                   = new Integer(0);

  // Maps binding targets to buffer objects. A null value indicates
  // that the binding is unknown. A zero value indicates that it is
  // known that no buffer is bound to the target.
  private Map/*<Integer,Integer>*/ bindingMap = new HashMap/*<Integer,Integer>*/();

  private int[] bufTmp = new int[1];

  public GLBufferStateTracker() {
    // Start with known unbound targets for known keys
    bindingMap.put(arrayBufferEnum,        zero);
    bindingMap.put(elementArrayBufferEnum, zero);
    bindingMap.put(pixelPackBufferEnum,    zero);
    bindingMap.put(pixelUnpackBufferEnum,  zero);
  }

  public void setBoundBufferObject(int target, int buffer) {
    Integer key = box(target);
    bindingMap.put(key, box(buffer));
  }

  /** Note: returns an unspecified value if the binding for the
      specified target (e.g. GL_ARRAY_BUFFER) is currently unknown.
      You must use isBoundBufferObjectKnown() to see whether the
      return value is valid. */
  public int getBoundBufferObject(int target, GL caller) {
    Integer key = box(target);
    Integer value = (Integer) bindingMap.get(key);
    if (value == null) {
      // User probably either called glPushClientAttrib /
      // glPopClientAttrib or is querying an unknown target. See
      // whether we know how to fetch this state.
      boolean gotQueryTarget = true;
      int queryTarget = 0;
      switch (target) {
        case GL.GL_ARRAY_BUFFER:          queryTarget = GL.GL_ARRAY_BUFFER_BINDING;         break;
        case GL.GL_ELEMENT_ARRAY_BUFFER:  queryTarget = GL.GL_ELEMENT_ARRAY_BUFFER_BINDING; break;
        case GL.GL_PIXEL_PACK_BUFFER:     queryTarget = GL.GL_PIXEL_PACK_BUFFER_BINDING;    break;
        case GL.GL_PIXEL_UNPACK_BUFFER:   queryTarget = GL.GL_PIXEL_UNPACK_BUFFER_BINDING;  break;
        default:                          gotQueryTarget = false; break;
      }
      if (gotQueryTarget) {
        caller.glGetIntegerv(queryTarget, bufTmp, 0);
        if (DEBUG) {
          System.err.println("GLBufferStateTracker.getBoundBufferObject(): queried bound buffer " +
                             bufTmp[0] +
                             " for query target 0x" + Integer.toHexString(queryTarget));
        }
        setBoundBufferObject(target, bufTmp[0]);
        // Try once more
        return getBoundBufferObject(target, caller);
      }
      return 0;
    }
    return value.intValue();
  }

  /** Indicates whether the binding state for the specified target is
      currently known. Should be called after getBoundBufferObject()
      because that method may change the answer for a given target. */
  public boolean isBoundBufferObjectKnown(int target) {
    return (bindingMap.get(box(target)) != null);
  }

  /** Clears out the known/unknown state of the various buffer object
      binding states. These will be refreshed later on an as-needed
      basis. This is called by the implementations of
      glPushClientAttrib / glPopClientAttrib. Might want to call this
      from GLContext.makeCurrent() in the future to possibly increase
      the robustness of these caches in the face of external native
      code manipulating OpenGL state. */
  public void clearBufferObjectState() {
    bindingMap.clear();
  }

  // FIXME: could largely remove this and use Integer.valueOf() in JDK 5
  private static Integer box(int key) {
    switch (key) {
      case 0:                          return zero;
      case GL.GL_ARRAY_BUFFER:         return arrayBufferEnum;
      case GL.GL_ELEMENT_ARRAY_BUFFER: return elementArrayBufferEnum;
      case GL.GL_PIXEL_PACK_BUFFER:    return pixelPackBufferEnum;
      case GL.GL_PIXEL_UNPACK_BUFFER:  return pixelUnpackBufferEnum;
      default:                         return new Integer(key);
    }
  }
}
