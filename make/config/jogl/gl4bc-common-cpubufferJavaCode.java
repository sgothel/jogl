
  /** Entry point to C language function: <code> void {@native glDrawElementsInstancedBaseInstance}(GLenum mode, GLsizei count, GLenum type, const void *  indices, GLsizei instancecount, GLuint baseinstance); </code> <br>Part of <code>GL_VERSION_4_2</code>, <code>GL_ARB_base_instance</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsInstancedBaseInstance(int mode, int count, int type, Buffer indices, int instancecount, int baseinstance);

  /** Entry point to C language function: <code> void {@native glDrawElementsInstancedBaseVertexBaseInstance}(GLenum mode, GLsizei count, GLenum type, const void *  indices, GLsizei instancecount, GLint basevertex, GLuint baseinstance); </code> <br>Part of <code>GL_VERSION_4_2</code>, <code>GL_ARB_base_instance</code>
      @param indices a direct or array-backed {@link java.nio.Buffer}   */
  public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, Buffer indices, int instancecount, int basevertex, int baseinstance);

  /** Entry point to C language function: <code> void {@native glVertexAttribLPointer}(GLuint index, GLint size, GLenum type, GLsizei stride, const GLvoid *  pointer); </code> <br>Part of <code>GL_VERSION_4_1</code>, <code>GL_ARB_vertex_attrib_64bit</code>
      @param pointer a direct only {@link java.nio.Buffer}   */
  public void glVertexAttribLPointer(int index, int size, int type, int stride, Buffer pointer);

