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
public class TraceGL3 extends TraceGL4bc {
    public TraceGL3(final GL3 downstream, final PrintStream stream) {
        super((GL4bc)downstream, stream);
    }
}
