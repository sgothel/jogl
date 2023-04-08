package com.jogamp.opengl.test.junit.jogl.demos.gl4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.util.Random;

import com.jogamp.opengl.DebugGL4;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.TraceGL4;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.GLArrayDataClient;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class TriangleInstancedRendererWithShaderState implements GLEventListener {
	private float aspect;

	private static final String shaderBasename = "triangles";
//	private static final boolean useInterleaved = false;
	private static final boolean useInterleaved = true;

	private ShaderState st;
	private PMVMatrix projectionMatrix;
	private GLUniformData projectionMatrixUniform;
	private GLUniformData transformMatrixUniform;

	private GLArrayDataServer interleavedVBO;
	private GLArrayDataClient verticesVBO;
	private GLArrayDataClient colorsVBO;

	private static final int NO_OF_INSTANCE = 30;
	private final FloatBuffer triangleTransform = FloatBuffer.allocate(16 * NO_OF_INSTANCE);
	private final Matrix4f[] mat = new Matrix4f[NO_OF_INSTANCE];
	private final float[] rotationSpeed = new float[NO_OF_INSTANCE];

	private static final boolean useTraceGL = false;
	private PrintStream stream;
	private final IInstancedRenderingView view;

	private boolean isInitialized = false;

	public TriangleInstancedRendererWithShaderState(final IInstancedRenderingView view) {
		this.view = view;

		if(useTraceGL) {
			try {
				stream = new PrintStream(new FileOutputStream(new File("instanced-with-st.txt")));
			} catch (final IOException e1) {
				e1.printStackTrace();
			}
		}

		initTransform();
	}

	private void initTransform() {
		final Random rnd = new Random();
		final Matrix4f tmp = new Matrix4f();
		for(int i = 0; i < NO_OF_INSTANCE; i++) {
			rotationSpeed[i] = 0.3f * rnd.nextFloat();
			mat[i] = new Matrix4f();
			mat[i].loadIdentity();
			final float scale = 1f + 4 * rnd.nextFloat();
			mat[i].scale(scale, tmp);
			//setup initial position of each triangle
			mat[i].translate(20f * rnd.nextFloat() - 10f,
							 10f * rnd.nextFloat() -  5f,
							 0f, tmp);
		}
	}

	@Override
	public void init(final GLAutoDrawable drawable) {
		final GL4 gl = drawable.getGL().getGL4();
		drawable.setGL(new DebugGL4(gl));
		if(useTraceGL) {
			drawable.setGL(new TraceGL4(gl, stream));
		}

		gl.glClearColor(1, 1, 1, 1); //white background
//		gl.glClearColor(0, 0, 0, 1); //black background
		gl.glClearDepth(1.0f);

		System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
		System.err.println("INIT GL IS: " + gl.getClass().getName());
		System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
		System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
		System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));

		initShader(gl);
        projectionMatrix = new PMVMatrix();
		projectionMatrixUniform = new GLUniformData("mgl_PMatrix", 4, 4, projectionMatrix.getSyncPMat());
		st.ownUniform(projectionMatrixUniform);
        if(!st.uniform(gl, projectionMatrixUniform)) {
            throw new GLException("Error setting mgl_PMatrix in shader: " + st);
        }

        transformMatrixUniform =  new GLUniformData("mgl_MVMatrix", 4, 4, triangleTransform);

        st.ownUniform(transformMatrixUniform);
        if(!st.uniform(gl, transformMatrixUniform)) {
            throw new GLException("Error setting mgl_MVMatrix in shader: " + st);
        }

        if(useInterleaved) {
        	initVBO_interleaved(gl);
        } else {
        	initVBO_nonInterleaved(gl);
        }

		isInitialized = true;
	}

	@Override
	public void display(final GLAutoDrawable drawable) {
		if(!isInitialized ) return;

		final GL4 gl = drawable.getGL().getGL4();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		st.useProgram(gl, true);
		projectionMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		projectionMatrix.glPushMatrix();

		float winScale = 0.1f;
		if(view != null) winScale = view.getScale();
		projectionMatrix.glScalef(winScale, winScale, winScale);
		projectionMatrix.update();
		st.uniform(gl, projectionMatrixUniform);
		projectionMatrix.glPopMatrix();

		generateTriangleTransform();
		st.uniform(gl, transformMatrixUniform);
		if(useInterleaved) {
			interleavedVBO.enableBuffer(gl, true);
		} else {
			verticesVBO.enableBuffer(gl, true);
			colorsVBO.enableBuffer(gl, true);
		}
		//gl.glVertexAttribDivisor() is not required since each instance has the same attribute (color).
		gl.glDrawArraysInstanced(GL.GL_TRIANGLES, 0, 3, NO_OF_INSTANCE);
		if(useInterleaved) {
			interleavedVBO.enableBuffer(gl, false);
		} else {
			verticesVBO.enableBuffer(gl, false);
			colorsVBO.enableBuffer(gl, false);
		}
		st.useProgram(gl, false);
	}

	@Override
	public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
		final GL4 gl3 = drawable.getGL().getGL4();
		gl3.glViewport(0, 0, width, height);
		aspect = (float) width / (float) height;

		projectionMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		projectionMatrix.glLoadIdentity();
		projectionMatrix.gluPerspective(45, aspect, 0.001f, 20f);
		projectionMatrix.gluLookAt(new Vec3f(0, 0, -10), new Vec3f(0, 0, 0), new Vec3f(0, 1, 0));
	}

	@Override
	public void dispose(final GLAutoDrawable drawable){
		final GL4 gl = drawable.getGL().getGL4();
		st.destroy(gl);
	}

	private void generateTriangleTransform() {
		triangleTransform.clear();
		final Matrix4f tmp = new Matrix4f();
		for(int i = 0; i < NO_OF_INSTANCE; i++) {
			mat[i].rotate(rotationSpeed[i], 0, 0, 1, tmp);
			mat[i].get(triangleTransform);
		}
		triangleTransform.rewind();
	}

	private void initVBO_nonInterleaved(final GL4 gl) {
		final int VERTEX_COUNT = 3;

        verticesVBO = GLArrayDataClient.createGLSL("mgl_Vertex", 3, GL.GL_FLOAT, false, VERTEX_COUNT);
        final FloatBuffer verticeBuf = (FloatBuffer)verticesVBO.getBuffer();
        verticeBuf.put(vertices);
        verticesVBO.seal(gl, true);

        colorsVBO = GLArrayDataClient.createGLSL("mgl_Color",  4, GL.GL_FLOAT, false, VERTEX_COUNT);
        final FloatBuffer colorBuf = (FloatBuffer)colorsVBO.getBuffer();
        colorBuf.put(colors);
        colorsVBO.seal(gl, true);

        verticesVBO.enableBuffer(gl, false);
        colorsVBO.enableBuffer(gl, false);

        st.ownAttribute(verticesVBO, true);
        st.ownAttribute(colorsVBO, true);
		st.useProgram(gl, false);
	}

	private void initVBO_interleaved(final GL4 gl) {
		final int VERTEX_COUNT = 3;
		interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3 + 4, GL.GL_FLOAT, false, VERTEX_COUNT, GL.GL_STATIC_DRAW);
        interleavedVBO.addGLSLSubArray("mgl_Vertex", 3, GL.GL_ARRAY_BUFFER);
        interleavedVBO.addGLSLSubArray("mgl_Color",  4, GL.GL_ARRAY_BUFFER);

        final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();

        for(int i = 0; i < VERTEX_COUNT; i++) {
            ib.put(vertices,  i*3, 3);
            ib.put(colors,    i*4, 4);
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);
        st.ownAttribute(interleavedVBO, true);
		st.useProgram(gl, false);
	}

    private void initShader(final GL4 gl) {
        // Create & Compile the shader objects
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                                            "shader", "shader/bin", shaderBasename, true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                                            "shader", "shader/bin", shaderBasename, true);
        vp0.replaceInShaderSource("NO_OF_INSTANCE", String.valueOf(NO_OF_INSTANCE));

        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);

        //vp0.dumpShaderSource(System.out);

        // Create & Link the shader program
        final ShaderProgram sp = new ShaderProgram();
        sp.add(vp0);
        sp.add(fp0);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, true);
    }


	private static final float[] vertices = {
			1.0f, 0.0f, 0,
			-0.5f, 0.866f, 0,
			-0.5f, -0.866f, 0
	};

	private final float[] colors = {
			1.0f, 0.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,
			0f, 0f, 1.0f, 1f
	};

}
