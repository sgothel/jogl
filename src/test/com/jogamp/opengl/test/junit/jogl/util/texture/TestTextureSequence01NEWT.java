package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.io.IOException;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureSequenceCubeES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.ImageSequence;
import com.jogamp.opengl.util.texture.TextureIO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextureSequence01NEWT extends UITestCase {
    static boolean showFPS = false;
    static int width = 510;
    static int height = 300;
    static boolean useBuildInTexLookup = false;
    static long duration = 500; // ms
    static GLProfile glp;
    static GLCapabilities caps;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getGL2ES2();
        Assert.assertNotNull(glp);
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
    }

    void testImpl() throws InterruptedException {
        final GLWindow window = GLWindow.create(caps);
        window.setTitle("TestTextureSequence01NEWT");
        // Size OpenGL to Video Surface
        window.setSize(width, height);
        final ImageSequence texSource = new ImageSequence(0, useBuildInTexLookup);
        window.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {
                try {
                    texSource.addFrame(drawable.getGL(), TestTextureSequence01NEWT.class, "test-ntscP_3-01-160x90.png", TextureIO.PNG);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void dispose(final GLAutoDrawable drawable) { }
            @Override
            public void display(final GLAutoDrawable drawable) { }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });
        window.addGLEventListener(new TextureSequenceCubeES2(texSource, false, -2.3f, 0f, 0f));
        final Animator animator = new Animator(window);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        final QuitAdapter quitAdapter = new QuitAdapter();
        window.addKeyListener(quitAdapter);
        window.addWindowListener(quitAdapter);
        animator.start();
        window.setVisible(true);

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        window.destroy();
    }

    @Test
    public void test1() throws InterruptedException {
        testImpl();
    }

    public static void main(final String[] args) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                i++;
                width = MiscUtils.atoi(args[i], width);
            } else if(args[i].equals("-height")) {
                i++;
                height = MiscUtils.atoi(args[i], height);
            } else if(args[i].equals("-shaderBuildIn")) {
                useBuildInTexLookup = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestTextureSequence01NEWT.class.getName());
    }

}
