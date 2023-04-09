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
import com.jogamp.opengl.TraceGL4;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.PMVMatrix;

public class TrianglesInstancedRendererHardcoded implements GLEventListener {
	private float aspect;

	private int shaderProgram;
	private int vertShader;
	private int fragShader;
	private int projectionMatrixLocation;
	private int transformMatrixLocation;
	private static final int locPos = 1;
	private static final int locCol = 2;
	private PMVMatrix projectionMatrix;

	private static final int NO_OF_INSTANCE = 30;
	private final FloatBuffer triangleTransform = FloatBuffer.allocate(16 * NO_OF_INSTANCE);
	private final Matrix4f[] mat = new Matrix4f[NO_OF_INSTANCE];
	private final float[] rotationSpeed = new float[NO_OF_INSTANCE];

	private int[] vbo;
	private int[] vao;
	private PrintStream stream;
	private final IInstancedRenderingView view;

	private static final boolean useTraceGL = false;

	public TrianglesInstancedRendererHardcoded(final IInstancedRenderingView view) {
		this.view = view;
		initTransform();

		if(useTraceGL) {
			try {
				stream = new PrintStream(new FileOutputStream(new File("instanced.txt")));
			} catch (final IOException e1) {
				e1.printStackTrace();
			}
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
		//	gl.glClearColor(0, 0, 0, 1); //black background
		gl.glClearDepth(1.0f);

		System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
		System.err.println("INIT GL IS: " + gl.getClass().getName());
		System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
		System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
		System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));

		try {
			initShaders(gl);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		initVBO(gl);
	}

	@Override
	public void display(final GLAutoDrawable drawable) {

		final GL4 gl = drawable.getGL().getGL4();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(shaderProgram);

		projectionMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);

		projectionMatrix.glPushMatrix();
		float winScale = 0.1f;
		if(view != null) {
			winScale = view.getScale();
		}
		projectionMatrix.glScalef(winScale, winScale, winScale);
		gl.glUniformMatrix4fv(projectionMatrixLocation, 1, false, projectionMatrix.getSyncPMat().getSyncFloats());
		projectionMatrix.glPopMatrix();
		generateTriangleTransform();
		gl.glUniformMatrix4fv(transformMatrixLocation, NO_OF_INSTANCE, false, triangleTransform);

		gl.glBindVertexArray(vao[0]);
		gl.glDrawArraysInstanced(GL.GL_TRIANGLES, 0, 3, NO_OF_INSTANCE);
		gl.glBindVertexArray(0);
		gl.glUseProgram(0);
	}

	@Override
	public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
		System.out.println("Window resized to width=" + width + " height=" + height);
		final GL4 gl3 = drawable.getGL().getGL4();
		gl3.glViewport(0, 0, width, height);
		aspect = (float) width / (float) height;

		projectionMatrix = new PMVMatrix();
		projectionMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		projectionMatrix.glLoadIdentity();
		projectionMatrix.gluPerspective(45, aspect, 0.001f, 20f);
		projectionMatrix.gluLookAt(new Vec3f(0, 0, -10), new Vec3f(0, 0, 0), new Vec3f(0, 1, 0));
	}

	@Override
	public void dispose(final GLAutoDrawable drawable){
		final GL4 gl = drawable.getGL().getGL4();
		gl.glUseProgram(0);
		gl.glDeleteBuffers(2, vbo, 0);
		gl.glDetachShader(shaderProgram, vertShader);
		gl.glDeleteShader(vertShader);
		gl.glDetachShader(shaderProgram, fragShader);
		gl.glDeleteShader(fragShader);
		gl.glDeleteProgram(shaderProgram);
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

	private void initVBO(final GL4 gl) {
		final FloatBuffer interleavedBuffer = Buffers.newDirectFloatBuffer(vertices.length + colors.length);
		for(int i = 0; i < vertices.length/3; i++) {
			for(int j = 0; j < 3; j++) {
				interleavedBuffer.put(vertices[i*3 + j]);
			}
			for(int j = 0; j < 4; j++) {
				interleavedBuffer.put(colors[i*4 + j]);
			}
		}
		interleavedBuffer.flip();

		vao = new int[1];
		gl.glGenVertexArrays(1, vao , 0);
		gl.glBindVertexArray(vao[0]);
		vbo = new int[1];
		gl.glGenBuffers(1, vbo, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, interleavedBuffer.limit() * Buffers.SIZEOF_FLOAT, interleavedBuffer, GL.GL_STATIC_DRAW);

		gl.glEnableVertexAttribArray(locPos);
		gl.glEnableVertexAttribArray(locCol);

		final int stride = Buffers.SIZEOF_FLOAT * (3+4);
		gl.glVertexAttribPointer( locPos, 3, GL.GL_FLOAT, false, stride, 0);
		gl.glVertexAttribPointer( locCol, 4, GL.GL_FLOAT, false, stride, Buffers.SIZEOF_FLOAT * 3);
	}

	private void initShaders(final GL4 gl) throws IOException {
		vertShader = gl.glCreateShader(GL2ES2.GL_VERTEX_SHADER);
		fragShader = gl.glCreateShader(GL2ES2.GL_FRAGMENT_SHADER);

		final String[] vlines = new String[] { vertexShaderString };
		final int[] vlengths = new int[] { vlines[0].length() };
		gl.glShaderSource(vertShader, vlines.length, vlines, vlengths, 0);
		gl.glCompileShader(vertShader);

		final int[] compiled = new int[1];
		gl.glGetShaderiv(vertShader, GL2ES2.GL_COMPILE_STATUS, compiled, 0);
		if(compiled[0] != 0) {
			System.out.println("Vertex shader compiled");
		} else {
			final int[] logLength = new int[1];
			gl.glGetShaderiv(vertShader, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);

			final byte[] log = new byte[logLength[0]];
			gl.glGetShaderInfoLog(vertShader, logLength[0], (int[])null, 0, log, 0);

			System.err.println("Error compiling the vertex shader: " + new String(log));
			System.exit(1);
		}

		final String[] flines = new String[] { fragmentShaderString };
		final int[] flengths = new int[] { flines[0].length() };
		gl.glShaderSource(fragShader, flines.length, flines, flengths, 0);
		gl.glCompileShader(fragShader);

		gl.glGetShaderiv(fragShader, GL2ES2.GL_COMPILE_STATUS, compiled, 0);
		if(compiled[0] != 0){
			System.out.println("Fragment shader compiled.");
		} else {
			final int[] logLength = new int[1];
			gl.glGetShaderiv(fragShader, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0);

			final byte[] log = new byte[logLength[0]];
			gl.glGetShaderInfoLog(fragShader, logLength[0], (int[])null, 0, log, 0);

			System.err.println("Error compiling the fragment shader: " + new String(log));
			System.exit(1);
		}

		shaderProgram = gl.glCreateProgram();
		gl.glAttachShader(shaderProgram, vertShader);
		gl.glAttachShader(shaderProgram, fragShader);

		gl.glBindAttribLocation(shaderProgram, locPos, "mgl_Vertex");
		gl.glBindAttribLocation(shaderProgram, locCol, "mgl_Color");

		gl.glLinkProgram(shaderProgram);

		projectionMatrixLocation = gl.glGetUniformLocation(shaderProgram, "mgl_PMatrix");
		System.out.println("projectionMatrixLocation:" + projectionMatrixLocation);
		transformMatrixLocation = gl.glGetUniformLocation(shaderProgram, "mgl_MVMatrix");
		System.out.println("transformMatrixLocation:" + transformMatrixLocation);
	}

	private void generateTriangleTransform() {
		triangleTransform.clear();
		final Matrix4f tmp = new Matrix4f();
		for(int i = 0; i < NO_OF_INSTANCE; i++) {
			//		mat[i].translate(0.1f, 0.1f, 0);
			mat[i].rotate(rotationSpeed[i], 0, 0, 1, tmp);
			//		mat[i].translate(-0.1f, -0.1f, 0);
			mat[i].get(triangleTransform);
		}
		triangleTransform.flip();
	}

	private final String vertexShaderString =
			"#version 410 \n" +
					"\n" +
					"uniform mat4 mgl_PMatrix; \n" +
					"uniform mat4 mgl_MVMatrix[" + NO_OF_INSTANCE + "]; \n" +
					"in vec3  mgl_Vertex; \n" +
					"in vec4  mgl_Color; \n" +
					"out vec4 frontColor; \n" +
					"void main(void) \n" +
					"{ \n" +
					"  frontColor = mgl_Color; \n" +
					"  gl_Position = mgl_PMatrix * mgl_MVMatrix[gl_InstanceID] * vec4(mgl_Vertex, 1);" +
					"} ";

	private final String fragmentShaderString =
			"#version 410\n" +
					"\n" +
					"in vec4    frontColor; \n" +
					"out vec4    mgl_FragColor; \n" +
					"void main (void) \n" +
					"{ \n" +
					"  mgl_FragColor = frontColor; \n" +
					"} ";

	private final float[] vertices = {
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
