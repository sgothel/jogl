package com.jogamp.opengl;

/**
 * <p>
 * Composable pipeline which wraps an underlying {@link GL} implementation,
 * providing error checking after each OpenGL method call. If an error occurs,
 * causes a {@link GLException} to be thrown at exactly the point of failure.
 * </p>
 * <p>
 * Sample code which installs this pipeline, manual:
 * <pre>
 *     gl = drawable.setGL(new DebugGL(drawable.getGL()));
 * </pre>
 * For automatic instantiation see {@link GLPipelineFactory#create(String, Class, GL, Object[])}.
 * </p>
 */
public class DebugGL3bc extends DebugGL4bc {
    public DebugGL3bc(final GL3bc downstream) {
        super((GL4bc)downstream);
    }
}
