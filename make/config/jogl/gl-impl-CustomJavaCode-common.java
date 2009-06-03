  public GLProfile getGLProfile() {
    return this.glProfile;
  }
  private GLProfile glProfile;

  public int glGetBoundBuffer(int target) {
    return bufferStateTracker.getBoundBufferObject(target, this);
  }

  public boolean glIsVBOArrayEnabled() {
    return checkArrayVBOEnabled(false);
  }

  public boolean glIsVBOElementEnabled() {
    return checkElementVBOEnabled(false);
  }
