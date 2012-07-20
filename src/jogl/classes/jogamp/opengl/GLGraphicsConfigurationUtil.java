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

package jogamp.opengl;

import java.util.List;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;

public class GLGraphicsConfigurationUtil {
    public static final String NV_coverage_sample = "NV_coverage_sample";
    public static final int WINDOW_BIT  = 1 << 0;
    public static final int BITMAP_BIT  = 1 << 1;
    public static final int PBUFFER_BIT = 1 << 2;
    public static final int FBO_BIT     = 1 << 3;
    public static final int ALL_BITS    = WINDOW_BIT | BITMAP_BIT | PBUFFER_BIT | FBO_BIT ;

    public static final StringBuilder winAttributeBits2String(StringBuilder sb, int winattrbits) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        boolean seperator = false;
        if( 0 != ( WINDOW_BIT & winattrbits )  )  {
            sb.append("WINDOW");
            seperator=true;
        }
        if( 0 != ( BITMAP_BIT & winattrbits )  )  {
            if(seperator) {
                sb.append(", ");
            }
            sb.append("BITMAP");
            seperator=true;
        }
        if( 0 != ( PBUFFER_BIT & winattrbits )  )  {
            if(seperator) {
                sb.append(", ");
            }
            sb.append("PBUFFER");
            seperator=true;
        }
        if( 0 != ( FBO_BIT & winattrbits )  )  {
            if(seperator) {
                sb.append(", ");
            }
            sb.append("FBO");
        }
        return sb;
    }

    /**
     * @param isFBO TODO
     * @return bitmask representing the input boolean in exclusive or logic, ie only one bit will be set
     */
    public static final int getWinAttributeBits(boolean isOnscreen, boolean isPBuffer, boolean isFBO) {
        int winattrbits = 0;
        if(isOnscreen) {
            winattrbits |= WINDOW_BIT;
        } else {
            if(isFBO) {
                winattrbits |= FBO_BIT;
            }
            if (!isPBuffer) {
                winattrbits |= BITMAP_BIT;
            } else {
                winattrbits |= PBUFFER_BIT;
            }
        }
        return winattrbits;
    }

    /**
     * @see #getWinAttributeBits(boolean, boolean, boolean)
     */
    public static final int getWinAttributeBits(GLCapabilitiesImmutable caps) {
        return getWinAttributeBits(caps.isOnscreen(), caps.isPBuffer(), false);
    }

    public static final boolean addGLCapabilitiesPermutations(List<GLCapabilitiesImmutable> capsBucket, GLCapabilitiesImmutable temp, int winattrbits) {
        int preSize = capsBucket.size();
        if( 0 != ( WINDOW_BIT & winattrbits )  )  {
            GLCapabilities cpy  = (GLCapabilities) temp.cloneMutable();
            cpy.setOnscreen(true);
            cpy.setPBuffer(false);
            cpy.setFBO(false);
            capsBucket.add(cpy);
        }
        if( 0 != ( PBUFFER_BIT & winattrbits ) || 0 != ( FBO_BIT & winattrbits )  )  {
            GLCapabilities cpy  = (GLCapabilities) temp.cloneMutable();
            cpy.setFBO(0 != ( FBO_BIT & winattrbits ));
            cpy.setPBuffer(0 != ( PBUFFER_BIT & winattrbits ));
            capsBucket.add(cpy);
        }
        if( 0 != ( BITMAP_BIT & winattrbits )  )  {
            GLCapabilities cpy  = (GLCapabilities) temp.cloneMutable();
            cpy.setOnscreen(false);
            cpy.setPBuffer(false);
            cpy.setFBO(false);
            capsBucket.add(cpy);
        }
        return capsBucket.size() > preSize;
    }

    public static GLCapabilitiesImmutable fixGLCapabilities(GLCapabilitiesImmutable capsRequested, boolean fboAvailable, boolean pbufferAvailable)
    {
        if( !capsRequested.isOnscreen() ) {
            return fixOffscreenGLCapabilities(capsRequested, fboAvailable, pbufferAvailable);
        }
        return fixOnscreenGLCapabilities(capsRequested);
    }

    public static GLCapabilitiesImmutable fixOnscreenGLCapabilities(GLCapabilitiesImmutable capsRequested)
    {
        if( !capsRequested.isOnscreen() ) {
            // fix caps ..
            GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setOnscreen(true);
            return caps2;
        }
        return capsRequested;
    }
    
    public static GLCapabilitiesImmutable fixOffscreenGLCapabilities(GLCapabilitiesImmutable capsRequested, boolean fboAvailable, boolean pbufferAvailable)
    {
        if( capsRequested.getDoubleBuffered() ||
            capsRequested.isOnscreen() ||
            ( fboAvailable != capsRequested.isFBO() ) || 
            ( pbufferAvailable != capsRequested.isPBuffer() ) )
        {
            // fix caps ..
            GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setDoubleBuffered(false); // FIXME DBLBUFOFFSCRN
            caps2.setOnscreen(false);
            caps2.setFBO( fboAvailable ); 
            caps2.setPBuffer( pbufferAvailable );
            return caps2;
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable fixGLPBufferGLCapabilities(GLCapabilitiesImmutable capsRequested)
    {
        if( capsRequested.getDoubleBuffered() || capsRequested.isOnscreen() || !capsRequested.isPBuffer() || capsRequested.isFBO() ) {
            // fix caps ..
            GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setDoubleBuffered(false); // FIXME DBLBUFOFFSCRN - we don't need to be single buffered ..
            caps2.setOnscreen(false);
            caps2.setPBuffer(true);
            caps2.setFBO(false);
            return caps2;
        }
        return capsRequested;
    }

    /** Fix opaque setting while preserve alpha bits */
    public static GLCapabilitiesImmutable fixOpaqueGLCapabilities(GLCapabilitiesImmutable capsRequested, boolean isOpaque)
    {
        GLCapabilities caps2 = null;
        
        if( capsRequested.isBackgroundOpaque() != isOpaque) {
            final int alphaBits = capsRequested.getAlphaBits();
            caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setBackgroundOpaque(isOpaque);
            caps2.setAlphaBits(alphaBits);
            return caps2;
        }
        return capsRequested;
    }
    
}
