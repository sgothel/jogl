  /** Part of <code>GL_ARB_separate_shader_objects</code>, <code>GL_EXT_separate_shader_objects</code> */
  public static final int GL_ALL_SHADER_BITS = 0xFFFFFFFF ;

  /** Start: GL_ARB_ES2_compatibility functions, which are part of ES2 core as well */

  /** Entry point to C language function: 
   * <code> void {@native glReleaseShaderCompiler}(void); </code> 
   * <br>Part of <code>GL_ES_VERSION_2_0</code> and <code>GL_ARB_ES2_compatibility</code>. 
   * <br> Nop if no native implementation is available.   */
  public void glReleaseShaderCompiler();

  /** Entry point to C language function: 
   * <code> void {@native glShaderBinary}(GLint n, const GLuint *  shaders, GLenum binaryformat, const void *  binary, GLint length); </code> 
   * <br>Part of <code>GL_ES_VERSION_2_0</code> and <code>GL_ARB_ES2_compatibility</code>. 
   * <br> Throws GLException if no native implementation is available.   */
  public void glShaderBinary(int n, IntBuffer shaders, int binaryformat, Buffer binary, int length);

  /** Entry point to C language function: 
   * <code> void {@native glShaderBinary}(GLint n, const GLuint *  shaders, GLenum binaryformat, const void *  binary, GLint length); </code> 
   * <br>Part of <code>GL_ES_VERSION_2_0</code> and <code>GL_ARB_ES2_compatibility</code>. 
   * <br> Throws GLException if no native implementation is available.   */
  public void glShaderBinary(int n, int[] shaders, int shaders_offset, int binaryformat, Buffer binary, int length);

  /** Entry point to C language function: 
   * <code> void {@native glGetShaderPrecisionFormat}(GLenum shadertype, GLenum precisiontype, GLint *  range, GLint *  precision); </code> 
   * <br>Part of <code>GL_ES_VERSION_2_0</code> and <code>GL_ARB_ES2_compatibility</code>. 
   * <br> Throws GLException if no native implementation is available.   */
  public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision);

  /** Entry point to C language function: 
   * <code> void {@native glGetShaderPrecisionFormat}(GLenum shadertype, GLenum precisiontype, GLint *  range, GLint *  precision); </code> 
   * <br>Part of <code>GL_ES_VERSION_2_0</code> and <code>GL_ARB_ES2_compatibility</code>. 
   * <br> Throws GLException if no native implementation is available.   */
  public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, int[] range, int range_offset, int[] precision, int precision_offset);

  /** Entry point to C language function: 
   * <code> void {@native glDepthRangef}(GLclampf zNear, GLclampf zFar); </code> 
   * <br>Part of <code>GL_ES_VERSION_2_0</code> and <code>GL_ARB_ES2_compatibility</code>. 
   * <br> Calls <code> void {@native glDepthRange}(GLclampd zNear, GLclampd zFar); </code> if no native implementation is available.   */
  public void glDepthRangef(float zNear, float zFar);

  public void glDepthRange(double zNear, double zFar);

  /** Entry point to C language function: 
   * <code> void {@native glClearDepthf}(GLclampf depth); </code> 
   * <br>Part of <code>GL_ES_VERSION_2_0</code> and <code>GL_ARB_ES2_compatibility</code>. 
   * <br> Calls <code> void {@native glClearDepth}(GLclampd depth); </code> if no native implementation is available.   */
  public void glClearDepthf(float depth);

  public void glClearDepth( double depth );

  public void glVertexAttribPointer(GLArrayData array);

  public void glUniform(GLUniformData data);

  /** Part of <code>GL_VERSION_1_0</code>, <code>GL_ES_VERSION_2_0</code>, <code>GL_ANGLE_depth_texture</code> */
  public static final int GL_DEPTH_COMPONENT = 0x1902;

  /** End: GL_ARB_ES2_compatibility functions, which are part of ES2 core as well */
