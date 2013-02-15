
/**
 * Creates the NSOpenGLLayer for FBO/PBuffer w/ optional GL3 shader program on Main-Thread
 */
public static long createNSOpenGLLayer(final long ctx, final int gl3ShaderProgramName, final long fmt, final long p, 
                                       final int texID, final boolean opaque, final int texWidth, final int texHeight) {
  return OSXUtil.RunOnMainThread(true, new Function<Long, Object>() {
   public Long eval(Object... args) {
       return Long.valueOf( createNSOpenGLLayerImpl(ctx, gl3ShaderProgramName, fmt, p, texID, opaque, texWidth, texHeight) );
   } } ).longValue();
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

