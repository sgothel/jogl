
  /** Entry point to C language function: <code> void {@native glDrawElementsInstanced}(GLenum mode, GLsizei count, GLenum type, const GLvoid *  indices, GLsizei instancecount); </code> <br>Part of <code>GL_ES_VERSION_3_0</code>, <code>GL_VERSION_3_1</code>; <code>GL_ARB_draw_instanced</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsInstanced(int mode, int count, int type, Buffer indices, int instancecount);

  /** Entry point to C language function: <code> void {@native glDrawRangeElements}(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const GLvoid *  indices); </code> <br>Part of <code>GL_VERSION_1_2</code>, <code>GL_ES_VERSION_3_0</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices);

  /** Entry point to C language function: <code> void {@native glVertexAttribIPointer}(GLuint index, GLint size, GLenum type, GLsizei stride, const GLvoid *  pointer); </code> <br>Part of <code>GL_ES_VERSION_3_0</code>, <code>GL_VERSION_3_0</code>
      @param pointer a direct only {@link java.nio.Buffer}   */
  public void glVertexAttribIPointer(int index, int size, int type, int stride, Buffer pointer);

