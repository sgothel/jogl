/**
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
package jogamp.newt.driver.android;

import javax.media.opengl.GLProfile;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.content.Context;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback2;

public class NEWTSurfaceView extends SurfaceView implements Callback2 {

    public NEWTSurfaceView(Context context) {
        super(context);
        
        System.setProperty("jogl.debug", "all");
        System.setProperty("jogamp.debug.JNILibLoader", "true");
        System.setProperty("jogamp.debug.NativeLibrary", "true");
        System.setProperty("jogamp.debug.NativeLibrary.Lookup", "true");
        getHolder().addCallback(this);
    }
    
    boolean created = false;
    
    public final boolean isCreated() {
        return created;        
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        Surface surface = getHolder().getSurface();

        /**
            EGL10 mEgl = (EGL10) EGLContext.getEGL();

            EGLDisplay mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }
            Log.d(MD.TAG, "EGL XXXXXX " + mEgl + ", " + mEglDisplay);
        */
        Log.d(MD.TAG, "YYYYYYYYYY ");
        Log.d(MD.TAG, "surfaceCreated - 0 - isValid: "+surface.isValid());
        GLProfile.initSingleton(true);
        Log.d(MD.TAG, "surfaceCreated - 1");
        Log.d(MD.TAG, MD.getInfo());
        Log.d(MD.TAG, "surfaceCreated - X");
        created = true;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.d(MD.TAG, "surfaceChanged: f "+Integer.toString(format)+", "+width+"x"+height);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(MD.TAG, "surfaceDestroyed");
        created = false;
    }

    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        Log.d(MD.TAG, "surfaceRedrawNeeded");
    }
}
