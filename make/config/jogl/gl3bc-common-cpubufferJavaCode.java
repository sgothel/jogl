
  /** Entry point to C language function: <code> void {@native glDrawElementsBaseVertex}(GLenum mode, GLsizei count, GLenum type, const GLvoid *  indices, GLint basevertex); </code> <br>Part of <code>GL_ARB_draw_elements_base_vertex</code>, <code>GL_VERSION_3_2</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsBaseVertex(int mode, int count, int type, Buffer indices, int basevertex);

  /** Entry point to C language function: <code> void {@native glDrawElementsInstancedBaseVertex}(GLenum mode, GLsizei count, GLenum type, const GLvoid *  indices, GLsizei instancecount, GLint basevertex); </code> <br>Part of <code>GL_ARB_draw_elements_base_vertex</code>, <code>GL_VERSION_3_2</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsInstancedBaseVertex(int mode, int count, int type, Buffer indices, int instancecount, int basevertex);

  /** Entry point to C language function: <code> void {@native glDrawRangeElementsBaseVertex}(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const GLvoid *  indices, GLint basevertex); </code> <br>Part of <code>GL_ARB_draw_elements_base_vertex</code>, <code>GL_VERSION_3_2</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawRangeElementsBaseVertex(int mode, int start, int end, int count, int type, Buffer indices, int basevertex);

