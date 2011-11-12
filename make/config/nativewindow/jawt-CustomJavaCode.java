/** Available and recommended on Mac OS X >= 10.6 Update 4 */
public static final int JAWT_MACOSX_USE_CALAYER = 0x80000000;
public static final VersionNumber JAWT_MacOSXCALayerMinVersion = new VersionNumber(10,6,4);

private int jawt_version_cached = 0;

public final int getCachedVersion() {
    return jawt_version_cached;
}

/** Helper routine for all users to call to access the JAWT. */
public static JAWT getJAWT(final int jawt_version_flags) {
    JAWTUtil.initSingleton();
    // Workaround for 4845371.
    // Make sure the first reference to the JNI GetDirectBufferAddress is done
    // from a privileged context so the VM's internal class lookups will succeed.
    return AccessController.doPrivileged(new PrivilegedAction<JAWT>() {
        public JAWT run() {
          int jawt_version_flags_mod = jawt_version_flags;
          JAWT jawt = JAWT.create();
          if( 0 != ( jawt_version_flags_mod & JAWT_MACOSX_USE_CALAYER ) ) {
              jawt.setVersion(jawt_version_flags_mod);
              if (JAWTFactory.JAWT_GetAWT(jawt)) {
                  jawt.jawt_version_cached = jawt.getVersion();
                  return jawt;
              }
              jawt_version_flags_mod &= ~JAWT_MACOSX_USE_CALAYER;
              System.err.println("MacOSX "+Platform.OS_VERSION_NUMBER+" >= "+JAWT_MacOSXCALayerMinVersion+": Failed to use JAWT_MACOSX_USE_CALAYER");
          }
          jawt.setVersion(jawt_version_flags_mod);
          if (!JAWTFactory.JAWT_GetAWT(jawt)) {
            throw new RuntimeException("Unable to initialize JAWT: 0x"+Integer.toHexString(jawt_version_flags_mod));
          }
          jawt.jawt_version_cached = jawt.getVersion();
          return jawt;
        }
      });
}
