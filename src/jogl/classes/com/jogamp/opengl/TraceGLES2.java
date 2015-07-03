package com.jogamp.opengl;

import java.io.PrintStream;

/**
 * <p>
 * Composable pipeline which wraps an underlying {@link GL} implementation,
 * providing tracing information to a user-specified {@link java.io.PrintStream}
 * before and after each OpenGL method call.
 * </p>
 * <p>
 * Sample code which installs this pipeline, manual:
 * <pre>
 *     gl = drawable.setGL(new TraceGL(drawable.getGL(), System.err));
 * </pre>
 * For automatic instantiation see {@link GLPipelineFactory#create(String, Class, GL, Object[])}.
 * </p>
 */
public class TraceGLES2 extends TraceGLES3 {
    public TraceGLES2(final GLES2 downstream, final PrintStream stream) {
        super((GLES3)downstream, stream);
    }
}
