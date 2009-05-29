/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.egl;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
  
    // We need more than one of these on certain devices (the NVidia APX 2500 in particular)
    private List/*<NativeLibrary>*/ glesLibraries = new ArrayList();

    public EGLDrawableFactory() {
        super();

        loadGLESLibrary();
        EGL.resetProcAddressTable(this);

        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        new EGLGraphicsConfigurationFactory();

        // Check for other underlying stuff ..
        if(NativeWindowFactory.TYPE_X11.equals(NativeWindowFactory.getNativeWindowType())) {
            try {
                NWReflection.createInstance("com.sun.opengl.impl.x11.glx.X11GLXGraphicsConfigurationFactory");
            } catch (Throwable t) {}
        }
    }

    private NativeLibrary loadFirstAvailable(List/*<String>*/ libNames, ClassLoader loader) {
        for (Iterator iter = libNames.iterator(); iter.hasNext(); ) {
            NativeLibrary lib = NativeLibrary.open((String) iter.next(), loader);
            if (lib != null) {
                return lib;
            }
        }
        return null;
    }

    private void loadGLESLibrary() {
        List/*<NativeLibrary>*/ libs = new ArrayList();

        // Try several variants
        List/*<String>*/ glesLibNames = new ArrayList();
        List/*<String>*/ eglLibNames = new ArrayList();

        if (GLProfile.usesNativeGLES2()) {
            // Unix
            glesLibNames.add("libGLES20");
            glesLibNames.add("libGLESv2");
            glesLibNames.add("libGLESv2_CM");
            // Windows
            glesLibNames.add("GLES20");
            glesLibNames.add("GLESv2");
            glesLibNames.add("GLESv2_CM");
        } else if (GLProfile.usesNativeGLES1()) {
            // Unix
            glesLibNames.add("libGLES_CM");
            glesLibNames.add("libGLES_CL");
            glesLibNames.add("libGLESv1_CM");

            // Windows
            glesLibNames.add("GLES_CM");
            glesLibNames.add("GLES_CL");
            glesLibNames.add("GLESv1_CM");
        } else {
            throw new GLException("Invalid GL Profile for EGL: "+GLProfile.getProfile());
        }

        // EGL Unix
        eglLibNames.add("libEGL");
        // EGL Windows
        eglLibNames.add("EGL");

        ClassLoader loader = getClass().getClassLoader();
        NativeLibrary lib = loadFirstAvailable(glesLibNames, loader);
        if (lib == null) {
            throw new GLException("Unable to dynamically load OpenGL ES library for profile \"" + GLProfile.getProfile() + "\"");
        }
        glesLibraries.add(lib);
        lib = loadFirstAvailable(eglLibNames, loader);
        if (lib == null) {
            throw new GLException("Unable to dynamically load EGL library for profile \"" + GLProfile.getProfile() + "\"");
        }
        glesLibraries.add(lib);
        
        if (GLProfile.usesNativeGLES2()) {
            NativeLibLoader.loadES2();
        } else if (GLProfile.usesNativeGLES1()) {
            NativeLibLoader.loadES1();
        }
    }

    public GLDrawable createGLDrawable(NativeWindow target) {
        target = NativeWindowFactory.getNativeWindow(target, null);
        return new EGLDrawable(this, target);
    }

    public GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                  GLCapabilitiesChooser chooser,
                                                  int width,
                                                  int height) {
        throw new GLException("Not yet implemented");
    }

    public boolean canCreateGLPbuffer() {
        // Not supported on OpenGL ES
        return false;
    }
    public GLPbuffer createGLPbuffer(final GLCapabilities capabilities,
                                     final GLCapabilitiesChooser chooser,
                                     final int initialWidth,
                                     final int initialHeight,
                                     final GLContext shareWith) {
        throw new GLException("Pbuffer support not available on OpenGL ES");
    }

    public GLContext createExternalGLContext() {
        AbstractGraphicsScreen absScreen = DefaultGraphicsScreen.createScreenDevice(0);
        return new EGLExternalContext(absScreen);
    }

    public boolean canCreateExternalGLDrawable() {
        return false;
    }

    public GLDrawable createExternalGLDrawable() {
        throw new GLException("Not yet implemented");
    }

    public void loadGLULibrary() {
    }

    private long dynamicLookupFunctionOnLibs(String glFuncName) {
        // Look up this function name in all known libraries
        for (Iterator iter = glesLibraries.iterator(); iter.hasNext(); ) {
            NativeLibrary lib = (NativeLibrary) iter.next();
            long addr = lib.lookupFunction(glFuncName);
            if (addr != 0) {
                return addr;
            }
        }

        return 0;
    }

    private long eglGetProcAddressHandle = 0;

    public long dynamicLookupFunction(String glFuncName) {
        if(null==glFuncName) {
            return 0;
        }

        // bootstrap eglGetProcAddress
        if(0==eglGetProcAddressHandle) {
            eglGetProcAddressHandle = dynamicLookupFunctionOnLibs("eglGetProcAddress");
            if(0==eglGetProcAddressHandle) {
                throw new GLException("Couldn't find eglGetProcAddress function entry");
            }
        }

        if(glFuncName.equals("eglGetProcAddress")) {
            return eglGetProcAddressHandle;
        }

        long addr = EGL.eglGetProcAddress(eglGetProcAddressHandle, glFuncName);
        if(0==addr) {
            addr = dynamicLookupFunctionOnLibs(glFuncName);
        }
        return addr;
    }

    public boolean canCreateContextOnJava2DSurface() {
        return false;
    }

    /*

    // FIXME: this is the OpenGL ES 2 initialization order

    // Initialize everything
    public void initialize() throws GLException {
        System.out.println("EGLDrawableFactory.initEGL()");
        if (!initEGL()) {
            throw new GLException("EGL init failed");
        }
        System.out.println("EGLDrawableFactory.chooseConfig()");
        if (!chooseConfig()) {
            throw new GLException("EGL choose config failed");
        }
        System.out.println("EGLDrawableFactory.checkDisplay()");
        if (!checkDisplay()) {
            throw new GLException("EGL check display failed");
        }
        System.out.println("EGLDrawableFactory.checkConfig()");
        if (!checkConfig()) {
            throw new GLException("EGL check config failed");
        }
        System.out.println("EGLDrawableFactory.createWindow()");
        if (!createWindow()) {
            throw new GLException("KD window init failed");
        }
        System.out.println("EGLDrawableFactory.setWindowVisible()");
        setWindowVisible();
        System.out.println("EGLDrawableFactory.setWindowFullscreen()");
        setWindowFullscreen();
        System.out.println("EGLDrawableFactory.realizeWindow()");
        if (!realizeWindow()) {
            throw new GLException("EGL/GLES window realize failed");
        }
        System.out.println("EGLDrawableFactory.createSurface()");
        if (!createSurface()) {
            throw new GLException("EGL create window surface failed");
        }
        System.out.println("EGLDrawableFactory.createContext()");
        if (!createContext()) {
            throw new GLException("EGL create context failed");
        }
        System.out.println("EGLDrawableFactory.makeCurrent()");
        if (!makeCurrent()) {
            throw new GLException("EGL make current failed");
        }
        System.out.println("EGLDrawableFactory.updateWindowSize()");
        updateWindowSize();
    }

    */

    /*

    // FIXME: this is the OpenGL ES 1 initialization order

    // Initialize everything
    public void initialize() throws GLException {
        System.out.println("EGLDrawableFactory.initEGL()");
        if (!initEGL()) {
            throw new GLException("EGL init failed");
        }
        System.out.println("EGLDrawableFactory.chooseConfig()");
        if (!chooseConfig()) {
            throw new GLException("EGL choose config failed");
        }
        System.out.println("EGLDrawableFactory.checkDisplay()");
        if (!checkDisplay()) {
            throw new GLException("EGL check display failed");
        }
        System.out.println("EGLDrawableFactory.checkConfig()");
        if (!checkConfig()) {
            throw new GLException("EGL check config failed");
        }
        System.out.println("EGLDrawableFactory.createContext()");
        if (!createContext()) {
            throw new GLException("EGL create context failed");
        }
        //
        // OpenKODE Core window system initialisation.
        //
        System.out.println("EGLDrawableFactory.createWindow()");
        if (!createWindow()) {
            throw new GLException("KD window init failed");
        }
        //        System.out.println("EGLDrawableFactory.setWindowVisible()");
        //        setWindowVisible();
        System.out.println("EGLDrawableFactory.setWindowFullscreen()");
        setWindowFullscreen();
        System.out.println("EGLDrawableFactory.realizeWindow()");
        if (!realizeWindow()) {
            throw new GLException("EGL/GLES window realize failed");
        }
        System.out.println("EGLDrawableFactory.createSurface()");
        if (!createSurface()) {
            throw new GLException("EGL create window surface failed");
        }
        System.out.println("EGLDrawableFactory.makeCurrent()");
        if (!makeCurrent()) {
            throw new GLException("EGL make current failed");
        }
        System.out.println("EGLDrawableFactory.updateWindowSize()");
        updateWindowSize();
    }

    */

    /*

    // Process incoming events -- must be called every frame
    public void processEvents() {
        if (shouldExit()) {
            shutdown();
        }
    }

    public void swapBuffers() {
        swapBuffers0();
    }

    private native boolean initEGL();
    private native boolean chooseConfig();
    private native boolean checkDisplay();
    private native boolean checkConfig();
    private native boolean createWindow();
    private native void    setWindowVisible();
    private native void    setWindowFullscreen();
    private native boolean realizeWindow();
    private native boolean createSurface();
    private native boolean createContext();
    private native boolean makeCurrent();
    private native void    updateWindowSize();
    private native void    swapBuffers0();

    // Runs the native message loop one step and checks to see if we should exit
    private native boolean shouldExit();
    public native void shutdown();

    public void testGetDirectBufferAddress() {
        java.nio.FloatBuffer buf = com.sun.opengl.impl.InternalBufferUtil.newFloatBuffer(12);
        int addr = getDirectBufferAddress(buf);
        System.out.println("Direct FloatBuffer's address: 0x" + Integer.toHexString(addr));
    }
    public native int getDirectBufferAddress(java.nio.Buffer buf);

    */

    public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
        throws GLException {
        throw new GLException("Unimplemented on this platform");
    }
}
