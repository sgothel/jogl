

  public void glShaderSource(int shader, java.lang.String[] source)
  {
    int count = (null!=source)?source.length:0;
    if(count<=0) {
        throw new GLException("Method \"glShaderSource\" called with invalid length of source: "+count);
    }
    IntBuffer length = BufferUtil.newIntBuffer(count);
    for(int i=0; i<count; i++) {
        length.put(source[i].length());
    }
    length.flip();
    glShaderSource(shader, count, source, length);
  }

  public void glShaderSource(IntBuffer shaders, java.lang.String[][] sources)
  {
    int sourceNum = (null!=sources)?sources.length:0;
    int shaderNum = (null!=shaders)?shaders.limit():0;
    if(shaderNum<=0 || sourceNum<=0 || shaderNum!=sourceNum) {
        throw new GLException("Method \"glShaderSource\" called with invalid number of shaders and/or sources: shaders="+
            shaderNum+", sources="+sourceNum);
    }
    for(int i=0; i<sourceNum; i++) {
        glShaderSource(shaders.get(i), sources[i]);
    }
  }

  public void glShaderBinary(IntBuffer shaders, int binFormat, java.nio.Buffer bin)
  {
    int shaderNum = shaders.limit();
    if(shaderNum<=0) {
        throw new GLException("Method \"glShaderBinary\" called with shaders number <= 0");
    }
    if(null==bin) {
        throw new GLException("Method \"glShaderBinary\" without binary (null)");
    }
    int binLength = bin.limit();
    if(0>=binLength) {
        throw new GLException("Method \"glShaderBinary\" without binary (limit == 0)");
    }
    try {
        glShaderBinary(shaderNum, shaders, binFormat, bin, binLength);
    } catch (Exception e) { }
  }

  /**
   * Wrapper for glShaderBinary and glShaderSource.
   * Tries binary first, if not null, then the source code, if not null.
   * The binary trial will fail in case no binary interface exist (GL2 profile),
   * hence the fallback to the source code.
   */
  public void glShaderBinaryOrSource(IntBuffer shaders, 
                                     int binFormat, java.nio.Buffer bin,
                                     java.lang.String[][] sources)
  {
    int shaderNum = shaders.limit();
    if(shaderNum<=0) {
        throw new GLException("Method \"glShaderBinaryOrSource\" called with shaders number <= 0");
    }
    if(null!=bin) {
        try {
            glShaderBinary(shaders, binFormat, bin);
            return; // done
        } catch (Exception e) { }
    }
    if(null!=sources) {
        glShaderSource(shaders, sources);
        return; // done
    }
    throw new GLException("Method \"glShaderBinaryOrSource\" without binary nor source");
  }


