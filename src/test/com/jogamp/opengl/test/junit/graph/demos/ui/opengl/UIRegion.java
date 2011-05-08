/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.test.junit.graph.demos.ui.opengl;

import javax.media.opengl.GL2ES2;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.test.junit.graph.demos.ui.UIShape;
import com.jogamp.opengl.test.junit.graph.demos.ui.UITextShape;

public class UIRegion {
    protected static final int DIRTY_REGION  = 1 << 0 ;    
    protected int dirty = DIRTY_REGION;
    
    private UIShape uiShape;
    private GLRegion region;
    
    public UIRegion(UIShape uis) {
        this.uiShape = uis;
    }
    
    public boolean updateRegion(GL2ES2 gl, RenderState rs, int renderModes) {
        if( uiShape.updateShape() || isRegionDirty() ) {
            destroy(gl, rs);
            if(uiShape instanceof UITextShape) {
                region = ((UITextShape)uiShape).getGlyphString().createRegion(gl, renderModes);    
            } else {
                region = GLRegion.create(uiShape.getShape(), renderModes);
            }
            dirty &= ~DIRTY_REGION;
            return true;
        }
        return false;
    }
    
    public GLRegion getRegion(GL2ES2 gl, RenderState rs, int renderModes) { 
        updateRegion(gl, rs, renderModes);
        return region; 
    }
    
    public boolean isRegionDirty() {
        return 0 != ( dirty & DIRTY_REGION ) ;
    }
    
    public void destroy(GL2ES2 gl, RenderState rs) {
        if(null != region) {
            region.destroy(gl, rs);
            region = null;
        }                    
    }
}
