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

import java.util.ArrayList;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;

public class GLGraphicsConfigurationUtil {
    public static final String NV_coverage_sample = "NV_coverage_sample";
    public static final int WINDOW_BIT  = 1 << 0;
    public static final int BITMAP_BIT  = 1 << 1;
    public static final int PBUFFER_BIT = 1 << 2;
    public static final int ALL_BITS    = WINDOW_BIT | BITMAP_BIT | PBUFFER_BIT ;

    public static final StringBuffer winAttributeBits2String(StringBuffer sb, int winattrbits) {
        if(null==sb) {
            sb = new StringBuffer();
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
        }
        return sb;
    }

    /**
     * @return bitmask representing the input boolean in exclusive or logic, ie only one bit will be set
     */
    public static final int getWinAttributeBits(boolean isOnscreen, boolean isPBuffer) {
        int winattrbits = 0;
        if(isOnscreen) {
            winattrbits |= WINDOW_BIT;
        } else if (!isPBuffer) {
            winattrbits |= BITMAP_BIT;
        } else {
            winattrbits |= PBUFFER_BIT;
        }
        return winattrbits;
    }

    /**
     * @see #getWinAttributeBits(boolean, boolean)
     */
    public static final int getWinAttributeBits(GLCapabilitiesImmutable caps) {
        return getWinAttributeBits(caps.isOnscreen(), caps.isPBuffer());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final boolean addGLCapabilitiesPermutations(ArrayList capsBucket, GLCapabilitiesImmutable temp, int winattrbits) {
        int preSize = capsBucket.size();
        if( 0 != ( WINDOW_BIT & winattrbits )  )  {
            GLCapabilities cpy  = (GLCapabilities) temp.cloneMutable();
            cpy.setOnscreen(true);
            capsBucket.add(cpy);
        }
        if( 0 != ( PBUFFER_BIT & winattrbits )  )  {
            GLCapabilities cpy  = (GLCapabilities) temp.cloneMutable();
            cpy.setPBuffer(true);
            capsBucket.add(cpy);
        }
        if( 0 != ( BITMAP_BIT & winattrbits )  )  {
            GLCapabilities cpy  = (GLCapabilities) temp.cloneMutable();
            cpy.setOnscreen(false);
            cpy.setPBuffer(false);
            capsBucket.add(cpy);
        }
        return capsBucket.size() > preSize;
    }

    public static GLCapabilitiesImmutable fixGLCapabilities(GLCapabilitiesImmutable capsRequested, boolean pbufferAvailable)
    {
        if( !capsRequested.isOnscreen() ) {
            return fixOffScreenGLCapabilities(capsRequested, pbufferAvailable);
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable fixOffScreenGLCapabilities(GLCapabilitiesImmutable capsRequested, boolean pbufferAvailable)
    {
        if( capsRequested.getDoubleBuffered() ||
            capsRequested.isOnscreen() ||
            ( !pbufferAvailable && capsRequested.isPBuffer() ) )
        {
            // fix caps ..
            GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setDoubleBuffered(false); // FIXME DBLBUFOFFSCRN
            caps2.setOnscreen(false);
            if(caps2.isPBuffer() && !pbufferAvailable) {
                caps2.setPBuffer(false);
            }
            return caps2;
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable fixGLPBufferGLCapabilities(GLCapabilitiesImmutable capsRequested)
    {
        if( capsRequested.getDoubleBuffered() || capsRequested.isOnscreen() || !capsRequested.isPBuffer()) {
            // fix caps ..
            GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setDoubleBuffered(false); // FIXME DBLBUFOFFSCRN - we don't need to be single buffered ..
            caps2.setOnscreen(false);
            caps2.setPBuffer(true);
            return caps2;
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable fixOpaqueGLCapabilities(GLCapabilitiesImmutable capsRequested, boolean isOpaque)
    {
        GLCapabilities caps2 = null;
        
        if( capsRequested.isBackgroundOpaque() != isOpaque) {
            // fix caps ..
            caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setBackgroundOpaque(isOpaque);
            return caps2;
        }
        return capsRequested;
    }
    
}
