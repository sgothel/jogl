  public final boolean matchesProfile() {
    return matchesProfile(GLProfile.getProfile());
  }

  public final boolean matchesProfile(String test_profile) {
    if(null==test_profile) {
        return false;
    }
    if(test_profile.equals(GLProfile.GL3)) {
        return isGL3();
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

  public int glGetBoundBuffer(int target) {
    return bufferStateTracker.getBoundBufferObject(target, this);
  }

  public boolean glIsVBOArrayEnabled() {
    return checkArrayVBOEnabled(false);
  }

  public boolean glIsVBOElementEnabled() {
    return checkElementVBOEnabled(false);
  }

