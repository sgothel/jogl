
/**
 * Creates the NSOpenGLLayer for FBO/PBuffer w/ optional GL3 shader program on Main-Thread
 * <p>
 * It is mandatory that the shared context handle <code>ctx</code>
 * is not locked while calling this method. 
 * </p>
 * <p>
 * The NSOpenGLLayer starts in enabled mode, 
 * you may enable/disable it via {@link #setNSOpenGLLayerEnabled(long, boolean)}.
 * </p>
 */
public static long createNSOpenGLLayer(final long ctx, final int gl3ShaderProgramName, final long fmt, final long p, 
                                       final int texID, final boolean opaque, final int texWidth, final int texHeight) {
  return OSXUtil.RunOnMainThread(true, new Function<Long, Object>() {
   public Long eval(Object... args) {
       return Long.valueOf( createNSOpenGLLayerImpl(ctx, gl3ShaderProgramName, fmt, p, texID, opaque, texWidth, texHeight) );
   } } ).longValue();
}

/**
 * Enable or disable NSOpenGLLayer. 
 *
 * <p>
 * If disabled, the NSOpenGLLayer will not be displayed, i.e. rendered.
 * </p>
 */
public static void setNSOpenGLLayerEnabled(final long nsOpenGLLayer, final boolean enable) {
  OSXUtil.RunOnMainThread(true, new Runnable() {
      public void run() {
          setNSOpenGLLayerEnabledImpl(nsOpenGLLayer, enable);
      } } );
}

/**
 * Releases the NSOpenGLLayer on Main-Thread
 */
public static void releaseNSOpenGLLayer(final long nsOpenGLLayer) {
  OSXUtil.RunOnMainThread(true, new Runnable() {
      public void run() {
          releaseNSOpenGLLayerImpl(nsOpenGLLayer);
      } } );
}

