/*
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package javax.media.opengl.fixedfunc;

public class GLPointerFuncUtil { 
    public static final String mgl_Vertex = "mgl_Vertex";
    public static final String mgl_Normal = "mgl_Normal";
    public static final String mgl_Color = "mgl_Color";
    public static final String mgl_MultiTexCoord = "mgl_MultiTexCoord" ;
    public static final String mgl_InterleaveArray = "mgl_InterleaveArray" ; // magic name for interleaved arrays w/ sub-arrays

    /**
     * @param glArrayIndex the fixed function array index
     * @return default fixed function array name 
     */
    public static String getPredefinedArrayIndexName(int glArrayIndex) {
        switch(glArrayIndex) {
            case GLPointerFunc.GL_VERTEX_ARRAY:
                return mgl_Vertex;
            case GLPointerFunc.GL_NORMAL_ARRAY:
                return mgl_Normal;
            case GLPointerFunc.GL_COLOR_ARRAY:
                return mgl_Color;
            case GLPointerFunc.GL_TEXTURE_COORD_ARRAY:
                return mgl_MultiTexCoord;
        }
        return null;
    }
}
