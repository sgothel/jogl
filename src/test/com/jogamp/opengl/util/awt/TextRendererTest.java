/*
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.awt;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test for {@link RasterTextRenderer}.
 */
public class TextRendererTest {

    // Font to render with
    private static final Font FONT = new Font("Monospace", Font.PLAIN, 110);

    // Time to wait before closing window
    private static final int LONG_WAIT_TIME = 4000;
    private static final int SHORT_WAIT_TIME = 1000;

    // Random collection of words to render
    private WordBank wordBank;

    // Instance to render with
    private TextRenderer textRenderer;

    // Utility for making canvases
    private final GLCanvasFactory canvasFactory = new GLCanvasFactory();

    /**
     * Constructs a raster text renderer test.
     */
    public TextRendererTest() {
        wordBank = new WordBank();
        textRenderer = null;
    }

    /**
     * Ensures the text renderer can draw text properly with an OpenGL 2 context.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Clears the screen
     *   <li>Draws a random word in red at a random position
     *   <li>Draws a random word in green at a random position
     *   <li>Repeats every second
     * </ul>
     */
    @Test
    public void testWithGL2() {

        final JFrame frame = new JFrame("testWithGL2");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL2");
        final FPSAnimator animator = new FPSAnimator(canvas, 1);

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL2EventAdapter() {

            @Override
            public void doInit(final GL2 gl) {
                textRenderer = new TextRenderer(FONT);
            }

            @Override
            public void doDisplay(final GL2 gl) {

                // Clear
                gl.glClearColor(0.85f, 0.85f, 0.85f, 1);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);

                // Draw
                final int width = canvas.getWidth();
                final int height = canvas.getHeight();
                textRenderer.beginRendering(width, height);
                textRenderer.setColor(Color.RED);
                textRenderer.draw(wordBank.next(), getX(width), getY(height));
                textRenderer.setColor(Color.GREEN);
                textRenderer.draw(wordBank.next(), getX(width), getY(height));
                textRenderer.endRendering();
            }

            @Override
            public void doDispose(final GL2 gl) {
                textRenderer.dispose();
            }
        });
        animator.start();
        TestRunner.run(frame, LONG_WAIT_TIME);
        animator.stop();
    }

    /**
     * Ensures the text renderer can draw text properly with an OpenGL 3 context.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Clears the screen
     *   <li>Draws a random word in red at a random position
     *   <li>Draws a random word in green at a random position
     *   <li>Repeats every second
     * </ul>
     */
    @Test
    public void testWithGL3() {

        final JFrame frame = new JFrame("testWithGL3");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL3");
        final FPSAnimator animator = new FPSAnimator(canvas, 1);

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL3EventAdapter() {

            @Override
            public void doInit(final GL3 gl) {
                textRenderer = new TextRenderer(FONT);
            }

            @Override
            public void doDisplay(final GL3 gl) {

                // Clear
                gl.glClearColor(0.85f, 0.85f, 0.85f, 1);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);

                // Draw
                final int width = canvas.getWidth();
                final int height = canvas.getHeight();
                textRenderer.beginRendering(width, height);
                textRenderer.setColor(Color.RED);
                textRenderer.draw(wordBank.next(), getX(width), getY(height));
                textRenderer.setColor(Color.GREEN);
                textRenderer.draw(wordBank.next(), getX(width), getY(height));
                textRenderer.endRendering();
            }

            @Override
            public void doDispose(final GL3 gl) {
                textRenderer.dispose();
            }
        });
        animator.start();
        TestRunner.run(frame, LONG_WAIT_TIME);
        animator.stop();
    }

    /**
     * Ensures the user can request whether vertex arrays are used before <i>beginRendering</i> is called.
     */
    @Test
    public void testSetUseVertexArraysBeforeBeginRendering() {

        final JFrame frame = new JFrame("testSetUseVertexArraysBeforeBeginRendering");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL2");

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL2EventAdapter() {

            @Override
            public void doInit(final GL2 gl) {
                textRenderer = new TextRenderer(FONT);
                Assert.assertTrue(textRenderer.getUseVertexArrays());
                textRenderer.setUseVertexArrays(false);
                Assert.assertFalse(textRenderer.getUseVertexArrays());
            }

            @Override
            public void doDisplay(final GL2 gl) {
                final int width = canvas.getWidth();
                final int height = canvas.getHeight();
                textRenderer.beginRendering(width, height);
                textRenderer.endRendering();
                Assert.assertFalse(textRenderer.getUseVertexArrays());
            }

            @Override
            public void doDispose(final GL2 gl) {
                textRenderer.dispose();
            }
        });
        TestRunner.run(frame, SHORT_WAIT_TIME);
    }

    //-----------------------------------------------------------------
    // Helpers
    //

    /**
     * Returns random X coordinate in left-hand side of window.
     */
    private static int getX(final int width) {
        return (int) (Math.random() * width / 2);
    }

    /**
     * Returns random Y coordinate in window.
     */
    private static int getY(final int height) {
        return (int) (Math.random() * height);
    }
}


/**
 * Random collection of words.
 */
class WordBank {

    // Available words
    private List<String> words;

    // Random number generator
    private Random random;

    // Number of words in bank
    private int size;

    /**
     * Constructs a word bank.
     */
    public WordBank() {
        this.words = createWords();
        this.random = new Random(37);
        this.size = words.size();
    }

    /**
     * Returns next random word from the word bank.
     */
    public String next() {
        return words.get((int) (random.nextDouble() * size));
    }

    /**
     * Makes a collection of words.
     */
    private static List<String> createWords() {
        final List<String> words = new ArrayList<String>();
        words.add("Lüké");
        words.add("Anàkin");
        words.add("Kit");
        words.add("Plô");
        words.add("Qüi-Gônn");
        words.add("Obi-Wàn");
        words.add("Lüminàrà");
        words.add("Mäcé");
        words.add("Yôdà");
        words.add("Ahsôkà");
        words.add("Ki Adi");
        words.add("Bàrriss");
        words.add("Ackbàr");
        words.add("Chéwbàccà");
        words.add("Jàbbà");
        words.add("C-3PO");
        words.add("Bôbà");
        words.add("R2-D2");
        words.add("Hàns");
        words.add("Léià");
        words.add("Shààk Ti");
        return words;
    }
}
