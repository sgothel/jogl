package com.jogamp.opengl.test.junit.jogl.glsl;

import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.util.glsl.ShaderUtil;

import java.io.IOException;

/**
 * Bug 'Function glTransformFeedbackVaryings incorrectly passes argument'
 * http://jogamp.org/bugzilla/show_bug.cgi?id=407
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTransformFeedbackVaryingsBug407NEWT extends UITestCase {

    private static final boolean debugGL = true;

    private static final String VERTEX_SHADER_TEXT =
                  "#version 150                           \n"
                + "                                       \n"
                + "out vec4 Position;                     \n"
                + "                                       \n"
                + "void main() {                          \n"
                + "  Position = vec4(1.0, 1.0, 1.0, 1.0); \n"
                + "}                                      \n";

    static class MyShader {
        int shaderProgram;
        int vertShader;

        MyShader(final int shaderProgram, final int vertShader) {
            this.shaderProgram = shaderProgram;
            this.vertShader = vertShader;
        }
    }

    private MyShader attachShader(final GL3 gl, final String text, final int type) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream pbaos = new PrintStream(baos);

        final int shaderProgram = gl.glCreateProgram();

        final int vertShader = gl.glCreateShader(type);

        final String[] lines = new String[]{text};
        final int[] lengths = new int[]{lines[0].length()};
        gl.glShaderSource(vertShader, lines.length, lines, lengths, 0);
        gl.glCompileShader(vertShader);

        if(!ShaderUtil.isShaderStatusValid(gl, vertShader, GL2ES2.GL_COMPILE_STATUS, pbaos)) {
            System.out.println("getShader:postCompile: "+baos.toString());
            Assert.assertTrue(false);
        }
        pbaos.flush(); baos.reset();

        gl.glAttachShader(shaderProgram, vertShader);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        return new MyShader(shaderProgram, vertShader);
    }

    private void releaseShader(final GL3 gl, final MyShader myShader) {
        if(null!=myShader) {
            gl.glDetachShader(myShader.shaderProgram, myShader.vertShader);
            gl.glDeleteShader(myShader.vertShader);
            gl.glDeleteProgram(myShader.shaderProgram);
        }
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
    }

    final static String glps = GLProfile.GL3;

    private NEWTGLContext.WindowContext prepareTest() throws GLException, InterruptedException {
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(
                new GLCapabilities(GLProfile.getMaxProgrammable(true)), 480, 480, debugGL);
        if(!winctx.context.getGL().isGL3()) {
            System.err.println("GL3 not available");
            cleanupTest(winctx);
            return null;
        }
        Assert.assertEquals(GL.GL_NO_ERROR, winctx.context.getGL().glGetError());
        return winctx;
    }

    private void cleanupTest(final NEWTGLContext.WindowContext winctx) {
        if(null!=winctx) {
            NEWTGLContext.destroyWindow(winctx);
        }
    }

    @Test(timeout=60000)
    public void testGlTransformFeedbackVaryings_WhenVarNameOK() throws GLException, InterruptedException {
        final NEWTGLContext.WindowContext winctx = prepareTest();
        if(null == winctx) {
            return;
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream pbaos = new PrintStream(baos);

        // given

        final GL3 gl = winctx.context.getGL().getGL3();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        final MyShader myShader = attachShader(gl, VERTEX_SHADER_TEXT, GL2ES2.GL_VERTEX_SHADER);
        final String[] vars = new String[]{"Position"};

        // when

        gl.glTransformFeedbackVaryings(myShader.shaderProgram, 1, vars, GL2ES3.GL_SEPARATE_ATTRIBS);
        gl.glLinkProgram(myShader.shaderProgram);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // then

        boolean error = false;

        if(!ShaderUtil.isProgramLinkStatusValid(gl, myShader.shaderProgram, pbaos)) {
            System.out.println("Error (unexpected link error) - testGlTransformFeedbackVaryings_WhenVarNameOK:postLink: "+baos.toString());
            error = true;
        }
        pbaos.flush(); baos.reset();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        releaseShader(gl, myShader);
        cleanupTest(winctx);
        Assert.assertFalse(error);
    }

    @Test(timeout=60000)
    public void testGlTransformFeedbackVaryings_WhenVarNameWrong() throws GLException, InterruptedException {
        final NEWTGLContext.WindowContext winctx = prepareTest();
        if(null == winctx) {
            return;
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream pbaos = new PrintStream(baos);

        // given

        final GL3 gl = winctx.context.getGL().getGL3();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        final MyShader myShader = attachShader(gl, VERTEX_SHADER_TEXT, GL2ES2.GL_VERTEX_SHADER);
        final String[] vars = new String[]{"PPPosition"};

        // when

        gl.glTransformFeedbackVaryings(myShader.shaderProgram, 1, vars, GL2ES3.GL_SEPARATE_ATTRIBS);
        gl.glLinkProgram(myShader.shaderProgram);

        // then

        boolean error = false;

        if(!ShaderUtil.isProgramLinkStatusValid(gl, myShader.shaderProgram, pbaos)) {
            System.out.println("GOOD (expected link error) - testGlTransformFeedbackVaryings_WhenVarNameWrong:postLink: "+baos.toString());
            // should be invalid, due to wrong var name
        } else {
            // oops
            System.out.println("Error (unexpected link success) - testGlTransformFeedbackVaryings_WhenVarNameWrong link worked, but it should not");
            error = true;
        }
        pbaos.flush(); baos.reset();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        releaseShader(gl, myShader);
        cleanupTest(winctx);

        Assert.assertFalse(error);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestTransformFeedbackVaryingsBug407NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
