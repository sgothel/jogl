
    static final DynamicLibraryBundle dynamicLookupHelper;

    static {
        dynamicLookupHelper = AccessController.doPrivileged(new PrivilegedAction<DynamicLibraryBundle>() {
                    public DynamicLibraryBundle run() {
                        final DynamicLibraryBundle bundle =  new DynamicLibraryBundle(new OVRDynamicLibraryBundleInfo());
                        if(null==bundle) {
                          throw new RuntimeException("Null DynamicLibraryBundle");
                        }
                        /** No native tool library to load
                        if(!bundle.isToolLibLoaded()) {
                          System.err.println("Couldn't load native OVR/JNI glue library");
                          return null;
                        } */
                        if(!bundle.isLibComplete()) {
                          System.err.println("Couldn't load native OVR/JNI glue library");
                          return null;
                        }
                        if( !initializeImpl() ) {
                          System.err.println("Native initialization failure of OVR/JNI glue library");
                          return null;
                        }
                        return bundle;
                    } } );
    }

    /**
     * Accessor.
     * @returns true if OVR library is available on this machine.
     */
    public static boolean isAvailable() { return dynamicLookupHelper != null; }

