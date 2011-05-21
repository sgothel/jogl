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
package jogamp.graph.curve.opengl;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;

/** RegionFactory to create a Context specific Region implementation. 
 *  
 * @see GLRegion
 */
public class RegionFactory {
    
    /**
     * Create a Region using the passed render mode
     * 
     * <p> In case {@link Region#TWO_PASS_RENDERING_BIT} is being requested the default texture unit
     * {@link Region#TWO_PASS_DEFAULT_TEXTURE_UNIT} is being used.</p>
     * 
     * @param rs the RenderState to be used
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, {@link Region#TWO_PASS_RENDERING_BIT} 
     */
    public static GLRegion create(int renderModes) {
        if( 0 != ( Region.TWO_PASS_RENDERING_BIT & renderModes ) ){
            return new VBORegion2PES2(renderModes, Region.TWO_PASS_DEFAULT_TEXTURE_UNIT);
        }
        else{
            return new VBORegionSPES2(renderModes);
        }
    }
        
    public static GLRegion createSinglePass(int renderModes) {
        return new VBORegionSPES2(renderModes);
    }
    
    public static GLRegion createTwoPass(int renderModes, int textureUnit) {
        return new VBORegion2PES2(renderModes, textureUnit);
    }
}
