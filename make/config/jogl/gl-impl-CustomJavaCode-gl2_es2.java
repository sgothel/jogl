    public String glGetShaderInfoLog(int shaderObj) {
        int[] infoLogLength=new int[1];
        glGetShaderiv(shaderObj, GL_INFO_LOG_LENGTH, infoLogLength, 0);

        if(infoLogLength[0]==0) {
            return "(InfoLog null)";
        }
        int[] charsWritten=new int[1];
        byte[] infoLogBytes = new byte[infoLogLength[0]];
        glGetShaderInfoLog(shaderObj, infoLogLength[0], charsWritten, 0, infoLogBytes, 0);

        return new String(infoLogBytes, 0, charsWritten[0]);
    }

    public String glGetProgramInfoLog(int programObj) {
        int[] infoLogLength=new int[1];
        glGetProgramiv(programObj, GL_INFO_LOG_LENGTH, infoLogLength, 0);

        if(infoLogLength[0]==0) {
            return "(InfoLog null)";
        }
        int[] charsWritten=new int[1];
        byte[] infoLogBytes = new byte[infoLogLength[0]];
        glGetProgramInfoLog(programObj, infoLogLength[0], charsWritten, 0, infoLogBytes, 0);

        return new String(infoLogBytes, 0, charsWritten[0]);
    }

    public boolean glIsShaderStatusValid(int shaderObj, int name) {
        return glIsShaderStatusValid(shaderObj, name, null);
    }
    public boolean glIsShaderStatusValid(int shaderObj, int name, PrintStream verboseOut) {
        int[] ires = new int[1];
        glGetShaderiv(shaderObj, name, ires, 0);

        boolean res = ires[0]==1;
        if(!res && null!=verboseOut) {
            verboseOut.println("Shader status invalid: "+glGetShaderInfoLog(shaderObj));
        }
        return res;
    }
    public boolean glIsShaderStatusValid(IntBuffer shaders, int name) {
        return glIsShaderStatusValid(shaders, name, null);
    }
    public boolean glIsShaderStatusValid(IntBuffer shaders, int name, PrintStream verboseOut) {
        boolean res = true;
        shaders.rewind();
        while(shaders.hasRemaining()) {
            res = glIsShaderStatusValid(shaders.get(), name, verboseOut) && res;
        }
        shaders.rewind();
        return res;
    }

    public boolean glIsProgramStatusValid(int programObj, int name) {
        int[] ires = new int[1];
        glGetProgramiv(programObj, name, ires, 0);

        return ires[0]==1;
    }

    public boolean glIsProgramValid(int programObj) {
        return glIsProgramValid(programObj, null);
    }
    public boolean glIsProgramValid(int programObj, PrintStream verboseOut) {
        int[] ires = new int[1];
        if(!glIsProgram(programObj)) {
            if(null!=verboseOut) {
                verboseOut.println("Program name invalid: "+programObj);
            }
            return false;
        }
        if(!glIsProgramStatusValid(programObj, GL_LINK_STATUS)) {
            if(null!=verboseOut) {
                verboseOut.println("Program link failed: "+programObj+"\n\t"+ glGetProgramInfoLog(programObj));
            }
            return false;
        }
        if ( !isGLES2() || glShaderCompilerAvailable() ) {
            // failed on APX2500 (ES2.0, no compiler) for valid programs
            glValidateProgram(programObj);
            if(!glIsProgramStatusValid(programObj, GL_VALIDATE_STATUS)) {
                if(null!=verboseOut) {
                    verboseOut.println("Program validation failed: "+programObj+"\n\t"+ glGetProgramInfoLog(programObj));
                }
                return false;
            }
        }
        return true;
    }

  public void glCreateShader(int type, IntBuffer shaders) {
    shaders.clear();
    while(shaders.hasRemaining()) {
        shaders.put(glCreateShader(type));
    }
    shaders.rewind();
  }

  private Boolean shaderCompilerAvailable = null;
  private Set shaderBinaryFormats = null;

  public Set glGetShaderBinaryFormats() 
  {
    if(null==shaderBinaryFormats) {
        if(getContext()!=GLContext.getCurrent()) {
            return new HashSet(0); // bail out
        }
        HashSet formatSet = new HashSet();

        int[] param = new int[1];

        glGetIntegerv(GL2ES2.GL_NUM_SHADER_BINARY_FORMATS, param, 0);
        int numFormats = param[0];
        if(numFormats>0) {
            int[] formats = new int[numFormats];
            glGetIntegerv(GL2ES2.GL_SHADER_BINARY_FORMATS, formats, 0);
            shaderBinaryFormats = new HashSet(numFormats);
            for(int i=0; i<numFormats; i++) {
                shaderBinaryFormats.add(new Integer(formats[i]));
            }
        } else {
            shaderBinaryFormats = new HashSet(0);
        }
    }
    return shaderBinaryFormats;
  }


  public boolean glShaderCompilerAvailable() {
    if(null==shaderCompilerAvailable) {
        if(getContext()!=GLContext.getCurrent()) {
            return false; // bail out
        }
        Set bfs = glGetShaderBinaryFormats();
        if(isGLES2()) {
            byte[] param = new byte[1];
            glGetBooleanv(GL2ES2.GL_SHADER_COMPILER, param, 0);
            boolean v = param[0]!=(byte)0x00;
            if(!v && bfs.size()==0) {
                // no supported binary formats, hence a compiler must be available!
                v = true;
            }
            shaderCompilerAvailable = new Boolean(v);
        } else if( isGL2() || isGL2ES2() ) {
            shaderCompilerAvailable = new Boolean(true);
        } else {
            throw new GLException("invalid profile");
        }
    }
    return shaderCompilerAvailable.booleanValue();
  }

  public void glShaderSource(int shader, java.lang.String[] source)
  {
    if(!glShaderCompilerAvailable()) {
        throw new GLException("no compiler is available");
    }

    int count = (null!=source)?source.length:0;
    if(count<=0) {
        throw new GLException("Method \"glShaderSource\" called with invalid length of source: "+count);
    }
    IntBuffer length = InternalBufferUtil.newIntBuffer(count);
    for(int i=0; i<count; i++) {
        length.put(source[i].length());
    }
    length.flip();
    glShaderSource(shader, count, source, length);
  }

  public void glShaderSource(IntBuffer shaders, java.lang.String[][] sources)
  {
    int sourceNum = (null!=sources)?sources.length:0;
    int shaderNum = (null!=shaders)?shaders.remaining():0;
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
    if(glGetShaderBinaryFormats().size()<=0) {
        throw new GLException("no binary formats are supported");
    }

    int shaderNum = shaders.remaining();
    if(shaderNum<=0) {
        throw new GLException("Method \"glShaderBinary\" called with shaders number <= 0");
    }
    if(null==bin) {
        throw new GLException("Method \"glShaderBinary\" without binary (null)");
    }
    int binLength = bin.remaining();
    if(0>=binLength) {
        throw new GLException("Method \"glShaderBinary\" without binary (remaining == 0)");
    }
    glShaderBinary(shaderNum, shaders, binFormat, bin, binLength);
  }

  public void glCompileShader(IntBuffer shaders)
  {
    shaders.rewind();
    while(shaders.hasRemaining()) {
        glCompileShader(shaders.get());
    }
    shaders.rewind();
  }

  public void glAttachShader(int program, IntBuffer shaders)
  {
    shaders.rewind();
    while(shaders.hasRemaining()) {
        glAttachShader(program, shaders.get());
    }
    shaders.rewind();
  }

  public void glDetachShader(int program, IntBuffer shaders)
  {
    shaders.rewind();
    while(shaders.hasRemaining()) {
        glDetachShader(program, shaders.get());
    }
    shaders.rewind();
  }

  public void glDeleteShader(IntBuffer shaders) {
    shaders.rewind();
    while(shaders.hasRemaining()) {
        glDeleteShader(shaders.get());
    }
    shaders.clear();
  }

  public void glVertexAttribPointer(GLArrayData array) {
    if(array.getComponentNumber()==0) return;
    if(array.isVBO()) {
        glVertexAttribPointer(array.getLocation(), array.getComponentNumber(), array.getComponentType(), 
                              array.getNormalized(), array.getStride(), array.getOffset());
    } else {
        glVertexAttribPointer(array.getLocation(), array.getComponentNumber(), array.getComponentType(), 
                              array.getNormalized(), array.getStride(), array.getBuffer());
    }
  }

  public void glUniform(GLUniformData data) {
    boolean done=false;
    if(data.isBuffer()) {
        Buffer buffer = data.getBuffer();
        if(data.isMatrix()) {
            if(buffer instanceof FloatBuffer) {
                switch(data.columns()) {
                    case 2: glUniformMatrix2fv(data.getLocation(), data.count(), false, (FloatBuffer)buffer); done=true; break;
                    case 3: glUniformMatrix3fv(data.getLocation(), data.count(), false, (FloatBuffer)buffer); done=true; break;
                    case 4: glUniformMatrix4fv(data.getLocation(), data.count(), false, (FloatBuffer)buffer); done=true; break;
                }
            }
            if(!done) {
                throw new GLException("glUniformMatrix only available for 2fv, 3fv and 4fv");
            }
        } else {
            if(buffer instanceof IntBuffer) {
                switch(data.components()) {
                    case 1: glUniform1iv(data.getLocation(), data.count(), (IntBuffer)buffer); done=true; break;
                    case 2: glUniform2iv(data.getLocation(), data.count(), (IntBuffer)buffer); done=true; break;
                    case 3: glUniform3iv(data.getLocation(), data.count(), (IntBuffer)buffer); done=true; break;
                    case 4: glUniform4iv(data.getLocation(), data.count(), (IntBuffer)buffer); done=true; break;
                }
            } else if(buffer instanceof FloatBuffer) {
                switch(data.components()) {
                    case 1: glUniform1fv(data.getLocation(), data.count(), (FloatBuffer)buffer); done=true; break;
                    case 2: glUniform2fv(data.getLocation(), data.count(), (FloatBuffer)buffer); done=true; break;
                    case 3: glUniform3fv(data.getLocation(), data.count(), (FloatBuffer)buffer); done=true; break;
                    case 4: glUniform4fv(data.getLocation(), data.count(), (FloatBuffer)buffer); done=true; break;
                }
            }
            if(!done) {
                throw new GLException("glUniform vector only available for 1[if]v 2[if]v, 3[if]v and 4[if]v");
            }
        }
    } else {
        Object obj = data.getObject();
        if(obj instanceof Integer) {
            glUniform1i(data.getLocation(), ((Integer)obj).intValue());
            done=true;
        } else if (obj instanceof Float) {
            glUniform1f(data.getLocation(), ((Float)obj).floatValue());
            done=true;
        }
        if(!done) {
            throw new GLException("glUniform atom only available for 1i and 1f");
        }
    }
  }

  public final String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(getClass().getName());
      if (isGL2ES2()) {
          buf.append(" [GLSL compiler: ");
          buf.append(glShaderCompilerAvailable());
          Set bfs = glGetShaderBinaryFormats();
          buf.append(", binary formats ");
          buf.append(bfs.size());
          buf.append(":");
          for(Iterator iter=bfs.iterator(); iter.hasNext(); ) {
              buf.append(" ");
              buf.append(((Integer)(iter.next())).intValue());
          }
          buf.append("]");
      }
      return buf.toString();
  }


