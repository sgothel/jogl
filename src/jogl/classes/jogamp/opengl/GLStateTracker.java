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

import javax.media.opengl.*;
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
  
  /** static size of pixel state map */
  static final int PIXEL_STATE_MAP_SIZE = 16;
  /** avoid rehash of static size pixel state map */
  static final int PIXEL_STATE_MAP_CAPACITY = 32;
  
  private volatile boolean enabled = true;

  private IntIntHashMap pixelStateMap;
  private final ArrayList<SavedState> stack;
  
  private static class SavedState {

    /**
     * Empty pixel-store state
     */ 
    private IntIntHashMap pixelStateMap;
    
    /**
     * set (client) pixel-store state, deep copy
     */ 
    private void setPixelStateMap(IntIntHashMap pixelStateMap) {
        this.pixelStateMap = (IntIntHashMap) pixelStateMap.clone();
    }
    
    /**
     * get (client) pixel-store state, return reference
     */ 
    private IntIntHashMap getPixelStateMap() { return pixelStateMap; }

  }
  

  public GLStateTracker() {    
    pixelStateMap = new IntIntHashMap(PIXEL_STATE_MAP_CAPACITY, 0.75f);
    pixelStateMap.setKeyNotFoundValue(-1);
    resetStates();
    
    stack = new ArrayList<SavedState>(MIN_CLIENT_ATTRIB_STACK_DEPTH);
  }

  public void clearStates(boolean enable) {
    enabled = enable;    
    pixelStateMap.clear();
  }

  public void setEnabled(boolean on) {
    enabled = on;    
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** @return true if found in our map, otherwise false, 
   *  which forces the caller to query GL. */
  public boolean getInt(int pname, int[] params, int params_offset) {
    if(enabled) {
        int value = pixelStateMap.get(pname);
        if(0 <= value) {
            params[params_offset] = value;
            return true;
        }
    }
    return false;
  }

  /** @return true if found in our map, otherwise false, 
   *  which forces the caller to query GL. */
  public boolean getInt(int pname, IntBuffer params, int dummy) {
    if(enabled) {
        int value = pixelStateMap.get(pname);
        if(0 <= value) {
            params.put(params.position(), value);
            return true;
        }
    }
    return false;
  }

  public void setInt(int pname, int param) {
    if(enabled) {
        pixelStateMap.put(pname, param);
    }
  }

  public void pushAttrib(int flags) {
    if(enabled) {
        SavedState state = new SavedState(); // empty-slot
        if( 0 != (flags&GL2.GL_CLIENT_PIXEL_STORE_BIT) ) {
            // save client pixel-store state
            state.setPixelStateMap(pixelStateMap);
        }
        stack.add(stack.size(), state); // push
    }
  }

  public void popAttrib() {
    if(enabled) {
        if(stack.isEmpty()) {
            throw new GLException("stack contains no elements");
        }
        SavedState state = stack.remove(stack.size()-1); // pop
        
        if(null==state) {
            throw new GLException("null stack element (remaining stack size "+stack.size()+")");
        }

        if ( null != state.getPixelStateMap() ) {
            // use pulled client pixel-store state from stack
            pixelStateMap = state.getPixelStateMap();
        } // else: empty-slot, not pushed by GL_CLIENT_PIXEL_STORE_BIT        
    }
  }

  private void resetStates() {
    pixelStateMap.clear();

    // 16 values -> PIXEL_STATE_MAP_SIZE
    pixelStateMap.put(GL.GL_PACK_ALIGNMENT,          4);
    pixelStateMap.put(GL2GL3.GL_PACK_SWAP_BYTES,     GL.GL_FALSE);
    pixelStateMap.put(GL2GL3.GL_PACK_LSB_FIRST,      GL.GL_FALSE);
    pixelStateMap.put(GL2GL3.GL_PACK_ROW_LENGTH,     0);
    pixelStateMap.put(GL2GL3.GL_PACK_SKIP_ROWS,      0);
    pixelStateMap.put(GL2GL3.GL_PACK_SKIP_PIXELS,    0);
    pixelStateMap.put(GL2GL3.GL_PACK_IMAGE_HEIGHT,   0);
    pixelStateMap.put(GL2GL3.GL_PACK_SKIP_IMAGES,    0);

    pixelStateMap.put(GL.GL_UNPACK_ALIGNMENT,        4);
    pixelStateMap.put(GL2GL3.GL_UNPACK_SWAP_BYTES,   GL.GL_FALSE);
    pixelStateMap.put(GL2GL3.GL_UNPACK_LSB_FIRST,    GL.GL_FALSE);
    pixelStateMap.put(GL2GL3.GL_UNPACK_ROW_LENGTH,   0);
    pixelStateMap.put(GL2GL3.GL_UNPACK_SKIP_ROWS,    0);
    pixelStateMap.put(GL2GL3.GL_UNPACK_SKIP_PIXELS,  0);
    pixelStateMap.put(GL2GL3.GL_UNPACK_IMAGE_HEIGHT, 0);
    pixelStateMap.put(GL2GL3.GL_UNPACK_SKIP_IMAGES,  0);
  }
}

