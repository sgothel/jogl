private static volatile JAWT jawt;

/** Helper routine for all users to call to access the JAWT. */
public static JAWT getJAWT() {
  if (jawt == null) {
    synchronized (JAWT.class) {
      if (jawt == null) {
        NativeLibLoader.loadAWTImpl();
        // Workaround for 4845371.
        // Make sure the first reference to the JNI GetDirectBufferAddress is done
        // from a privileged context so the VM's internal class lookups will succeed.
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              JAWT j = JAWT.create();
              j.version(JAWTFactory.JAWT_VERSION_1_4);
              if (!JAWTFactory.JAWT_GetAWT(j)) {
                throw new RuntimeException("Unable to initialize JAWT");
              }
              jawt = j;
              return null;
            }
          });
      }
    }
  }
  return jawt;
}
