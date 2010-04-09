/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.test.junit.jogl.offscreen;

import javax.media.opengl.*;

import com.jogamp.opengl.util.texture.TextureIO;

import java.io.File;
import java.io.IOException;

import javax.media.nativewindow.*;

public class Surface2File implements SurfaceUpdatedListener {

    ReadBufferUtil readBufferUtil = new ReadBufferUtil();
    int shotNum=0;

    public void dispose() {
        readBufferUtil.dispose();
    }

    public void surfaceUpdated(Object updater, NativeWindow window, long when) {
        if(updater instanceof GLDrawable) {
            GLDrawable drawable = (GLDrawable) updater;
            GLContext ctx = GLContext.getCurrent();
            if(null!=ctx && ctx.getGLDrawable()==drawable) {
                readBufferUtil.fetchOffscreenTexture(drawable, ctx.getGL());
                surface2File("shot");
            }
        }
    }

    public void surface2File(String basename) {
      if(!readBufferUtil.isValid()) return;

      try {
        File file = File.createTempFile(basename+shotNum+"-", ".ppm");
        TextureIO.write(readBufferUtil.getTextureData(), file);
        if(0==shotNum) {
            System.out.println("Wrote: "+file.getAbsolutePath()+", ...");
        }
        shotNum++;
      } catch (IOException ioe) { ioe.printStackTrace(); }
      readBufferUtil.rewindPixelBuffer();
    }
}

