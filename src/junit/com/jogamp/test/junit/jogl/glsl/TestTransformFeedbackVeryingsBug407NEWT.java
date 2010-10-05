package com.jogamp.test.junit.jogl.glsl;

import com.jogamp.test.junit.util.UITestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.media.opengl.GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.opengl.util.glsl.ShaderUtil;

import java.io.IOException;

/**
 * Bug 'Function glTransformFeedbackVaryings incorrectly passes argument'
 * http://jogamp.org/bugzilla/show_bug.cgi?id=407
 */
public class TestTransformFeedbackVeryingsBug407NEWT extends UITestCase {

    static {
        //NativeUtil.preloadNativeLibs(); // internal method
        GLProfile.initSingleton();
    }
    private GLContext context;
    private String VERTEX_SHADER_TEXT;

    @Before
    public void setUp() {
        if(!GLProfile.isGL3Available()) {
            System.err.println("GL3 not available");
            System.err.println(GLProfile.glAvailabilityToString());
            return;
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
        Window window;
        GLDrawable drawable;

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

        Display display = NewtFactory.createDisplay(null); // local display
        Screen screen = NewtFactory.createScreen(display, 0); // screen 0

        window = NewtFactory.createWindow(screen, caps);
        window.setUndecorated(true);
        window.setSize(800, 600);
        window.setVisible(true);

        drawable = GLDrawableFactory.getFactory(glp).createGLDrawable(window);
        drawable.setRealized(true);

        context = drawable.createContext(null);

        context.makeCurrent();
    }

    @After
    public void tearDown() {
        if(null!=context) {
            context.release();
        }
    }

    private int getShader(GL3 gl, String text, int type) {
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


        return shaderProgram;
    }

    @Test
    public void testGlTransformFeedbackVaryings_WhenVarNameOK() {
        if(!GLProfile.isGL3Available()) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        // given

        GL3 gl = context.getGL().getGL3();
        int shaderProgram = getShader(gl, VERTEX_SHADER_TEXT, GL3.GL_VERTEX_SHADER);
        String[] vars = new String[]{"Position"};

        // when

        gl.glTransformFeedbackVaryings(shaderProgram, 1, vars, GL3.GL_SEPARATE_ATTRIBS);
        gl.glLinkProgram(shaderProgram);

        // then

        if(!ShaderUtil.isProgramValid(gl, shaderProgram, pbaos)) {
            System.out.println("testGlTransformFeedbackVaryings_WhenVarNameOK:postLink: "+baos.toString());
            Assert.assertTrue(false);
        }
        pbaos.flush(); baos.reset();

        Assert.assertEquals(GL3.GL_NO_ERROR, gl.glGetError());
    }

    @Test
    public void testGlTransformFeedbackVaryings_WhenVarNameWrong() {
        if(!GLProfile.isGL3Available()) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        // given

        GL3 gl = context.getGL().getGL3();
        int shaderProgram = getShader(gl, VERTEX_SHADER_TEXT, GL3.GL_VERTEX_SHADER);
        String[] vars = new String[]{"PPPosition"};

        // when

        gl.glTransformFeedbackVaryings(shaderProgram, 1, vars, GL3.GL_SEPARATE_ATTRIBS);
        gl.glLinkProgram(shaderProgram);

        // then

        if(!ShaderUtil.isProgramValid(gl, shaderProgram, pbaos)) {
            System.out.println("testGlTransformFeedbackVaryings_WhenVarNameWrong:postLink: "+baos.toString());
            // should be invalid, due to wrong var name
        } else {
            Assert.assertTrue(false);
        }
        pbaos.flush(); baos.reset();

        Assert.assertEquals(GL3.GL_NO_ERROR, gl.glGetError());
        // You cannot assume this error message - Assert.assertTrue(baos.toString().contains("(named PPPosition)"));
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestTransformFeedbackVeryingsBug407NEWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}
