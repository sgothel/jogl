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
public class DebugGL2 extends DebugGL4bc {
    public DebugGL2(final GL2 downstream) {
        super((GL4bc)downstream);
    }
}
