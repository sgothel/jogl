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
package com.jogamp.opengl.demos.androidfat;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.android.LauncherUtil.OrderedProperties;
import com.jogamp.opengl.demos.graph.ui.UISceneDemo20;

import jogamp.newt.driver.android.NewtBaseActivity;

import com.jogamp.newt.event.MonitorEvent;
import com.jogamp.newt.event.MonitorModeListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;

import android.os.Bundle;
import android.util.Log;

public class NEWTGraphUISceneDemo20Activity1p extends NewtBaseActivity {
   static String TAG = "NEWTGraphUISceneDemo20Activity1p";

   public void initProperties() {
       final OrderedProperties props = new OrderedProperties();
       // props.setProperty("jogamp.debug.JNILibLoader", "true");
       // props.setProperty("jogamp.debug.NativeLibrary", "true");
       // props.setProperty("jogamp.debug.NativeLibrary.Lookup", "true");
       // props.setProperty("jogamp.debug.IOUtil", "true");
       // props.setProperty("nativewindow.debug", "all");
       // props.setProperty("nativewindow.debug.GraphicsConfiguration", "true");
       // props.setProperty("jogl.debug", "all");
       // props.setProperty("jogl.debug.GLProfile", "true");
       // props.setProperty("jogl.debug.GLDrawable", "true");
       // props.setProperty("jogl.debug.GLContext", "true");
       props.setProperty("jogl.debug.GLSLCode", "true");
       // props.setProperty("jogl.debug.CapabilitiesChooser", "true");
       // props.setProperty("jogl.debug.GLSLState", "true");
       // props.setProperty("jogl.debug.DebugGL", "true");
       // props.setProperty("jogl.debug.TraceGL", "true");
       // props.setProperty("newt.debug", "all");
       // props.setProperty("newt.debug.Window", "true");
       // props.setProperty("newt.debug.Window.MouseEvent", "true");
       // props.setProperty("newt.debug.Window.KeyEvent", "true");
       props.setSystemProperties();
   }

   @Override
   public void onCreate(final Bundle savedInstanceState) {
       Log.d(TAG, "onCreate - 0");

       initProperties();

       super.onCreate(savedInstanceState);

       // create GLWindow (-> incl. underlying NEWT Display, Screen & Window)
       final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2));
       caps.setAlphaBits(4);
       caps.setNumSamples(4);
       caps.setSampleBuffers(true);
       Log.d(TAG, "req caps: "+caps);
       final GLWindow glWindow = GLWindow.create(caps);
       glWindow.setFullscreen(true);
       setContentView(getWindow(), glWindow);

       glWindow.addGLEventListener(new UISceneDemo20(0));
       glWindow.getScreen().addMonitorModeListener(new MonitorModeListener() {
           @Override
           public void monitorModeChangeNotify(final MonitorEvent me) { }
           @Override
           public void monitorModeChanged(final MonitorEvent me, final boolean success) {
               System.err.println("MonitorMode Changed (success "+success+"): "+me);
           }
       });
       glWindow.setVisible(true);
       final Animator animator = new Animator(glWindow);

       animator.setUpdateFPSFrames(60, System.err);
       animator.resetFPSCounter();
       glWindow.resetFPSCounter();

       Log.d(TAG, "onCreate - X");
   }
}
