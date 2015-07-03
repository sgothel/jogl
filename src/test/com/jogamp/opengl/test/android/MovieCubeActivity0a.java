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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Arrays;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import jogamp.newt.driver.android.NewtBaseActivity;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieCube;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

import android.os.Bundle;
import android.util.Log;

public class MovieCubeActivity0a extends NewtBaseActivity {
   static String TAG = "MovieCubeActivity0a";

   MouseAdapter showKeyboardMouseListener = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
           if( e.getPointerCount() == 4 && e.getPressure(0, true) > 0.7f ) {
               ((com.jogamp.newt.Window) e.getSource()).setKeyboardVisible(true);
           }
        }
   };

   @Override
   public void onCreate(final Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       final String[] streamLocs = new String[] {
               System.getProperty("jnlp.media0_url0"),
               System.getProperty("jnlp.media0_url1"),
               System.getProperty("jnlp.media0_url2") };
       final Uri streamLoc = getUri(streamLocs, 0, false);
       if(null == streamLoc) { throw new RuntimeException("no media reachable: "+Arrays.asList(streamLocs)); }

       // also initializes JOGL
       final GLCapabilities capsMain = new GLCapabilities(GLProfile.getGL2ES2());
       capsMain.setBackgroundOpaque(false);

       // screen for layout params ..
       final com.jogamp.newt.Display dpy = NewtFactory.createDisplay(null);
       final com.jogamp.newt.Screen scrn = NewtFactory.createScreen(dpy, 0);
       scrn.addReference();

       try {
           final Animator anim = new Animator();

           // Main
           final GLWindow glWindowMain = GLWindow.create(scrn, capsMain);
           glWindowMain.setFullscreen(true);
           setContentView(getWindow(), glWindowMain);
           anim.add(glWindowMain);
           glWindowMain.setVisible(true);
           glWindowMain.addMouseListener(showKeyboardMouseListener);

           final MovieCube demoMain = new MovieCube(MovieCube.zoom_def, 0f, 0f, false);
           final GLMediaPlayer mPlayer = demoMain.getGLMediaPlayer();
           mPlayer.addEventListener(new GLMediaEventListener() {
                @Override
                public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) {
                }

                @Override
                public void attributesChanged(final GLMediaPlayer mp, final int event_mask, final long when) {
                    System.err.println("MovieCubeActivity0 AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                    System.err.println("MovieCubeActivity0 State: "+mp);
                    if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                        glWindowMain.addGLEventListener(demoMain);
                        anim.setUpdateFPSFrames(60*5, null);
                        anim.resetFPSCounter();
                    }
                    if( 0 != ( GLMediaEventListener.EVENT_CHANGE_PLAY & event_mask ) ) {
                        anim.resetFPSCounter();
                    }
                    if( 0 != ( ( GLMediaEventListener.EVENT_CHANGE_ERR | GLMediaEventListener.EVENT_CHANGE_EOS ) & event_mask ) ) {
                        final StreamException se = mPlayer.getStreamException();
                        if( null != se ) {
                            se.printStackTrace();
                        }
                        getActivity().finish();
                    }
                }
            });
           demoMain.initStream(streamLoc, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, 0);
       } catch (final IOException e) {
           e.printStackTrace();
       }

       scrn.removeReference();

       Log.d(TAG, "onCreate - X");
   }

   static Uri getUri(final String path[], final int off, final boolean checkAvail) {
       Uri uri = null;
       for(int i=off; null==uri && i<path.length; i++) {
           if(null != path[i] && path[i].length()>0) {
               if( checkAvail ) {
                   final URLConnection uc = IOUtil.getResource(path[i], null);
                   if( null != uc ) {
                       try {
                           uri = Uri.valueOf(uc.getURL());
                       } catch (final URISyntaxException e) {
                           uri = null;
                       }
                       if( uc instanceof HttpURLConnection ) {
                           ((HttpURLConnection)uc).disconnect();
                       }
                   }
               } else {
                   try {
                       uri = Uri.cast(path[i]);
                   } catch (final URISyntaxException e) {
                       uri = null;
                   }
               }
               Log.d(TAG, "Stream: <"+path[i]+">: "+(null!=uri));
           }
       }
       return uri;
   }
}
