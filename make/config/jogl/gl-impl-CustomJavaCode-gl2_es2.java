  public void glVertexAttribPointer(GLArrayData array) {
    if(array.getComponentNumber()==0) return;
    if(array.isVBO()) {
        glVertexAttribPointer(array.getLocation(), array.getComponentNumber(), array.getComponentType(), 
                              array.getNormalized(), array.getStride(), array.getVBOOffset());
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

