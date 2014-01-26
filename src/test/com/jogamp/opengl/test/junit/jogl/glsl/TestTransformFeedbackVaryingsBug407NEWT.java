package com.jogamp.opengl.test.junit.jogl.glsl;

import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

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

    class MyShader {
        int shaderProgram;
        int vertShader;

        MyShader(int shaderProgram, int vertShader) {
            this.shaderProgram = shaderProgram;
            this.vertShader = vertShader;
        }
    }

    private MyShader attachShader(GL3 gl, String text, int type) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        int shaderProgram = gl.glCreateProgram();

        int vertShader = gl.glCreateShader(type);

        String[] lines = new String[]{text};
        int[] lengths = new int[]{lines[0].length()};
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

    private void releaseShader(GL3 gl, MyShader myShader) {
        if(null!=myShader) {
            gl.glDetachShader(myShader.shaderProgram, myShader.vertShader);
            gl.glDeleteShader(myShader.vertShader);
            gl.glDeleteProgram(myShader.shaderProgram);
        }
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
    }

    final static String glps = GLProfile.GL3;
    
    private NEWTGLContext.WindowContext prepareTest() throws GLException, InterruptedException {
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createOnscreenWindow(
                new GLCapabilities(GLProfile.getMaxProgrammable(true)), 480, 480, debugGL);
        if(!winctx.context.getGL().isGL3()) {
            System.err.println("GL3 not available");
            cleanupTest(winctx);
            return null;
        }
        Assert.assertEquals(GL.GL_NO_ERROR, winctx.context.getGL().glGetError());        
        return winctx;
    }

    private void cleanupTest(NEWTGLContext.WindowContext winctx) {
        if(null!=winctx) {
            NEWTGLContext.destroyWindow(winctx);
        }
    }

    @Test(timeout=60000)
    public void testGlTransformFeedbackVaryings_WhenVarNameOK() throws GLException, InterruptedException {
        NEWTGLContext.WindowContext winctx = prepareTest();
        if(null == winctx) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        // given

        final GL3 gl = winctx.context.getGL().getGL3();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        MyShader myShader = attachShader(gl, VERTEX_SHADER_TEXT, GL3.GL_VERTEX_SHADER);
        String[] vars = new String[]{"Position"};

        // when

        gl.glTransformFeedbackVaryings(myShader.shaderProgram, 1, vars, GL3.GL_SEPARATE_ATTRIBS);
        gl.glLinkProgram(myShader.shaderProgram);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // then

        boolean error = false;

        if(!ShaderUtil.isProgramLinkStatusValid(gl, myShader.shaderProgram, pbaos)) {
            System.out.println("Error (unexpected link error) - testGlTransformFeedbackVaryings_WhenVarNameOK:postLink: "+baos.toString());
            error = true;
        }
        pbaos.flush(); baos.reset();
        Assert.assertEquals(GL3.GL_NO_ERROR, gl.glGetError());

        releaseShader(gl, myShader);        
        cleanupTest(winctx);
        Assert.assertFalse(error);
    }

    @Test(timeout=60000)
    public void testGlTransformFeedbackVaryings_WhenVarNameWrong() throws GLException, InterruptedException {
        NEWTGLContext.WindowContext winctx = prepareTest();
        if(null == winctx) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        // given

        final GL3 gl = winctx.context.getGL().getGL3();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        MyShader myShader = attachShader(gl, VERTEX_SHADER_TEXT, GL3.GL_VERTEX_SHADER);
        String[] vars = new String[]{"PPPosition"};

        // when

        gl.glTransformFeedbackVaryings(myShader.shaderProgram, 1, vars, GL3.GL_SEPARATE_ATTRIBS);
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
        Assert.assertEquals(GL3.GL_NO_ERROR, gl.glGetError());
        
        releaseShader(gl, myShader);
        cleanupTest(winctx);

        Assert.assertFalse(error);
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestTransformFeedbackVaryingsBug407NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
