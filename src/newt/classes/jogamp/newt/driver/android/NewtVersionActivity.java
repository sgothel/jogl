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

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

public class NewtVersionActivity extends NewtBaseActivity {
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       setFullscreenFeature(getWindow(), true);

       final android.view.ViewGroup viewGroup = new android.widget.FrameLayout(getActivity().getApplicationContext());
       getWindow().setContentView(viewGroup);

       final TextView tv = new TextView(getActivity());
       final ScrollView scroller = new ScrollView(getActivity());
       scroller.addView(tv);
       viewGroup.addView(scroller, new android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.TOP|Gravity.LEFT));

       final String info1 = VersionUtil.getPlatformInfo()+Platform.NEWLINE+GlueGenVersion.getInstance()+Platform.NEWLINE+JoglVersion.getInstance()+Platform.NEWLINE;
       Log.d(MD.TAG, info1);
       tv.setText(info1);

       final GLProfile glp;
       if( GLProfile.isAvailable(GLProfile.GL2ES2) ) {
           glp = GLProfile.get(GLProfile.GL2ES2);
       } else if( GLProfile.isAvailable(GLProfile.GL2ES1) ) {
           glp = GLProfile.get(GLProfile.GL2ES1);
       } else {
           glp = null;
           tv.append("No GLProfile GL2ES2 nor GL2ES1 available!");
       }
       if( null != glp ) {
           // create GLWindow (-> incl. underlying NEWT Display, Screen & Window)
           GLCapabilities caps = new GLCapabilities(glp);
           GLWindow glWindow = GLWindow.create(caps);
           glWindow.setUndecorated(true);
           glWindow.setSize(32, 32);
           glWindow.setPosition(0, 0);
           final android.view.View androidGLView = ((WindowDriver)glWindow.getDelegatedWindow()).getAndroidView();
           viewGroup.addView(androidGLView, new android.widget.FrameLayout.LayoutParams(glWindow.getWidth(), glWindow.getHeight(), Gravity.BOTTOM|Gravity.RIGHT));
           registerNEWTWindow(glWindow);

           glWindow.addGLEventListener(new GLEventListener() {
                public void init(GLAutoDrawable drawable) {
                    GL gl = drawable.getGL();
                    final StringBuilder sb = new StringBuilder();
                    sb.append(JoglVersion.getGLInfo(gl, null, true)).append(Platform.NEWLINE);
                    sb.append("Requested: ").append(Platform.NEWLINE);
                    sb.append(drawable.getNativeSurface().getGraphicsConfiguration().getRequestedCapabilities()).append(Platform.NEWLINE).append(Platform.NEWLINE);
                    sb.append("Chosen: ").append(Platform.NEWLINE);
                    sb.append(drawable.getChosenGLCapabilities()).append(Platform.NEWLINE).append(Platform.NEWLINE);
                    final String info2 = sb.toString();
                    // Log.d(MD.TAG, info2); // too big!
                    System.err.println(info2);
                    viewGroup.post(new Runnable() {
                        public void run() {
                            tv.append(info2);
                            viewGroup.removeView(androidGLView);
                        } } );
                }

                public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                }

                public void display(GLAutoDrawable drawable) {
                }

                public void dispose(GLAutoDrawable drawable) {
                }
            });
           glWindow.setVisible(true);
       }
       Log.d(MD.TAG, "onCreate - X");
   }
}
