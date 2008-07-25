  public final boolean isGL2() {
    return GLProfile.implementationOfGL2(this);
  }

  public final boolean isGLES1() {
    return GLProfile.implementationOfGLES1(this);
  }

  public final boolean isGLES2() {
    return GLProfile.implementationOfGLES2(this);
  }

  public final boolean isGLES() {
    return GLProfile.implementationOfGLES(this);
  }

  public final boolean isGL2ES1() {
    return GLProfile.implementationOfGL2ES1(this);
  }

  public final boolean isGL2ES2() {
    return GLProfile.implementationOfGL2ES2(this);
  }

  public final GL2 getGL2() throws GLException {
    if(!isGL2()) {
        throw new GLException("Not a GL2 implementation");
    }
    return (GL2)this;
  }

  public final GLES1 getGLES1() throws GLException {
    if(!isGLES1()) {
        throw new GLException("Not a GLES1 implementation");
    }
    return (GLES1)this;
  }

  public final GLES2 getGLES2() throws GLException {
    if(!isGLES2()) {
        throw new GLException("Not a GLES2 implementation");
    }
    return (GLES2)this;
  }

  public final GL2ES1 getGL2ES1() throws GLException {
    if(!isGL2ES1()) {
        throw new GLException("Not a GL2ES1 implementation");
    }
    return (GL2ES1)this;
  }

  public final GL2ES2 getGL2ES2() throws GLException {
    if(!isGL2ES2()) {
        throw new GLException("Not a GL2ES2 implementation");
    }
    return (GL2ES2)this;
  }

  public final boolean matchesProfile() {
    return matchesProfile(GLProfile.getProfile());
  }

  public final boolean matchesProfile(String test_profile) {
    if(null==test_profile) {
        return false;
    }
    if(test_profile.equals(GLProfile.GL2)) {
        return isGL2();
    }
    if(test_profile.equals(GLProfile.GLES1)) {
        return isGLES1();
    }
    if(test_profile.equals(GLProfile.GLES2)) {
        return isGLES2();
    }
    return false;
  }

