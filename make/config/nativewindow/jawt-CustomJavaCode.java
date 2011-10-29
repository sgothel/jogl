/** Available and recommended on Mac OS X >= 10.6 Update 4 */
public static final int JAWT_MACOSX_USE_CALAYER = 0x80000000;
public static final VersionNumber JAWT_MacOSXCALayerMinVersion = new VersionNumber(10,6,4);

private static volatile JAWT jawt = null;
private static int jawt_version_flags = 0;

private int jawt_version_cached = 0;

public final int getVersionCached() {
    return jawt_version_cached;
}

public static void setJAWTVersionFlags(int versionFlags) {
    synchronized (JAWT.class) {
      if (jawt != null) {
        throw new RuntimeException("JAWT already instantiated");
      }
      jawt_version_flags = versionFlags;
    }
}

/** Helper routine for all users to call to access the JAWT. */
public static JAWT getJAWT() {
  if (jawt == null) {
    synchronized (JAWT.class) {
      if (jawt == null) {
        JAWTUtil.initSingleton();
        // Workaround for 4845371.
        // Make sure the first reference to the JNI GetDirectBufferAddress is done
        // from a privileged context so the VM's internal class lookups will succeed.
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
              JAWT j = JAWT.create();
              if( 0 != ( jawt_version_flags & JAWT_MACOSX_USE_CALAYER ) ) {
                  j.setVersion(jawt_version_flags);
                  if (JAWTFactory.JAWT_GetAWT(j)) {
                      jawt = j;
                      jawt.jawt_version_cached = jawt.getVersion();
                      return null;
                  }
                  jawt_version_flags &= ~JAWT_MACOSX_USE_CALAYER;
                  System.err.println("MacOSX "+Platform.OS_VERSION_NUMBER+" >= "+JAWT_MacOSXCALayerMinVersion+": Failed to use JAWT_MACOSX_USE_CALAYER");
              }
              j.setVersion(jawt_version_flags);
              if (!JAWTFactory.JAWT_GetAWT(j)) {
                throw new RuntimeException("Unable to initialize JAWT: 0x"+Integer.toHexString(jawt_version_flags));
              }
              jawt = j;
              jawt.jawt_version_cached = jawt.getVersion();
              return null;
            }
          });
      }
    }
  }
  return jawt;
}
