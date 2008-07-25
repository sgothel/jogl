
  public static final int GL_NVIDIA_PLATFORM_BINARY_NV = 0x890B;

  /**
   * Emulated FixedFunction matrix bit, enables PVM matrix functionality,
   * referenced below.
   *
   * <br>Enabled by default.
   *
   * @see #enableFixedFunctionEmulationMode
   * @see #disableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   * @see #GL_MODELVIEW
   * @see #GL_PROJECTION
   * @see #glPopMatrix()
   * @see #glPushMatrix()
   * @see #glLoadIdentity()
   * @see #glLoadMatrixf(java.nio.FloatBuffer)
   * @see #glMatrixMode(int)
   * @see #glMultMatrixf(java.nio.FloatBuffer)
   * @see #glTranslatef(float, float, float)
   * @see #glRotatef(float, float, float, float)
   * @see #glScalef(float, float, float)
   * @see #glOrthof(float, float, float, float, float, float)
   * @see #glFrustumf(float, float, float, float, float, float)
   * @see #glPopMatrix()
   * @see #glPushMatrix()
   * @see #glLoadIdentity()
   * @see #glLoadMatrixf(java.nio.FloatBuffer)
   * @see #glMatrixMode(int)
   * @see #glMultMatrixf(java.nio.FloatBuffer)
   * @see #glTranslatef(float, float, float)
   * @see #glRotatef(float, float, float, float)
   * @see #glScalef(float, float, float)
   * @see #glOrthof(float, float, float, float, float, float)
   * @see #glFrustumf(float, float, float, float, float, float)
   */
  public static final int FIXED_EMULATION_MATRIX       = (1 << 0) ;

  /**
   * Emulated FixedFunction vertex color bit, enables vertex|color arrays ,
   * referenced below.
   *
   * <br>Disabled by default.
   *
   * @see #enableFixedFunctionEmulationMode
   * @see #disableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   * @see #GL_VERTEX_ARRAY
   * @see #GL_NORMAL_ARRAY
   * @see #GL_COLOR_ARRAY
   * @see #GL_TEXTURE_COORD_ARRAY
   * @see #glEnableClientState(int);
   * @see #glVertexPointer(int, int, int, Buffer);
   * @see #glVertexPointer(int, int, int, long);
   * @see #glColorPointer(int, int, int, Buffer);
   * @see #glColorPointer(int, int, int, long);
   * @see #glNormalPointer(int, int, int, Buffer);
   * @see #glNormalPointer(int, int, int, long);
   * @see #glTexCoordPointer(int, int, int, Buffer);
   * @see #glTexCoordPointer(int, int, int, long);
   * @see #glDrawArrays(int, int, int);
   */
  public static final int FIXED_EMULATION_VERTEXCOLOR  = (1 << 1) ;

  /**
   * Emulated FixedFunction vertex color bit, enables normal arrays and lights 
   * Not implemented yet.
  */
  public static final int FIXED_EMULATION_NORMALLIGHT  = (1 << 2) ;

  /**
   * Emulated FixedFunction vertex color bit, enables texcoord arrays and textures 
   * Can be enabled or disabled only in combination with FIXED_EMULATION_VERTEXCOLOR.
   */
  public static final int FIXED_EMULATION_TEXTURE      = (1 << 4) ;

  /**
   * Emulated FixedFunction implementation.
   * @see #disableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   */
  public void enableFixedFunctionEmulationMode(int mode);

  /**
   * Emulated FixedFunction implementation.
   * @see #enableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   */
  public void disableFixedFunctionEmulationMode(int mode);

  /**
   * Emulated FixedFunction matrix implementation.
   * @see #enableFixedFunctionEmulationMode
   */
  public int  getEnabledFixedFunctionEmulationModes();

  /**
   * Emulated FixedFunction matrix implementation.
   *
   * Fetches the internal matrix of matrixName.
   * @param matrixName GL_MODELVIEW or GL_PROJECTION
   * @see #getEnabledFixedFunctionEmulationModes
   * @see #enableFixedFunctionEmulationMode
   * @see #disableFixedFunctionEmulationMode
   */
  public PMVMatrix getPMVMatrix();

  /**
   * Emulated FixedFunction matrix implementation.
   *
   * Fetches the internal matrix of matrixName.
   * @param matrixName GL_MODELVIEW or GL_PROJECTION
   * @see #enableFixedFunctionEmulationMode
   * @see #disableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   */
  public FloatBuffer glGetPMVMatrixf();

  /**
   * Emulated FixedFunction matrix implementation.
   *
   * Fetches the internal matrix of matrixName.
   * @param matrixName GL_MODELVIEW or GL_PROJECTION
   * @see #enableFixedFunctionEmulationMode
   * @see #disableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   */
  public FloatBuffer glGetMatrixf(int matrixName);

  /**
   * Emulated FixedFunction matrix implementation.
   *
   * Fetches the internal matrix of matrixName.
   * @param matrixName GL_MODELVIEW or GL_PROJECTION
   * @see #enableFixedFunctionEmulationMode
   * @see #disableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   */
  public FloatBuffer glGetMatrixf();

  /**
   * Emulated FixedFunction matrix implementation.
   *
   * Fetches the internal matrix of matrixName.
   * @param matrixName GL_MODELVIEW or GL_PROJECTION
   * @see #enableFixedFunctionEmulationMode
   * @see #disableFixedFunctionEmulationMode
   * @see #getEnabledFixedFunctionEmulationModes
   */
  public int  glGetMatrixMode();
