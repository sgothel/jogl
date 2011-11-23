/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
 */

package jogamp.opengl.windows.wgl;

import java.util.ArrayList;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.MutableGraphicsConfiguration;
import jogamp.nativewindow.windows.DWM_BLURBEHIND;
import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.MARGINS;
import jogamp.nativewindow.windows.PIXELFORMATDESCRIPTOR;
import jogamp.opengl.GLGraphicsConfigurationUtil;

public class WindowsWGLGraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {
    // Keep this under the same debug flag as the drawable factory for convenience
    protected static final boolean DEBUG = jogamp.opengl.Debug.debug("GraphicsConfiguration");
    
    protected static final int MAX_PFORMATS = 256;
    protected static final int MAX_ATTRIBS  = 256;

    private GLCapabilitiesChooser chooser;
    private boolean isDetermined = false;
    private boolean isExternal = false;

    WindowsWGLGraphicsConfiguration(AbstractGraphicsScreen screen, 
                                    GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested,
                                    GLCapabilitiesChooser chooser) {
        super(screen, capsChosen, capsRequested);
        this.chooser=chooser;
        this.isDetermined = false;
    }

    WindowsWGLGraphicsConfiguration(AbstractGraphicsScreen screen,
                                    WGLGLCapabilities capsChosen, GLCapabilitiesImmutable capsRequested) {
        super(screen, capsChosen, capsRequested);
        setCapsPFD(capsChosen);
        this.chooser=null;
    }


    static WindowsWGLGraphicsConfiguration createFromExternal(GLDrawableFactory _factory, long hdc, int pfdID,
                                                             GLProfile glp, AbstractGraphicsScreen screen, boolean onscreen)
    {
        if(_factory==null) {
            throw new GLException("Null factory");
        }
        if(hdc==0) {
            throw new GLException("Null HDC");
        }
        if(pfdID<=0) {
            throw new GLException("Invalid pixelformat id "+pfdID);
        }
        if(null==glp) {
          glp = GLProfile.getDefault(screen.getDevice());
        }
        WindowsWGLDrawableFactory factory = (WindowsWGLDrawableFactory) _factory;
        AbstractGraphicsDevice device = screen.getDevice();
        WindowsWGLDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResource(device);
        boolean hasARB = null != sharedResource && sharedResource.hasARBPixelFormat();

        WGLGLCapabilities caps = null;

        if(hasARB) {
            caps = wglARBPFID2GLCapabilities(sharedResource, hdc, pfdID, glp, onscreen, true /* pbuffer */);
        } else {
            caps = PFD2GLCapabilities(glp, hdc, pfdID, onscreen);
        }
        if(null==caps) {
            throw new GLException("Couldn't choose Capabilities by: HDC 0x"+Long.toHexString(hdc)+
                                  ", pfdID "+pfdID+", onscreen "+onscreen+", hasARB "+hasARB);
        }

        WindowsWGLGraphicsConfiguration cfg = new WindowsWGLGraphicsConfiguration(screen, caps, caps);
        cfg.markExternal();
        return cfg;
    }

    public Object clone() {
        return super.clone();
    }

    /**
     * Updates the graphics configuration in case it has been determined yet.<br>
     * Uses the NativeSurface's HDC.<br>
     * Ensures that a PIXELFORMAT is set.
     *
     * @param factory
     * @param ns
     * @param pfIDs optional pool of preselected PixelFormat IDs, maybe null for unrestricted selection
     *
     * @see #isDetermined()
     * @see #isExternal()
     */
    public final void updateGraphicsConfiguration(GLDrawableFactory factory, NativeSurface ns, int[] pfIDs) {
        WindowsWGLGraphicsConfigurationFactory.updateGraphicsConfiguration(chooser, factory, ns, pfIDs);
    }

    /**
     * Preselect the graphics configuration in case it has been determined yet.<br>
     * Uses a shared device's HDC and the given pfdIDs to preselect the pfd.
     * No PIXELFORMAT is set.
     *
     * @param factory
     * @param pfIDs optional pool of preselected PixelFormat IDs, maybe null for unrestricted selection
     *
     * @see #isDetermined()
     */
    public final void preselectGraphicsConfiguration(GLDrawableFactory factory, int[] pfdIDs) {
        AbstractGraphicsDevice device = getScreen().getDevice();
        WindowsWGLGraphicsConfigurationFactory.preselectGraphicsConfiguration(chooser, factory, device, this, pfdIDs);
    }

    /**
     * Sets the hdc's PixelFormat, this configuration's capabilities and marks it as determined.
     */
    final void setPixelFormat(long hdc, WGLGLCapabilities caps) {
        if (0 == hdc) {
            throw new GLException("Error: HDC is null");
        }
    
        if (!GDI.SetPixelFormat(hdc, caps.getPFDID(), caps.getPFD())) {
            throw new GLException("Unable to set pixel format " + caps +
                                  " for device context " + toHexString(hdc) +
                                  ": error code " + GDI.GetLastError());
        }
        if(!caps.isBackgroundOpaque()) {
            final long hwnd = GDI.WindowFromDC(hdc);
            DWM_BLURBEHIND bb = DWM_BLURBEHIND.create();
            bb.setDwFlags(GDI.DWM_BB_ENABLE);
            bb.setFEnable(1);
            boolean ok = GDI.DwmEnableBlurBehindWindow(hwnd, bb);
            if(ok) {
                MARGINS m = MARGINS.create();
                m.setCxLeftWidth(-1);
                m.setCxRightWidth(-1);
                m.setCyBottomHeight(-1);
                m.setCyTopHeight(-1);
                ok = GDI.DwmExtendFrameIntoClientArea(hwnd, m);
            }
            if(DEBUG) {
                System.err.println("!!! translucency enabled on wnd: 0x"+Long.toHexString(hwnd)+" - ok: "+ok);
            }
        }
        if (DEBUG) {
            System.err.println("!!! setPixelFormat (ARB): hdc "+toHexString(hdc) +", "+caps);
        }
        setCapsPFD(caps);
    }
    
    /**
     * Only sets this configuration's capabilities and marks it as determined,
     * the actual pixelformat is not set.
     */
    final void setCapsPFD(WGLGLCapabilities caps) {
        setChosenCapabilities(caps);
        this.isDetermined = true;
        if (DEBUG) {
            System.err.println("*** setCapsPFD: "+caps);
        }
    }

    /**
     * External configuration's HDC pixelformat shall not be modified
     */
    public final boolean isExternal() { return isExternal; }
    
    final void markExternal() {
        this.isExternal=true;
    }
    
    /**
     * Determined configuration states set target capabilties via {@link #setCapsPFD(WGLGLCapabilities)},
     * but does not imply a set pixelformat.
     * 
     * @see #setPixelFormat(long, WGLGLCapabilities) 
     * @see #setCapsPFD(WGLGLCapabilities)
     */
    public final boolean isDetermined() { return isDetermined; }
    
    public final PIXELFORMATDESCRIPTOR getPixelFormat()   { return isDetermined ? ((WGLGLCapabilities)capabilitiesChosen).getPFD() : null; }
    public final int getPixelFormatID() { return isDetermined ? ((WGLGLCapabilities)capabilitiesChosen).getPFDID() : 0; }
    public final boolean isChoosenByARB() { return isDetermined ? ((WGLGLCapabilities)capabilitiesChosen).isSetByARB() : false; }

    static int fillAttribsForGeneralWGLARBQuery(WindowsWGLDrawableFactory.SharedResource sharedResource, int[] iattributes) {
        int niattribs = 0;
        iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_WINDOW_ARB;
        if(sharedResource.hasARBPBuffer()) {
            iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_PBUFFER_ARB;
        }
        iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_BITMAP_ARB;
        iattributes[niattribs++] = WGLExt.WGL_ACCELERATION_ARB;
        iattributes[niattribs++] = WGLExt.WGL_SUPPORT_OPENGL_ARB;
        iattributes[niattribs++] = WGLExt.WGL_DEPTH_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_STENCIL_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_DOUBLE_BUFFER_ARB;
        iattributes[niattribs++] = WGLExt.WGL_STEREO_ARB;
        iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
        iattributes[niattribs++] = WGLExt.WGL_RED_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_GREEN_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_BLUE_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_ALPHA_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_ACCUM_RED_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_ACCUM_GREEN_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_ACCUM_BLUE_BITS_ARB;
        iattributes[niattribs++] = WGLExt.WGL_ACCUM_ALPHA_BITS_ARB;
        if(sharedResource.hasARBMultisample()) {
            iattributes[niattribs++] = WGLExt.WGL_SAMPLE_BUFFERS_ARB;
            iattributes[niattribs++] = WGLExt.WGL_SAMPLES_ARB;
        }
            
        if(sharedResource.hasARBPBuffer()) {
            WindowsWGLContext sharedCtx = sharedResource.getContext();
            if(null != sharedCtx && sharedCtx.isExtensionAvailable(WindowsWGLDrawableFactory.WGL_NV_float_buffer)) {
                // pbo float buffer
                iattributes[niattribs++] = WGLExt.WGL_FLOAT_COMPONENTS_NV; // nvidia
            }
        }

        return niattribs;
    }
    
    static boolean wglARBPFIDValid(WindowsWGLContext sharedCtx, long hdc, int pfdID) {
        int[] in = new int[1];
        int[] out = new int[1];
        in[0] = WGLExt.WGL_COLOR_BITS_ARB;
        if (!sharedCtx.getWGLExt().wglGetPixelFormatAttribivARB(hdc, pfdID, 0, 1, in, 0, out, 0)) {
            // Some GPU's falsely fails with a zero error code (success)
            return GDI.GetLastError() == GDI.ERROR_SUCCESS ;
        }
        return true;
    }

    static int[] wglAllARBPFIDs(WindowsWGLContext sharedCtx, long hdc) {
        int[] iattributes = new int[1];
        int[] iresults = new int[1];

        WGLExt wglExt = sharedCtx.getWGLExt();
        iattributes[0] = WGLExt.WGL_NUMBER_PIXEL_FORMATS_ARB;
        if (!wglExt.wglGetPixelFormatAttribivARB(hdc, 0, 0, 1, iattributes, 0, iresults, 0)) {
            if(DEBUG) {
                System.err.println("GetPixelFormatAttribivARB: Failed - HDC 0x" + Long.toHexString(hdc) +
                                  ", LastError: " + GDI.GetLastError());
                Thread.dumpStack();
            }
            return null;
        }
        int numFormats = iresults[0];
        if(0 == numFormats) {
            if(DEBUG) {
                System.err.println("GetPixelFormatAttribivARB: No formats - HDC 0x" + Long.toHexString(hdc) +
                                  ", LastError: " + GDI.GetLastError());
                Thread.dumpStack();
            }
            return null;
        }
        int[] pfdIDs = new int[numFormats];
        for (int i = 0; i < numFormats; i++) {
            pfdIDs[i] = 1 + i;
        }
        return pfdIDs;
    }

    static WGLGLCapabilities wglARBPFID2GLCapabilities(WindowsWGLDrawableFactory.SharedResource sharedResource,
                                                       long hdc, int pfdID,
                                                       GLProfile glp, boolean onscreen, boolean usePBuffer) {
        if (!sharedResource.hasARBPixelFormat()) {
            return null;
        }

        int[] iattributes = new int  [2*MAX_ATTRIBS];
        int[] iresults    = new int  [2*MAX_ATTRIBS];

        int niattribs = fillAttribsForGeneralWGLARBQuery(sharedResource, iattributes);

        if (!sharedResource.getContext().getWGLExt().wglGetPixelFormatAttribivARB(hdc, pfdID, 0, niattribs, iattributes, 0, iresults, 0)) {
            throw new GLException("wglARBPFID2GLCapabilities: Error getting pixel format attributes for pixel format " + pfdID + 
                                  " of device context " + toHexString(hdc) + ", werr " + GDI.GetLastError());
        }
        ArrayList<WGLGLCapabilities> bucket = new ArrayList<WGLGLCapabilities>(1);
        final int winattrbits = GLGraphicsConfigurationUtil.getWinAttributeBits(onscreen, usePBuffer);
        if(AttribList2GLCapabilities(bucket, glp, hdc, pfdID, iattributes, niattribs, iresults, winattrbits)) {
            return bucket.get(0);
        }
        return null;
    }

    static int[] wglChoosePixelFormatARB(long hdc, WindowsWGLDrawableFactory.SharedResource sharedResource,
                                         GLCapabilitiesImmutable capabilities,
                                         int[] iattributes, int accelerationMode, float[] fattributes)
    {

        if ( !WindowsWGLGraphicsConfiguration.GLCapabilities2AttribList(capabilities,
                iattributes, sharedResource, accelerationMode, null))
        {
            if (DEBUG) {
                System.err.println("wglChoosePixelFormatARB: GLCapabilities2AttribList failed: " + GDI.GetLastError());
                Thread.dumpStack();
            }
            return null;
        }

        int[] pformatsTmp = new int[WindowsWGLGraphicsConfiguration.MAX_PFORMATS];
        int[] numFormatsTmp = new int[1];
        if ( !sharedResource.getContext().getWGLExt().wglChoosePixelFormatARB(hdc, iattributes, 0,
                                                                fattributes, 0,
                                                                WindowsWGLGraphicsConfiguration.MAX_PFORMATS,
                                                                pformatsTmp, 0, numFormatsTmp, 0))
        {
            if (DEBUG) {
                System.err.println("wglChoosePixelFormatARB: wglChoosePixelFormatARB failed: " + GDI.GetLastError());
                Thread.dumpStack();
            }
            return null;
        }
        int numFormats = numFormatsTmp[0];
        int[] pformats = null;
        if( 0 < numFormats ) {
            pformats = new int[numFormats];
            System.arraycopy(pformatsTmp, 0, pformats, 0, numFormats);
        }
        if (DEBUG) {
            System.err.println("wglChoosePixelFormatARB: NumFormats (wglChoosePixelFormatARB) accelMode 0x"
                    + Integer.toHexString(accelerationMode) + ": " + numFormats);
            for (int i = 0; i < numFormats; i++) {
                WGLGLCapabilities dbgCaps0 = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(
                                                sharedResource, hdc, pformats[i],
                                                capabilities.getGLProfile(), capabilities.isOnscreen(), capabilities.isPBuffer());
                System.err.println("pixel format " + pformats[i] + " (index " + i + "): " + dbgCaps0);
            }
        }
        return pformats;
    }

    static List /*<GLCapabilitiesImmutable>*/ wglARBPFIDs2GLCapabilities(WindowsWGLDrawableFactory.SharedResource sharedResource,
                                                                         long hdc, int[] pfdIDs, GLProfile glp, boolean onscreen, boolean usePBuffer) {
        final int winattrbits = GLGraphicsConfigurationUtil.getWinAttributeBits(onscreen, usePBuffer);
        return wglARBPFIDs2GLCapabilitiesImpl(sharedResource, hdc, pfdIDs, glp, winattrbits);
    }

    static List /*<GLCapabilitiesImmutable>*/ wglARBPFIDs2AllGLCapabilities(WindowsWGLDrawableFactory.SharedResource sharedResource,
                                                                            long hdc, int[] pfdIDs, GLProfile glp) {
        return wglARBPFIDs2GLCapabilitiesImpl(sharedResource, hdc, pfdIDs, glp, GLGraphicsConfigurationUtil.ALL_BITS);
    }
    
    private static List /*<GLCapabilitiesImmutable>*/ wglARBPFIDs2GLCapabilitiesImpl(WindowsWGLDrawableFactory.SharedResource sharedResource,
                                                                                     long hdc, int[] pfdIDs, GLProfile glp, int winattrbits) {
        if (!sharedResource.hasARBPixelFormat()) {
            return null;
        }
        final int numFormats = pfdIDs.length;

        int[] iattributes = new int [2*MAX_ATTRIBS];
        int[] iresults    = new int [2*MAX_ATTRIBS];
        int niattribs = fillAttribsForGeneralWGLARBQuery(sharedResource, iattributes);

        ArrayList<GLCapabilitiesImmutable> bucket = new ArrayList<GLCapabilitiesImmutable>();

        for(int i = 0; i<numFormats; i++) {
            if ( pfdIDs[i] >= 1 &&
                 sharedResource.getContext().getWGLExt().wglGetPixelFormatAttribivARB(hdc, pfdIDs[i], 0, niattribs, iattributes, 0, iresults, 0) ) {
                AttribList2GLCapabilities(bucket, glp, hdc, pfdIDs[i], iattributes, niattribs, iresults, winattrbits);
            } else if (DEBUG) {
                System.err.println("wglARBPFIDs2GLCapabilities: Cannot get pixel format attributes for pixel format " +
                                   i + "/" + numFormats + ": " + pfdIDs[i] + ", " +
                                   GLGraphicsConfigurationUtil.winAttributeBits2String(null, winattrbits).toString());
            }
        }
        return bucket;
    }

    static boolean GLCapabilities2AttribList(GLCapabilitiesImmutable caps,
                                             int[] iattributes,
                                             WindowsWGLDrawableFactory.SharedResource sharedResource,
                                             int accelerationValue,
                                             int[] floatMode) throws GLException {
        if (!sharedResource.hasARBPixelFormat()) {
          return false;
        }

        boolean onscreen = caps.isOnscreen();
        boolean pbuffer = caps.isPBuffer();

        int niattribs = 0;

        iattributes[niattribs++] = WGLExt.WGL_SUPPORT_OPENGL_ARB;
        iattributes[niattribs++] = GL.GL_TRUE;
        if(accelerationValue>0) {
            iattributes[niattribs++] = WGLExt.WGL_ACCELERATION_ARB;
            iattributes[niattribs++] = accelerationValue;
        }
        if (onscreen) {
          iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_WINDOW_ARB;
          iattributes[niattribs++] = GL.GL_TRUE;
        } else if (pbuffer && sharedResource.hasARBPBuffer()) {
          iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_PBUFFER_ARB;
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = WGLExt.WGL_DRAW_TO_BITMAP_ARB;
          iattributes[niattribs++] = GL.GL_TRUE;
        }

        iattributes[niattribs++] = WGLExt.WGL_DOUBLE_BUFFER_ARB;
        if (caps.getDoubleBuffered()) {
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = GL.GL_FALSE;
        }

        iattributes[niattribs++] = WGLExt.WGL_STEREO_ARB;
        if (caps.getStereo()) {
          iattributes[niattribs++] = GL.GL_TRUE;
        } else {
          iattributes[niattribs++] = GL.GL_FALSE;
        }
        
        iattributes[niattribs++] = WGLExt.WGL_DEPTH_BITS_ARB;
        iattributes[niattribs++] = caps.getDepthBits();
        iattributes[niattribs++] = WGLExt.WGL_RED_BITS_ARB;
        iattributes[niattribs++] = caps.getRedBits();
        iattributes[niattribs++] = WGLExt.WGL_GREEN_BITS_ARB;
        iattributes[niattribs++] = caps.getGreenBits();
        iattributes[niattribs++] = WGLExt.WGL_BLUE_BITS_ARB;
        iattributes[niattribs++] = caps.getBlueBits();
        iattributes[niattribs++] = WGLExt.WGL_ALPHA_BITS_ARB;
        iattributes[niattribs++] = caps.getAlphaBits();
        iattributes[niattribs++] = WGLExt.WGL_STENCIL_BITS_ARB;
        iattributes[niattribs++] = caps.getStencilBits();
        if (caps.getAccumRedBits()   > 0 ||
            caps.getAccumGreenBits() > 0 ||
            caps.getAccumBlueBits()  > 0 ||
            caps.getAccumAlphaBits() > 0) {
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_BITS_ARB;
          iattributes[niattribs++] = (caps.getAccumRedBits() +
                                      caps.getAccumGreenBits() +
                                      caps.getAccumBlueBits() +
                                      caps.getAccumAlphaBits());
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_RED_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumRedBits();
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_GREEN_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumGreenBits();
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_BLUE_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumBlueBits();
          iattributes[niattribs++] = WGLExt.WGL_ACCUM_ALPHA_BITS_ARB;
          iattributes[niattribs++] = caps.getAccumAlphaBits();
        }

        if (caps.getSampleBuffers() && sharedResource.hasARBMultisample()) {
            iattributes[niattribs++] = WGLExt.WGL_SAMPLE_BUFFERS_ARB;
            iattributes[niattribs++] = GL.GL_TRUE;
            iattributes[niattribs++] = WGLExt.WGL_SAMPLES_ARB;
            iattributes[niattribs++] = caps.getNumSamples();
        }

        boolean rtt      = caps.getPbufferRenderToTexture();
        boolean rect     = caps.getPbufferRenderToTextureRectangle();
        boolean useFloat = caps.getPbufferFloatingPointBuffers();
        boolean ati      = false;
        boolean nvidia   = false;
        if (pbuffer && sharedResource.hasARBPBuffer()) {
          // Check some invariants and set up some state
          if (rect && !rtt) {
            throw new GLException("Render-to-texture-rectangle requires render-to-texture to be specified");
          }

          WindowsWGLContext sharedCtx = sharedResource.getContext();
          if (rect) {
            if (!sharedCtx.isExtensionAvailable("GL_NV_texture_rectangle")) {
              throw new GLException("Render-to-texture-rectangle requires GL_NV_texture_rectangle extension");
            }
          }

          if (useFloat) {
            // Prefer NVidia extension over ATI
            nvidia = sharedCtx.isExtensionAvailable(WindowsWGLDrawableFactory.WGL_NV_float_buffer);
            if(nvidia) {
              floatMode[0] = GLPbuffer.NV_FLOAT;
            } else {
                ati = sharedCtx.isExtensionAvailable("WGL_ATI_pixel_format_float");
                if(ati) {
                    floatMode[0] = GLPbuffer.ATI_FLOAT;
                } else {
                    throw new GLException("Floating-point pbuffers not supported by this hardware");                    
                }
            }
            
            if (DEBUG) {
              System.err.println("Using " + (ati ? "ATI" : ( nvidia ? "NVidia" : "NONE" ) ) + " floating-point extension");
            }
          }

          // See whether we need to change the pixel type to support ATI's
          // floating-point pbuffers
          if (useFloat && ati) {
            if (rtt) {
              throw new GLException("Render-to-floating-point-texture not supported on ATI hardware");
            } else {
              iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
              iattributes[niattribs++] = WGLExt.WGL_TYPE_RGBA_FLOAT_ARB;
            }
          } else {
            if (!rtt) {
              // Currently we don't support non-truecolor visuals in the
              // GLCapabilities, so we don't offer the option of making
              // color-index pbuffers.
              iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
              iattributes[niattribs++] = WGLExt.WGL_TYPE_RGBA_ARB;
            }
          }

          if (useFloat && nvidia) {
            iattributes[niattribs++] = WGLExt.WGL_FLOAT_COMPONENTS_NV;
            iattributes[niattribs++] = GL.GL_TRUE;
          }

          if (rtt) {
            if (useFloat) {
              assert(!ati);
              assert(nvidia);
              if (!rect) {
                throw new GLException("Render-to-floating-point-texture only supported on NVidia hardware with render-to-texture-rectangle");
              }
              iattributes[niattribs++] = WGLExt.WGL_BIND_TO_TEXTURE_RECTANGLE_FLOAT_RGB_NV;
              iattributes[niattribs++] = GL.GL_TRUE;
            } else {
              iattributes[niattribs++] = rect ? WGLExt.WGL_BIND_TO_TEXTURE_RECTANGLE_RGB_NV : WGLExt.WGL_BIND_TO_TEXTURE_RGB_ARB;
              iattributes[niattribs++] = GL.GL_TRUE;
            }
          }
        } else {
          iattributes[niattribs++] = WGLExt.WGL_PIXEL_TYPE_ARB;
          iattributes[niattribs++] = WGLExt.WGL_TYPE_RGBA_ARB;
        }
        iattributes[niattribs++] = 0;

        return true;
    }

    static int AttribList2DrawableTypeBits(final int[] iattribs, final int niattribs, final int[] iresults) {
        int val = 0;

        for (int i = 0; i < niattribs; i++) {
          int attr = iattribs[i];
          switch (attr) {
            case WGLExt.WGL_DRAW_TO_WINDOW_ARB:
                if(iresults[i] == GL.GL_TRUE) val |= GLGraphicsConfigurationUtil.WINDOW_BIT;
                break;
            case WGLExt.WGL_DRAW_TO_BITMAP_ARB:
                if(iresults[i] == GL.GL_TRUE) val |= GLGraphicsConfigurationUtil.BITMAP_BIT;
                break;
            case WGLExt.WGL_DRAW_TO_PBUFFER_ARB:
                if(iresults[i] == GL.GL_TRUE) val |= GLGraphicsConfigurationUtil.PBUFFER_BIT;
                break;
            }
        }
        return val;
    }

    static boolean AttribList2GLCapabilities( ArrayList<? extends GLCapabilitiesImmutable> capsBucket,
                                              final GLProfile glp, final long hdc, final int pfdID, final int[] iattribs,
                                              final int niattribs,
                                              final int[] iresults, final int winattrmask) {
        final int allDrawableTypeBits = AttribList2DrawableTypeBits(iattribs, niattribs, iresults);
        int drawableTypeBits = winattrmask & allDrawableTypeBits;

        if( 0 == drawableTypeBits ) {
            return false;
        }
        PIXELFORMATDESCRIPTOR pfd = createPixelFormatDescriptor();

        if (GDI.DescribePixelFormat(hdc, pfdID, PIXELFORMATDESCRIPTOR.size(), pfd) == 0) {
            // remove displayable bits, since pfdID is non displayable
            drawableTypeBits = drawableTypeBits & ~(GLGraphicsConfigurationUtil.WINDOW_BIT | GLGraphicsConfigurationUtil.BITMAP_BIT);
            if( 0 == drawableTypeBits ) {
                return false;
            }
            // non displayable requested (pbuffer)
        }
        WGLGLCapabilities res = new WGLGLCapabilities(pfd, pfdID, glp);
        res.setValuesByARB(iattribs, niattribs, iresults);

        return GLGraphicsConfigurationUtil.addGLCapabilitiesPermutations(capsBucket, res, drawableTypeBits );
    }

    //
    // GDI PIXELFORMAT
    //

    static int[] wglAllGDIPFIDs(long hdc) {
        int numFormats = GDI.DescribePixelFormat(hdc, 1, 0, null);
        if (numFormats == 0) {
            throw new GLException("DescribePixelFormat: No formats - HDC 0x" + Long.toHexString(hdc) +
                                  ", LastError: " + GDI.GetLastError());
        }
        int[] pfdIDs = new int[numFormats];
        for (int i = 0; i < numFormats; i++) {
            pfdIDs[i] = 1 + i;
        }
        return pfdIDs;
    }

    static int PFD2DrawableTypeBits(PIXELFORMATDESCRIPTOR pfd) {
        int val = 0;

        int dwFlags = pfd.getDwFlags();

        if( 0 != (GDI.PFD_DRAW_TO_WINDOW & dwFlags ) ) {
            val |= GLGraphicsConfigurationUtil.WINDOW_BIT;
        }
        if( 0 != (GDI.PFD_DRAW_TO_BITMAP & dwFlags ) ) {
            val |= GLGraphicsConfigurationUtil.BITMAP_BIT;
        }
        return val;
    }

    static WGLGLCapabilities PFD2GLCapabilities(GLProfile glp, long hdc, int pfdID, boolean onscreen) {
        final int winattrmask = GLGraphicsConfigurationUtil.getWinAttributeBits(onscreen, false);
        ArrayList<WGLGLCapabilities> capsBucket = new ArrayList<WGLGLCapabilities>(1);
        if( PFD2GLCapabilities(capsBucket, glp, hdc, pfdID, winattrmask) ) {
            return capsBucket.get(0);
        }
        return null;
    }

    static boolean  PFD2GLCapabilities(ArrayList<? extends GLCapabilitiesImmutable> capsBucket, final GLProfile glp, final long hdc, final int pfdID, final int winattrmask) {
        PIXELFORMATDESCRIPTOR pfd = createPixelFormatDescriptor(hdc, pfdID);
        if(null == pfd) {
            return false;
        }
        if ((pfd.getDwFlags() & GDI.PFD_SUPPORT_OPENGL) == 0) {
          return false;
        }
        final int allDrawableTypeBits = PFD2DrawableTypeBits(pfd);
        final int drawableTypeBits = winattrmask & allDrawableTypeBits;

        if( 0 == drawableTypeBits ) {
            return false;
        }

        WGLGLCapabilities res = new WGLGLCapabilities(pfd, pfdID, glp);
        res.setValuesByGDI();

        return GLGraphicsConfigurationUtil.addGLCapabilitiesPermutations(capsBucket, res, drawableTypeBits );
  }

  static PIXELFORMATDESCRIPTOR GLCapabilities2PFD(GLCapabilitiesImmutable caps, PIXELFORMATDESCRIPTOR pfd) {
    int colorDepth = (caps.getRedBits() +
                      caps.getGreenBits() +
                      caps.getBlueBits());
    if (colorDepth < 15) {
      throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
    }
    int pfdFlags = (GDI.PFD_SUPPORT_OPENGL |
                    GDI.PFD_GENERIC_ACCELERATED);
    if (caps.getDoubleBuffered()) {
      pfdFlags |= GDI.PFD_DOUBLEBUFFER;
    }
    if (caps.isOnscreen()) {
      pfdFlags |= GDI.PFD_DRAW_TO_WINDOW;
    } else {
      pfdFlags |= GDI.PFD_DRAW_TO_BITMAP;
    }
    if (caps.getStereo()) {
      pfdFlags |= GDI.PFD_STEREO;
    }
    pfd.setDwFlags(pfdFlags);
    pfd.setIPixelType((byte) GDI.PFD_TYPE_RGBA);
    pfd.setCColorBits((byte) colorDepth);
    pfd.setCRedBits  ((byte) caps.getRedBits());
    pfd.setCGreenBits((byte) caps.getGreenBits());
    pfd.setCBlueBits ((byte) caps.getBlueBits());
    pfd.setCAlphaBits((byte) caps.getAlphaBits());
    int accumDepth = (caps.getAccumRedBits() +
                      caps.getAccumGreenBits() +
                      caps.getAccumBlueBits());
    pfd.setCAccumBits     ((byte) accumDepth);
    pfd.setCAccumRedBits  ((byte) caps.getAccumRedBits());
    pfd.setCAccumGreenBits((byte) caps.getAccumGreenBits());
    pfd.setCAccumBlueBits ((byte) caps.getAccumBlueBits());
    pfd.setCAccumAlphaBits((byte) caps.getAccumAlphaBits());
    pfd.setCDepthBits((byte) caps.getDepthBits());
    pfd.setCStencilBits((byte) caps.getStencilBits());
    pfd.setILayerType((byte) GDI.PFD_MAIN_PLANE);

    // n/a with non ARB/GDI method:
    //       multisample
    //       opaque
    //       pbuffer
    return pfd;
  }

  static PIXELFORMATDESCRIPTOR createPixelFormatDescriptor(long hdc, int pfdID) {
    PIXELFORMATDESCRIPTOR pfd = PIXELFORMATDESCRIPTOR.create();
    pfd.setNSize((short) PIXELFORMATDESCRIPTOR.size());
    pfd.setNVersion((short) 1);
    if(0 != hdc && 1 <= pfdID) {
        if (GDI.DescribePixelFormat(hdc, pfdID, PIXELFORMATDESCRIPTOR.size(), pfd) == 0) {
            // Accelerated pixel formats that are non displayable
            if(DEBUG) {
                System.err.println("Info: Non displayable pixel format " + pfdID + " of device context: error code " + GDI.GetLastError());
            }
            return null;
        }
    }
    return pfd;
  }

  static PIXELFORMATDESCRIPTOR createPixelFormatDescriptor() {
    return createPixelFormatDescriptor(0, 0);
  }
  
  public String toString() {
    return "WindowsWGLGraphicsConfiguration["+getScreen()+", pfdID " + getPixelFormatID() + ", ARB-Choosen " + isChoosenByARB() +
                                            ",\n\trequested " + getRequestedCapabilities() +
                                            ",\n\tchosen    " + getChosenCapabilities() +
                                            "]";
  }
}

