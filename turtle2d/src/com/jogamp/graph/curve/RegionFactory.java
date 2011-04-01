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
package com.jogamp.graph.curve;

import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import com.jogamp.opengl.util.glsl.ShaderState;

import jogamp.graph.curve.opengl.VBORegionSPES2;
import jogamp.graph.curve.opengl.VBORegion2PES2;


/** RegionFactory to create a Context specific Region implementation. 
 *  
 * @see Region
 */
public class RegionFactory {
	
	/**Create a Region based on the GLContext attached
	 * @param context the current {@link GLContext}
	 * @param st the {@link ShaderState} object
	 * @param type can be one of Region.SINGLE_PASS or Region.TWO_PASS
	 * @return region 
	 */
	public static Region create(GLContext context, ShaderState st, int type){
	    if( !context.isGL2ES2() ) {
	        throw new GLException("At least a GL2ES2 GL context is required. Given: " + context);
	    }
		if( Region.TWO_PASS == type ){
			return new VBORegion2PES2(context, st);
		}
		else{
			return new VBORegionSPES2(context);
		}
	}
}
