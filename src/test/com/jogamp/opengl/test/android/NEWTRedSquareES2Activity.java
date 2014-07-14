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
package com.jogamp.opengl.test.android;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import jogamp.newt.driver.android.NewtBaseActivity;

import com.jogamp.newt.event.MonitorEvent;
import com.jogamp.newt.event.MonitorModeListener;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.util.Animator;

import android.os.Bundle;
import android.util.Log;

public class NEWTRedSquareES2Activity extends NewtBaseActivity {
   static String TAG = "NEWTGearsES2Activity";

   @Override
   public void onCreate(final Bundle savedInstanceState) {
       Log.d(TAG, "onCreate - 0");
       super.onCreate(savedInstanceState);

       // create GLWindow (-> incl. underlying NEWT Display, Screen & Window)
       final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2));
       Log.d(TAG, "req caps: "+caps);
       final GLWindow glWindow = GLWindow.create(caps);
       // glWindow.setSize(200, 200);
       // glWindow.setUndecorated(true);
       glWindow.setFullscreen(true);
       setContentView(getWindow(), glWindow);

       final RedSquareES2 demo = new RedSquareES2(-1);
       // demo.enableAndroidTrace(true);
       glWindow.addGLEventListener(demo);
       glWindow.getScreen().addMonitorModeListener(new MonitorModeListener() {
           @Override
           public void monitorModeChangeNotify(final MonitorEvent me) { }
           @Override
           public void monitorModeChanged(final MonitorEvent me, final boolean success) {
               System.err.println("MonitorMode Changed (success "+success+"): "+me);
           }
       });
       final Animator animator = new Animator(glWindow);
       // animator.setRunAsFastAsPossible(true);
       // glWindow.setSkipContextReleaseThread(animator.getThread());
       glWindow.setVisible(true);

       animator.setUpdateFPSFrames(60, System.err);
       animator.resetFPSCounter();
       glWindow.resetFPSCounter();

       Log.d(TAG, "onCreate - X");
   }

   @Override
   public void onResume() {
       // android.os.Debug.startMethodTracing("GearsES2.trace");
       // android.os.Debug.startAllocCounting();
       super.onResume();
   }

   @Override
   public void onPause() {
       // android.os.Debug.stopAllocCounting();
       // android.os.Debug.stopMethodTracing();
       super.onPause();
   }

}
