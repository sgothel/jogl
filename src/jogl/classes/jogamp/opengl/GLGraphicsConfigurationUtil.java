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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import com.jogamp.opengl.GLRendererQuirks;

public class GLGraphicsConfigurationUtil {
    public static final String NV_coverage_sample = "NV_coverage_sample";
    public static final int WINDOW_BIT  = 1 << 0;
    public static final int BITMAP_BIT  = 1 << 1;
    public static final int PBUFFER_BIT = 1 << 2;
    public static final int FBO_BIT     = 1 << 3; // generic bit must be mapped to native one at impl. level
    public static final int ALL_BITS    = WINDOW_BIT | BITMAP_BIT | PBUFFER_BIT | FBO_BIT ;

    public static final StringBuilder winAttributeBits2String(StringBuilder sb, final int winattrbits) {
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
    public static final int getWinAttributeBits(boolean isOnscreen, boolean isFBO, boolean isPBuffer, boolean isBitmap) {
        int winattrbits = 0;
        if(isOnscreen) {
            winattrbits |= WINDOW_BIT;
        }
        if(isFBO) {
            winattrbits |= FBO_BIT;
        }
        if(isPBuffer ){
            winattrbits |= PBUFFER_BIT;
        }
        if(isBitmap) {
            winattrbits |= BITMAP_BIT;
        }
        return winattrbits;
    }
    public static final int getWinAttributeBits(GLCapabilitiesImmutable caps) {
        return getWinAttributeBits(caps.isOnscreen(), caps.isFBO(), caps.isPBuffer(), caps.isBitmap());
    } */

    /**
     * @return bitmask representing the input boolean in exclusive or logic, ie only one bit will be set.
     */
    public static final int getExclusiveWinAttributeBits(final boolean isOnscreen, final boolean isFBO, final boolean isPBuffer, final boolean isBitmap) {
        final int winattrbits;
        if(isOnscreen) {
            winattrbits = WINDOW_BIT;
        } else if(isFBO) {
            winattrbits = FBO_BIT;
        } else if(isPBuffer ){
            winattrbits = PBUFFER_BIT;
        } else if(isBitmap) {
            winattrbits = BITMAP_BIT;
        } else {
            throw new InternalError("Empty bitmask");
        }
        return winattrbits;
    }

    /**
     * @see #getExclusiveWinAttributeBits(boolean, boolean, boolean, boolean)
     */
    public static final int getExclusiveWinAttributeBits(final GLCapabilitiesImmutable caps) {
        return getExclusiveWinAttributeBits(caps.isOnscreen(), caps.isFBO(), caps.isPBuffer(), caps.isBitmap());
    }

    public static final GLCapabilities fixWinAttribBitsAndHwAccel(final AbstractGraphicsDevice device, final int winattrbits, final GLCapabilities caps) {
        caps.setBitmap  ( 0 != ( BITMAP_BIT  & winattrbits ) );
        caps.setPBuffer ( 0 != ( PBUFFER_BIT & winattrbits ) );
        caps.setFBO     ( 0 != ( FBO_BIT     & winattrbits ) );
        // we reflect availability semantics, hence setting onscreen at last (maybe overwritten above)!
        caps.setOnscreen( 0 != ( WINDOW_BIT  & winattrbits ) );

        final int accel = GLContext.isHardwareRasterizer( device, caps.getGLProfile() );
        if(0 == accel && caps.getHardwareAccelerated() ) {
            caps.setHardwareAccelerated(false);
        }

        return caps;
    }

    /**
     * Fixes the requested  {@link GLCapabilitiesImmutable} according to on- and offscreen usage.
     * <p>
     * No modification will be made for onscreen usage, for offscreen usage see
     * {@link #fixOffscreenGLCapabilities(GLCapabilitiesImmutable, GLDrawableFactory, AbstractGraphicsDevice)}.
     * </p>
     * @param capsRequested the requested {@link GLCapabilitiesImmutable}
     * @param factory the {@link GLDrawableFactory} used to validate the requested capabilities and later used to create the drawable.
     * @param device the device on which the drawable will be created, maybe null for the {@link GLDrawableFactory#getDefaultDevice() default device}.
     * @return either the given requested {@link GLCapabilitiesImmutable} instance if no modifications were required, or a modified {@link GLCapabilitiesImmutable} instance.
     */
    public static GLCapabilitiesImmutable fixGLCapabilities(final GLCapabilitiesImmutable capsRequested,
                                                            final GLDrawableFactory factory, final AbstractGraphicsDevice device) {
        if( !capsRequested.isOnscreen() ) {
            return fixOffscreenGLCapabilities(capsRequested, factory, device);
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable fixOnscreenGLCapabilities(final GLCapabilitiesImmutable capsRequested)
    {
        if( !capsRequested.isOnscreen() || capsRequested.isFBO() || capsRequested.isPBuffer() || capsRequested.isBitmap() ) {
            // fix caps ..
            final GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setBitmap  (false);
            caps2.setPBuffer (false);
            caps2.setFBO     (false);
            caps2.setOnscreen(true);
            return caps2;
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable fixOffscreenBitOnly(final GLCapabilitiesImmutable capsRequested)
    {
        if( capsRequested.isOnscreen() ) {
            // fix caps ..
            final GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setOnscreen(false);
            return caps2;
        }
        return capsRequested;
    }

    /**
     * Fixes the requested  {@link GLCapabilitiesImmutable} according to:
     * <ul>
     *   <li>offscreen usage</li>
     *   <li>availability of FBO, PBuffer, Bitmap</li>
     *   <li>{@link GLRendererQuirks}</li>
     * </ul>
     * @param capsRequested the requested {@link GLCapabilitiesImmutable}
     * @param factory the {@link GLDrawableFactory} used to validate the requested capabilities and later used to create the drawable.
     * @param device the device on which the drawable will be created, maybe null for the {@link GLDrawableFactory#getDefaultDevice() default device}.
     * @return either the given requested {@link GLCapabilitiesImmutable} instance if no modifications were required, or a modified {@link GLCapabilitiesImmutable} instance.
     */
    public static GLCapabilitiesImmutable fixOffscreenGLCapabilities(final GLCapabilitiesImmutable capsRequested,
                                                                     final GLDrawableFactory factory, AbstractGraphicsDevice device) {
        if(null == device) {
            device = factory.getDefaultDevice();
        }
        final GLProfile glp = capsRequested.getGLProfile();
        final boolean fboAvailable = GLContext.isFBOAvailable(device, glp);
        final boolean pbufferAvailable = factory.canCreateGLPbuffer(device, glp);

        final GLRendererQuirks glrq = factory.getRendererQuirks(device, glp);
        final boolean bitmapAvailable;
        final boolean doubleBufferAvailable;

        if(null != glrq) {
            bitmapAvailable = !glrq.exist(GLRendererQuirks.NoOffscreenBitmap);
            if( capsRequested.getDoubleBuffered() &&
                ( capsRequested.isPBuffer() && glrq.exist(GLRendererQuirks.NoDoubleBufferedPBuffer) ) ||
                ( capsRequested.isBitmap() && glrq.exist(GLRendererQuirks.NoDoubleBufferedBitmap) ) ) {
                doubleBufferAvailable = false;
            } else {
                doubleBufferAvailable = true;
            }
        } else {
            bitmapAvailable = true;
            doubleBufferAvailable = true;
        }

        final boolean auto = !( fboAvailable     && capsRequested.isFBO()     ) &&
                             !( pbufferAvailable && capsRequested.isPBuffer() ) &&
                             !( bitmapAvailable  && capsRequested.isBitmap()  ) ;

        final boolean useFBO     =                           fboAvailable     && ( auto || capsRequested.isFBO()     ) ;
        final boolean usePbuffer = !useFBO                && pbufferAvailable && ( auto || capsRequested.isPBuffer() ) ;
        final boolean useBitmap  = !useFBO && !usePbuffer && bitmapAvailable  && ( auto || capsRequested.isBitmap()  ) ;

        if( capsRequested.isOnscreen() ||
            useFBO != capsRequested.isFBO() ||
            usePbuffer != capsRequested.isPBuffer() ||
            useBitmap != capsRequested.isBitmap() ||
            !doubleBufferAvailable && capsRequested.getDoubleBuffered() )
        {
            // fix caps ..
            final GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setOnscreen(false);
            caps2.setFBO( useFBO );
            caps2.setPBuffer( usePbuffer );
            caps2.setBitmap( useBitmap );
            if( !doubleBufferAvailable ) {
                caps2.setDoubleBuffered(false);
            }
            return caps2;
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable fixGLPBufferGLCapabilities(final GLCapabilitiesImmutable capsRequested)
    {
        if( capsRequested.isOnscreen() ||
            !capsRequested.isPBuffer() ||
            capsRequested.isFBO() )
        {
            // fix caps ..
            final GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setOnscreen(false);
            caps2.setFBO(false);
            caps2.setPBuffer(true);
            caps2.setBitmap(false);
            return caps2;
        }
        return capsRequested;
    }

    /** Fix opaque setting while preserve alpha bits */
    public static GLCapabilities fixOpaqueGLCapabilities(final GLCapabilities capsRequested, final boolean isOpaque)
    {
        if( capsRequested.isBackgroundOpaque() != isOpaque) {
            final int alphaBits = capsRequested.getAlphaBits();
            capsRequested.setBackgroundOpaque(isOpaque);
            capsRequested.setAlphaBits(alphaBits);
        }
        return capsRequested;
    }

    /** Fix double buffered setting */
    public static GLCapabilitiesImmutable fixDoubleBufferedGLCapabilities(final GLCapabilitiesImmutable capsRequested, final boolean doubleBuffered)
    {
        if( capsRequested.getDoubleBuffered() != doubleBuffered) {
            final GLCapabilities caps2 = (GLCapabilities) capsRequested.cloneMutable();
            caps2.setDoubleBuffered(doubleBuffered);
            return caps2;
        }
        return capsRequested;
    }

    public static GLCapabilitiesImmutable clipRGBAGLCapabilities(final GLCapabilitiesImmutable caps, final boolean allowRGB555, final boolean allowAlpha)
    {
        final int iR = caps.getRedBits();
        final int iG = caps.getGreenBits();
        final int iB = caps.getBlueBits();
        final int iA = caps.getAlphaBits();
        final int oR = clipColor(iR, allowRGB555);
        final int oG = clipColor(iG, allowRGB555);
        final int oB = clipColor(iB, allowRGB555);
        final int oA = ( allowAlpha && 0 < iA ) ? oR : 0 ; // align alpha to red if requested and allowed
        if( iR != oR || iG != oG || iB != oB || iA != oA ) {
            final GLCapabilities caps2 = (GLCapabilities) caps.cloneMutable();
            caps2.setRedBits(oR);
            caps2.setGreenBits(oG);
            caps2.setBlueBits(oB);
            caps2.setAlphaBits(oA);
            return caps2;
        }
        return caps;
    }

    public static int clipColor(final int compIn, final boolean allowRGB555) {
        final int compOut;
        if( 5 < compIn || !allowRGB555 ) {
            compOut = 8;
        } else {
            compOut = 5;
        }
        return compOut;
    }

    public static GLCapabilitiesImmutable fixGLProfile(final GLCapabilitiesImmutable caps, final GLProfile glp)
    {
        if( caps.getGLProfile() != glp ) {
            final GLCapabilities caps2 = (GLCapabilities) caps.cloneMutable();
            caps2.setGLProfile(glp);
            return caps2;
        }
        return caps;
    }
}
