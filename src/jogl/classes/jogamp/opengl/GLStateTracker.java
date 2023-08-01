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

package jogamp.opengl;

import com.jogamp.opengl.*;

import com.jogamp.common.util.IntIntHashMap;

import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * Tracks as closely as possible OpenGL states.
 * GLStateTracker objects are allocated on a per-OpenGL-context basis.
 * <p>
 * Currently supported states: PixelStorei
 */
public class GLStateTracker {

  /** Minimum value of MAX_CLIENT_ATTRIB_STACK_DEPTH */
  public static final int MIN_CLIENT_ATTRIB_STACK_DEPTH = 16;

  /**
   * PixelStorei states
   */
  public static class PixelStateMap {
      /** static size of pixel state map
      private static final int PIXEL_STATE_MAP_SIZE = 16;
      */
      /** avoid rehash of static size pixel state map */
      private static final int PIXEL_STATE_MAP_CAPACITY = 32;

      private final IntIntHashMap states;

      public PixelStateMap() {
        states = new IntIntHashMap(PIXEL_STATE_MAP_CAPACITY, 0.75f);
        states.setKeyNotFoundValue(0xFFFFFFFF);
      }

      /** Copy constructor */
      public PixelStateMap(final PixelStateMap src) {
        this.states = (IntIntHashMap) src.states.clone();
      }

      /** Set this instance's state using OpenGL default values. */
      public final void clearStates() {
        states.clear();

        // 16 values -> PIXEL_STATE_MAP_SIZE
        states.put(GL.GL_PACK_ALIGNMENT,          4);
        states.put(GL2GL3.GL_PACK_SWAP_BYTES,     GL.GL_FALSE);
        states.put(GL2GL3.GL_PACK_LSB_FIRST,      GL.GL_FALSE);
        states.put(GL2ES3.GL_PACK_ROW_LENGTH,     0);
        states.put(GL2ES3.GL_PACK_SKIP_ROWS,      0);
        states.put(GL2ES3.GL_PACK_SKIP_PIXELS,    0);
        states.put(GL2GL3.GL_PACK_IMAGE_HEIGHT,   0);
        states.put(GL2GL3.GL_PACK_SKIP_IMAGES,    0);

        states.put(GL.GL_UNPACK_ALIGNMENT,        4);
        states.put(GL2GL3.GL_UNPACK_SWAP_BYTES,   GL.GL_FALSE);
        states.put(GL2GL3.GL_UNPACK_LSB_FIRST,    GL.GL_FALSE);
        states.put(GL2ES2.GL_UNPACK_ROW_LENGTH,   0);
        states.put(GL2ES2.GL_UNPACK_SKIP_ROWS,    0);
        states.put(GL2ES2.GL_UNPACK_SKIP_PIXELS,  0);
        states.put(GL2ES3.GL_UNPACK_IMAGE_HEIGHT, 0);
        states.put(GL2ES3.GL_UNPACK_SKIP_IMAGES,  0);
      }
  }


  private volatile boolean enabled = true;

  private PixelStateMap pixelStateMap;
  private final ArrayList<SavedState> stack;

  static class SavedState {

    /**
     * Empty pixel-store state
     */
    private PixelStateMap pixelStateMap;

    /**
     * set (client) pixel-store state, deep copy
     */
    final void setPixelStateMap(final PixelStateMap pixelStateMap) {
        this.pixelStateMap = new PixelStateMap(pixelStateMap);
    }

    /**
     * get (client) pixel-store state, return reference
     */
    final PixelStateMap getPixelStateMap() { return pixelStateMap; }
  }


  public GLStateTracker() {
    pixelStateMap = new PixelStateMap();

    stack = new ArrayList<SavedState>(MIN_CLIENT_ATTRIB_STACK_DEPTH);
    clearStates();
  }

  public final void setEnabled(final boolean on) {
    enabled = on;
  }

  public final boolean isEnabled() {
    return enabled;
  }

  /** Return the local {@link PixelStateMap} */
  public final PixelStateMap getPixelStateMap() { return this.pixelStateMap; }
  /** @return true if found in our map, otherwise false,
   *  which forces the caller to query GL. */
  public final boolean getInt(final int pname, final int[] params, final int params_offset) {
    if(enabled) {
        final int value = pixelStateMap.states.get(pname);
        if(0xFFFFFFFF != value) {
            params[params_offset] = value;
            return true;
        }
    }
    return false;
  }

  /** @return true if found in our map, otherwise false,
   *  which forces the caller to query GL. */
  public final boolean getInt(final int pname, final IntBuffer params, final int dummy) {
    if(enabled) {
        final int value = pixelStateMap.states.get(pname);
        if(0xFFFFFFFF != value) {
            params.put(params.position(), value);
            return true;
        }
    }
    return false;
  }

  public final void setInt(final int pname, final int param) {
    if(enabled) {
        pixelStateMap.states.put(pname, param);
    }
  }

  /**
   * GL_CLIENT_PIXEL_STORE_BIT
   * @param flags
   */
  public final void pushAttrib(final int flags) {
    if(enabled) {
        final SavedState state = new SavedState(); // empty-slot
        if( 0 != (flags&GL2.GL_CLIENT_PIXEL_STORE_BIT) ) {
            // save client pixel-store state
            state.setPixelStateMap(pixelStateMap);
        }
        stack.add(stack.size(), state); // push
    }
  }

  public final void popAttrib() {
    if(enabled) {
        if(stack.isEmpty()) {
            throw new GLException("stack contains no elements");
        }
        final SavedState state = stack.remove(stack.size()-1); // pop

        if(null==state) {
            throw new GLException("null stack element (remaining stack size "+stack.size()+")");
        }
        final PixelStateMap savedPixelStateMap = state.getPixelStateMap();

        if ( null != savedPixelStateMap ) {
            // use pulled client pixel-store state from stack
            pixelStateMap = savedPixelStateMap;
        } // else: empty-slot, not pushed by GL_CLIENT_PIXEL_STORE_BIT
    }
  }

  public final void clearStates() {
    stack.clear();
    pixelStateMap.clearStates();
  }
}

