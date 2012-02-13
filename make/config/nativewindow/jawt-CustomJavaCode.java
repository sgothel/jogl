private int jawt_version_cached = 0;

public final int getCachedVersion() {
    return jawt_version_cached;
}

protected static boolean getJAWT(final JAWT jawt, final int jawt_version_flags) {
    JAWTUtil.initSingleton();
    // Workaround for 4845371.
    // Make sure the first reference to the JNI GetDirectBufferAddress is done
    // from a privileged context so the VM's internal class lookups will succeed.
    return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        public Boolean run() {
          jawt.setVersion(jawt_version_flags);
          if (JAWTFactory.JAWT_GetAWT(jawt)) {
            jawt.jawt_version_cached = jawt.getVersion();
            return new Boolean(true);
          }
          jawt.jawt_version_cached = 0;
          return new Boolean(false);
        }
      }).booleanValue();
}
