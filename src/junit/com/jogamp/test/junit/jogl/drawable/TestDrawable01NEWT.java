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

package com.jogamp.test.junit.jogl.drawable;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

public class TestDrawable01NEWT {
    static GLProfile glp;
    static int width, height;
    GLCapabilities caps;
    Window window;
    GLDrawable drawable;
    GLContext context;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        width  = 640;
        height = 480;
    }

    @Before
    public void initTest() {
        caps = new GLCapabilities(glp);
    }

    void createWindow(boolean onscreen, boolean pbuffer, boolean undecorated) {
        caps.setOnscreen(onscreen);
        caps.setPBuffer(!onscreen && pbuffer);
        caps.setDoubleBuffered(!onscreen);
        System.out.println("**********************************************************");
        System.out.println("**********************************************************");
        System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Display display = NewtFactory.createDisplay(null); // local display
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        window = NewtFactory.createWindow(screen, caps, onscreen && undecorated);
        window.setSize(width, height);
        window.setVisible(true);
        System.out.println("**********************************************************");
        System.out.println("**********************************************************");
        System.out.println("Created: "+window);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        GLCapabilities glCaps = (GLCapabilities) window.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();

        GLDrawableFactory factory = GLDrawableFactory.getFactory(glCaps.getGLProfile());
        System.out.println(factory);
        drawable = factory.createGLDrawable(window);
        System.out.println("**********************************************************");
        System.out.println("**********************************************************");
        System.out.println("Pre: "+drawable);
        drawable.setRealized(true);
        System.out.println("**********************************************************");
        System.out.println("**********************************************************");
        System.out.println("Post: "+drawable);
        context = drawable.createContext(null);
        System.out.println(context);

        System.out.println("**********************************************************");
        System.out.println("**********************************************************");
        System.out.println("Final: "+window);
    }

    void destroyWindow() {
        context.destroy();
        drawable.setRealized(false);
        window.destroy(true); // incl screen + display
    }

    @Test
    public void testOnScreenDecorated() {
        createWindow(true, false, false);
        try {
            Thread.sleep(1000); // 1000 ms
        } catch (Exception e) {}
        destroyWindow();
    }

    @Test
    public void testOnScreenUndecorated() {
        createWindow(true, false, true);
        try {
            Thread.sleep(1000); // 1000 ms
        } catch (Exception e) {}
        destroyWindow();
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestDrawable01NEWT.class.getName());
    }

}
