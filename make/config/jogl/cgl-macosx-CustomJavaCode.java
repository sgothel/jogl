
/**
 * Creates the NSOpenGLLayer for FBO/PBuffer w/ optional GL3 shader program
 * <p>
 * The NSOpenGLLayer will immediatly create a OpenGL context sharing the given ctx,
 * which will be used to render the texture offthread.
 * </p>
 * <p>
 * The NSOpenGLLayer starts in enabled mode, 
 * you may enable/disable it via {@link #setNSOpenGLLayerEnabled(long, boolean)}.
 * </p>
 */
public static long createNSOpenGLLayer(final long ctx, final int gl3ShaderProgramName, final long fmt, final long p, 
                                       final int texID, final boolean opaque, 
                                       final int texWidth, final int texHeight, 
                                       final int winWidth, final int winHeight) {
   return createNSOpenGLLayerImpl(ctx, gl3ShaderProgramName, fmt, p, texID, opaque, texWidth, texHeight, winWidth, winHeight);
}

/**
 * Enable or disable NSOpenGLLayer. 
 *
 * <p>
 * If disabled, the NSOpenGLLayer will not be displayed, i.e. rendered.
 * </p>
 */
public static void setNSOpenGLLayerEnabled(final long nsOpenGLLayer, final boolean enable) {
  setNSOpenGLLayerEnabledImpl(nsOpenGLLayer, enable);
}

/**
 * Releases the NSOpenGLLayer
 */
public static void releaseNSOpenGLLayer(final long nsOpenGLLayer) {
  releaseNSOpenGLLayerImpl(nsOpenGLLayer);
}

