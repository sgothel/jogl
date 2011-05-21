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
package com.jogamp.graph.curve.opengl;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;

public abstract class RegionRenderer extends Renderer {

    /** 
     * Create a Hardware accelerated Region Renderer.
     * @param rs the used {@link RenderState} 
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, {@link Region#TWO_PASS_RENDERING_BIT} 
     * @return an instance of Region Renderer
     */
    public static RegionRenderer create(RenderState rs, int renderModes) {
        return new jogamp.graph.curve.opengl.RegionRendererImpl01(rs, renderModes);
    }
    
    protected RegionRenderer(RenderState rs, int renderModes) {
        super(rs, renderModes);
    }
    
    
    /** Render an {@link OutlineShape} in 3D space at the position provided
     *  the triangles of the shapes will be generated, if not yet generated
     * @param region the OutlineShape to Render.
     * @param position the initial translation of the outlineShape. 
     * @param texSize texture size for multipass render
     * @throws Exception if HwRegionRenderer not initialized
     */
    public final void draw(GL2ES2 gl, Region region, float[] position, int texSize) {
        if(!isInitialized()) {
            throw new GLException("RegionRenderer: not initialized!");
        }
        if( !areRenderModesCompatible(region) ) {
            throw new GLException("Incompatible render modes, : region modes "+region.getRenderModes()+
                                  " doesn't contain renderer modes "+this.getRenderModes());
        }        
        drawImpl(gl, region, position, texSize);
    }
    
    /**
     * Usually just dispatched the draw call to the Region's draw implementation,
     * e.g. {@link com.jogamp.graph.curve.opengl.GLRegion#draw(GL2ES2, RenderState, int, int, int) GLRegion#draw(GL2ES2, RenderState, int, int, int)}.
     */
    protected abstract void drawImpl(GL2ES2 gl, Region region, float[] position, int texSize);

    @Override
    protected void destroyImpl(GL2ES2 gl) {
        // nop
    }
    
    
}
