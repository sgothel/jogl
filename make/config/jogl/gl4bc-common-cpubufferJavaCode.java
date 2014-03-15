
  /** Entry point to C language function: <code> void {@native glDrawArraysIndirect}(GLenum mode, const GLvoid *  indirect); </code> <br>Part of <code>GL_VERSION_4_0</code>, <code>GL_ARB_draw_indirect</code>
      @param indirect a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawArraysIndirect(int mode, Buffer indirect);

  /** Entry point to C language function: <code> void {@native glDrawElementsIndirect}(GLenum mode, GLenum type, const GLvoid *  indirect); </code> <br>Part of <code>GL_VERSION_4_0</code>, <code>GL_ARB_draw_indirect</code>
      @param indirect a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsIndirect(int mode, int type, Buffer indirect);

  /** Entry point to C language function: <code> void {@native glMultiDrawArraysIndirect}(GLenum mode, const void *  indirect, GLsizei drawcount, GLsizei stride); </code> <br>Part of <code>GL_VERSION_4_3</code>, <code>GL_ARB_multi_draw_indirect</code>
      @param indirect a direct or array-backed {@link java.nio.Buffer}   */
  public void glMultiDrawArraysIndirect(int mode, Buffer indirect, int drawcount, int stride);

  /** Entry point to C language function: <code> void {@native glDrawElementsInstancedBaseInstance}(GLenum mode, GLsizei count, GLenum type, const void *  indices, GLsizei instancecount, GLuint baseinstance); </code> <br>Part of <code>GL_VERSION_4_2</code>, <code>GL_ARB_base_instance</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsInstancedBaseInstance(int mode, int count, int type, Buffer indices, int instancecount, int baseinstance);

  /** Entry point to C language function: <code> void {@native glDrawElementsInstancedBaseVertexBaseInstance}(GLenum mode, GLsizei count, GLenum type, const void *  indices, GLsizei instancecount, GLint basevertex, GLuint baseinstance); </code> <br>Part of <code>GL_VERSION_4_2</code>, <code>GL_ARB_base_instance</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, Buffer indices, int instancecount, int basevertex, int baseinstance);

  /** Entry point to C language function: <code> void {@native glVertexAttribLPointer}(GLuint index, GLint size, GLenum type, GLsizei stride, const GLvoid *  pointer); </code> <br>Part of <code>GL_VERSION_4_1</code>, <code>GL_ARB_vertex_attrib_64bit</code>
      @param pointer a direct only {@link java.nio.Buffer}   */
  public void glVertexAttribLPointer(int index, int size, int type, int stride, Buffer pointer);

