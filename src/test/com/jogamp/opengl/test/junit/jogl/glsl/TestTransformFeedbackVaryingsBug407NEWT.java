package com.jogamp.opengl.test.junit.jogl.glsl;

import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.media.opengl.GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.glsl.ShaderUtil;

import java.io.IOException;
import org.junit.AfterClass;

/**
 * Bug 'Function glTransformFeedbackVaryings incorrectly passes argument'
 * http://jogamp.org/bugzilla/show_bug.cgi?id=407
 */
public class TestTransformFeedbackVaryingsBug407NEWT extends UITestCase {

    private String VERTEX_SHADER_TEXT;

    class MyShader {
        int shaderProgram;
        int vertShader;

        MyShader(int shaderProgram, int vertShader) {
            this.shaderProgram = shaderProgram;
            this.vertShader = vertShader;
        }
    }

    private MyShader getShader(GL3 gl, String text, int type) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        int shaderProgram = gl.glCreateProgram();

        int vertShader = gl.glCreateShader(type);

        String[] lines = new String[]{text};
        int[] lengths = new int[]{lines[0].length()};
        gl.glShaderSource(vertShader, lines.length, lines, lengths, 0);
        gl.glCompileShader(vertShader);

        if(!ShaderUtil.isShaderStatusValid(gl, vertShader, gl.GL_COMPILE_STATUS, pbaos)) {
            System.out.println("getShader:postCompile: "+baos.toString());
            Assert.assertTrue(false);
        }
        pbaos.flush(); baos.reset();

        gl.glAttachShader(shaderProgram, vertShader);

        return new MyShader(shaderProgram, vertShader);
    }

    private void releaseShader(GL3 gl, MyShader myShader) {
        if(null!=myShader) {
            gl.glDetachShader(myShader.shaderProgram, myShader.vertShader);
            gl.glDeleteShader(myShader.vertShader);
            gl.glDeleteProgram(myShader.shaderProgram);
        }
    }


    private GLWindow prepareTest() {
        if(!GLProfile.isAvailable(GLProfile.GL3)) {
            System.err.println("GL3 not available");
            System.err.println(GLProfile.glAvailabilityToString());
            return null;
        }
        VERTEX_SHADER_TEXT =
                  "#version 150                           \n"
                + "                                       \n"
                + "out vec4 Position;                     \n"
                + "                                       \n"
                + "void main() {                          \n"
                + "  Position = vec4(1.0, 1.0, 1.0, 1.0); \n"
                + "}                                      \n";

        GLCapabilities caps;        

        GLProfile glp = null;
        try {
            glp = GLProfile.get(GLProfile.GL3);
        } catch (Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
        caps = new GLCapabilities(glp);

        caps.setOnscreen(true);
        caps.setDoubleBuffered(true);

        GLWindow window = GLWindow.create(caps);
        Assert.assertNotNull(window);
        window.setUndecorated(true);
        window.setSize(800, 600);
        window.setVisible(true);
        Assert.assertTrue(window.isNativeValid());

        window.getContext().setSynchronized(true);

        // trigger native creation of drawable/context
        window.display();
        Assert.assertTrue(window.isRealized());
        Assert.assertTrue(window.getContext().isCreated());

        return window;
    }

    private void cleanupTest(GLWindow window) {
        if(null!=window) {
            window.destroy();
        }
    }

    @Test(timeout=60000)
    public void testGlTransformFeedbackVaryings_WhenVarNameOK() {
        if(!GLProfile.isAvailable(GLProfile.GL3)) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        GLWindow window = prepareTest();
        GLContext context = window.getContext();
        context.makeCurrent();

        // given

        GL3 gl = context.getGL().getGL3();
        MyShader myShader = getShader(gl, VERTEX_SHADER_TEXT, GL3.GL_VERTEX_SHADER);
        String[] vars = new String[]{"Position"};

        // when

        gl.glTransformFeedbackVaryings(myShader.shaderProgram, 1, vars, GL3.GL_SEPARATE_ATTRIBS);
        gl.glLinkProgram(myShader.shaderProgram);

        // then

        boolean error = false;

        if(!ShaderUtil.isProgramValid(gl, myShader.shaderProgram, pbaos)) {
            System.out.println("Error (unexpected link error) - testGlTransformFeedbackVaryings_WhenVarNameOK:postLink: "+baos.toString());
            error = true;
        }
        pbaos.flush(); baos.reset();

        Assert.assertEquals(GL3.GL_NO_ERROR, gl.glGetError());

        releaseShader(gl, myShader);
        context.release();
        cleanupTest(window);

        Assert.assertFalse(error);
    }

    @Test(timeout=60000)
    public void testGlTransformFeedbackVaryings_WhenVarNameWrong() {
        if(!GLProfile.isAvailable(GLProfile.GL3)) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        GLWindow window = prepareTest();
        GLContext context = window.getContext();
        context.makeCurrent();

        // given

        GL3 gl = context.getGL().getGL3();
        MyShader myShader = getShader(gl, VERTEX_SHADER_TEXT, GL3.GL_VERTEX_SHADER);
        String[] vars = new String[]{"PPPosition"};

        // when

        gl.glTransformFeedbackVaryings(myShader.shaderProgram, 1, vars, GL3.GL_SEPARATE_ATTRIBS);
        gl.glLinkProgram(myShader.shaderProgram);

        // then

        boolean error = false;

        if(!ShaderUtil.isProgramValid(gl, myShader.shaderProgram, pbaos)) {
            System.out.println("GOOD (expected link error) - testGlTransformFeedbackVaryings_WhenVarNameWrong:postLink: "+baos.toString());
            // should be invalid, due to wrong var name
        } else {
            // oops 
            System.out.println("Error (unexpected link success) - testGlTransformFeedbackVaryings_WhenVarNameWrong link worked, but it should not");
            error = true;
        }
        pbaos.flush(); baos.reset();

        Assert.assertEquals(GL3.GL_NO_ERROR, gl.glGetError());
        // You cannot assume this error message - Assert.assertTrue(baos.toString().contains("(named PPPosition)"));

        releaseShader(gl, myShader);
        context.release();
        cleanupTest(window);

        Assert.assertFalse(error);
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestTransformFeedbackVaryingsBug407NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
