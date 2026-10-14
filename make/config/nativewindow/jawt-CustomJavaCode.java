private int jawt_version_cached = 0;

public final int getCachedVersion() {
    return jawt_version_cached;
}

protected static boolean getJAWT(final JAWT jawt, final int jawt_version_flags) {
    JAWTUtil.initSingleton();
    jawt.setVersion(jawt_version_flags);
    if (JAWTFactory.JAWT_GetAWT(jawt)) {
        jawt.jawt_version_cached = jawt.getVersion();
        return true;
    }
    jawt.jawt_version_cached = 0;
    return false;
}
