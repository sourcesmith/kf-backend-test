package uk.co.truenotfalse;

/**
 * Provides a version for the distribution.  An application can use this class to display a distribution version.
 */
public final class Version {
    /**
     * The version as a String.
     */
    public String getVersion() {
        return version;
    }


    @Override
    public String toString() {
        return version;
    }


    /**
     * Outputs the version.
     *
     * @param args Not used.
     */
    public static void main(final String[] args) {
        System.out.println("Outage Agent/" + version);
    }


    static {
        final String implVersion = Version.class.getPackage().getImplementationVersion();

        if (implVersion == null || implVersion.isEmpty()) {
            version = "version unknown";
        } else {
            version = implVersion;
        }
    }


    private static final String version;
}
