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

import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;

public class TestOffscreen01NEWT {
    int width, height;
    GLProfile glp;
    GLCapabilities caps;

    @Before
    public void init() {
        glp = GLProfile.getDefault();
        width  = 640;
        height = 480;
        caps = new GLCapabilities(glp);
    }

    @Test
    public void test01OffscreenWindow() {
        GLWindow windowOffscreen = WindowUtilNEWT.createGLWindow(caps, width, height, false, true, false);
        GLEventListener demo = new RedSquare();
        GLWindow windowOnScreen = null;
        WindowListener wl=null;
        MouseListener ml=null;
        SurfaceUpdatedListener ul=null;

        WindowUtilNEWT.run(windowOffscreen, null, windowOnScreen, wl, ml, ul, 2, false /*snapshot*/, false /*debug*/);
        try {
            Thread.sleep(1000); // 1000 ms
        } catch (Exception e) {}
        WindowUtilNEWT.shutdown(windowOffscreen, windowOnScreen);
    }

    @Test
    public void test02OffscreenSnapshotWithDemo() {
        GLWindow windowOffscreen = WindowUtilNEWT.createGLWindow(caps, width, height, false, true, false);
        GLEventListener demo = new RedSquare();
        GLWindow windowOnScreen = null;
        WindowListener wl=null;
        MouseListener ml=null;
        SurfaceUpdatedListener ul=null;

        WindowUtilNEWT.run(windowOffscreen, demo, windowOnScreen, wl, ml, ul, 2, true /*snapshot*/, false /*debug*/);
        try {
            Thread.sleep(1000); // 1000 ms
        } catch (Exception e) {}
        WindowUtilNEWT.shutdown(windowOffscreen, windowOnScreen);
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestOffscreen01NEWT.class.getName());
    }

}
