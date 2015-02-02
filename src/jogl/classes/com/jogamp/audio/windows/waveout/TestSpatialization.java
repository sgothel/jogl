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
 */

package com.jogamp.audio.windows.waveout;

import java.io.*;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GLDrawableFactory;

public class TestSpatialization {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: TestSpatialization [file name]");
            System.exit(1);
        }

        try {
            // FIXME: this is a hack to get the native library loaded
            try {
                GLDrawableFactory.getFactory(NativeSurface.class);
            } catch (Exception e) {}
            // Initialize the audio subsystem
            Audio audio = Audio.getInstance();
            // Create a track
            Track track = audio.newTrack(new File(args[0]));
            track.setPosition(1, 0, 0);
            // Run for ten seconds
            long startTime = System.currentTimeMillis();
            long duration = 10000;
            long curTime = 0;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            System.out.println("Playing...");
            track.setLooping(true);
            track.play();
            while ((curTime = System.currentTimeMillis()) < startTime + duration) {
                // Make one revolution every two seconds
                float rads = (float) (((2 * Math.PI) * (((float) (curTime - startTime)) / 1000.0f)) / 2);
                track.setPosition((float) Math.cos(rads), 0, (float) Math.sin(rads));
                // Would like to make it go in a circle, but since
                // stereo doesn't work now, make it move along a line
                // track.setPosition(-1.0f, 0, 2.0f * (float) Math.sin(rads));
                // Sleep a little between updates
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
            System.out.println("Shutting down audio subsystem");
            audio.shutdown();
            System.out.println("Exiting.");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
