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
 * Tracks as closely as possible OpenGL states.
 * GLStateTracker objects are allocated on a per-OpenGL-context basis.
 * <p>
 * Currently supported states: PixelStorei
 */

public class GLStateTracker {
  private static final boolean DEBUG = Debug.debug("GLStateTracker");

  private boolean enabled = true;

  private Map/*<Integer,Integer>*/ pixelStateMap = new HashMap/*<Integer,Integer>*/();

  static class SavedState {
    SavedState() {
        this.pixelStateMap = null;
    }
    void putPixelStateMap(Map pixelStateMap) {
        this.pixelStateMap = new HashMap();
        this.pixelStateMap.putAll(pixelStateMap);
    }
    Map getPixelStateMap() { return pixelStateMap; }

    private Map pixelStateMap;
    // private Map otherStateMap;
  }
  private List/*<SavedState>*/ stack = new ArrayList();

  public GLStateTracker() {
    resetStates();
  }

  public void clearStates() {
    pixelStateMap.clear();
  }

  public void setEnabled(boolean on) {
    enabled = on;    
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean getInt(int pname, int[] params, int params_offset) {
    if(enabled) {
        Integer key = boxKey(pname);
        if(null!=key) {
            params[params_offset] = ((Integer) pixelStateMap.get(key)).intValue();
            return true;
        }
    }
    return false;
  }

  public boolean getInt(int pname, java.nio.IntBuffer params, int dummy) {
    if(enabled) {
        Integer key = boxKey(pname);
        if(null!=key) {
            params.put(params.position(), ((Integer) pixelStateMap.get(key)).intValue());
            return true;
        }
    }
    return false;
  }

  public void setInt(int pname, int param) {
    if(enabled) {
        Integer key = boxKey(pname);
        if(null!=key) {
            pixelStateMap.put(key, boxInt(param));
        }
    }
  }

  public void pushAttrib(int flags) {
    if(enabled) {
        SavedState state = new SavedState();
        if( 0 != (flags&GL2.GL_CLIENT_PIXEL_STORE_BIT) ) {
            state.putPixelStateMap(pixelStateMap);
        }
        stack.add(0, state);
    }
  }

  public void popAttrib() {
    if(enabled) {
        if(stack.size()==0) {
            throw new GLException("stack contains no elements");
        }
        SavedState state = (SavedState) stack.remove(0);
        if(null==state) {
            throw new GLException("null stack element (remaining stack size "+stack.size()+")");
        }

        clearStates();

        if ( null != state.getPixelStateMap() ) {
            pixelStateMap.putAll(state.getPixelStateMap());
        }
    }
  }

  private static final Integer GL_PACK_SWAP_BYTES   = new Integer(GL2GL3.GL_PACK_SWAP_BYTES);
  private static final Integer GL_PACK_LSB_FIRST    = new Integer(GL2GL3.GL_PACK_LSB_FIRST);
  private static final Integer GL_PACK_ROW_LENGTH   = new Integer(GL2GL3.GL_PACK_ROW_LENGTH);
  private static final Integer GL_PACK_SKIP_ROWS    = new Integer(GL2GL3.GL_PACK_SKIP_ROWS);
  private static final Integer GL_PACK_SKIP_PIXELS  = new Integer(GL2GL3.GL_PACK_SKIP_PIXELS);
  private static final Integer GL_PACK_ALIGNMENT    = new Integer(GL.GL_PACK_ALIGNMENT);
  private static final Integer GL_PACK_IMAGE_HEIGHT = new Integer(GL2GL3.GL_PACK_IMAGE_HEIGHT);
  private static final Integer GL_PACK_SKIP_IMAGES  = new Integer(GL2GL3.GL_PACK_SKIP_IMAGES);

  private static final Integer GL_UNPACK_SWAP_BYTES   = new Integer(GL2GL3.GL_UNPACK_SWAP_BYTES);
  private static final Integer GL_UNPACK_LSB_FIRST    = new Integer(GL2GL3.GL_UNPACK_LSB_FIRST);
  private static final Integer GL_UNPACK_ROW_LENGTH   = new Integer(GL2GL3.GL_UNPACK_ROW_LENGTH);
  private static final Integer GL_UNPACK_SKIP_ROWS    = new Integer(GL2GL3.GL_UNPACK_SKIP_ROWS);
  private static final Integer GL_UNPACK_SKIP_PIXELS  = new Integer(GL2GL3.GL_UNPACK_SKIP_PIXELS);
  private static final Integer GL_UNPACK_ALIGNMENT    = new Integer(GL.GL_UNPACK_ALIGNMENT);
  private static final Integer GL_UNPACK_IMAGE_HEIGHT = new Integer(GL2GL3.GL_UNPACK_IMAGE_HEIGHT);
  private static final Integer GL_UNPACK_SKIP_IMAGES  = new Integer(GL2GL3.GL_UNPACK_SKIP_IMAGES);

  private static final Integer zero                   = new Integer(0);
  private static final Integer one                    = new Integer(1);

  private static Integer boxKey(int key) {
    switch (key) {
      case 0:                          return zero;
      case GL2GL3.GL_PACK_SWAP_BYTES:     return GL_PACK_SWAP_BYTES;
      case GL2GL3.GL_PACK_LSB_FIRST:      return GL_PACK_LSB_FIRST;
      case GL2GL3.GL_PACK_ROW_LENGTH:     return GL_PACK_ROW_LENGTH;
      case GL2GL3.GL_PACK_SKIP_ROWS:      return GL_PACK_SKIP_ROWS;
      case GL2GL3.GL_PACK_SKIP_PIXELS:    return GL_PACK_SKIP_PIXELS;
      case GL.GL_PACK_ALIGNMENT:          return GL_PACK_ALIGNMENT;
      case GL2GL3.GL_PACK_IMAGE_HEIGHT:   return GL_PACK_IMAGE_HEIGHT;
      case GL2GL3.GL_PACK_SKIP_IMAGES:    return GL_PACK_SKIP_IMAGES;

      case GL2GL3.GL_UNPACK_SWAP_BYTES:   return GL_UNPACK_SWAP_BYTES;
      case GL2GL3.GL_UNPACK_LSB_FIRST:    return GL_UNPACK_LSB_FIRST;
      case GL2GL3.GL_UNPACK_ROW_LENGTH:   return GL_UNPACK_ROW_LENGTH;
      case GL2GL3.GL_UNPACK_SKIP_ROWS:    return GL_UNPACK_SKIP_ROWS;
      case GL2GL3.GL_UNPACK_SKIP_PIXELS:  return GL_UNPACK_SKIP_PIXELS;
      case GL.GL_UNPACK_ALIGNMENT:        return GL_UNPACK_ALIGNMENT;
      case GL2GL3.GL_UNPACK_IMAGE_HEIGHT: return GL_UNPACK_IMAGE_HEIGHT;
      case GL2GL3.GL_UNPACK_SKIP_IMAGES:  return GL_UNPACK_SKIP_IMAGES;

      default: return null;
    }
  }

  public void resetStates() {
    pixelStateMap.clear();

    pixelStateMap.put(GL_PACK_SWAP_BYTES,   zero /* GL_FALSE */);
    pixelStateMap.put(GL_PACK_LSB_FIRST,    zero /* GL_FALSE */);
    pixelStateMap.put(GL_PACK_ROW_LENGTH,   zero);
    pixelStateMap.put(GL_PACK_SKIP_ROWS,    zero);
    pixelStateMap.put(GL_PACK_SKIP_PIXELS,  zero);
    pixelStateMap.put(GL_PACK_ALIGNMENT,    new Integer(4));
    pixelStateMap.put(GL_PACK_IMAGE_HEIGHT, zero);
    pixelStateMap.put(GL_PACK_SKIP_IMAGES,  zero);

    pixelStateMap.put(GL_UNPACK_SWAP_BYTES,   zero /* GL_FALSE */);
    pixelStateMap.put(GL_UNPACK_LSB_FIRST,    zero /* GL_FALSE */);
    pixelStateMap.put(GL_UNPACK_ROW_LENGTH,   zero);
    pixelStateMap.put(GL_UNPACK_SKIP_ROWS,    zero);
    pixelStateMap.put(GL_UNPACK_SKIP_PIXELS,  zero);
    pixelStateMap.put(GL_UNPACK_ALIGNMENT,    new Integer(4));
    pixelStateMap.put(GL_UNPACK_IMAGE_HEIGHT, zero);
    pixelStateMap.put(GL_UNPACK_SKIP_IMAGES,  zero);
  }

  private static Integer boxInt(int value) {
    switch (value) {
      case 0:           return zero;
      case 1:           return one;

      default: return new Integer(value);
    }
  }
}
