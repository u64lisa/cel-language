package dtool.source;

public enum SourceSuffix {

    SOURCE("ag", false),

    UNKNOWN("/../", false);


    private static final SourceSuffix[] fastValues = SourceSuffix.values();

    private final String suffix;
    private final boolean requiresSubFile;

    SourceSuffix(String suffix, boolean requiresSubFile) {
        this.suffix = suffix;
        this.requiresSubFile = requiresSubFile;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean isRequiresSubFile() {
        return requiresSubFile;
    }

    public static SourceSuffix fromSuffix(String suffix) {
        for (SourceSuffix fastValue : fastValues) {
            if (fastValue.suffix.equals(suffix)) {
                return fastValue;
            }
        }
        return UNKNOWN;
    }

}
