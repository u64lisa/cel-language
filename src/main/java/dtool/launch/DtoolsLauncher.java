package dtool.launch;

import dtool.config.DtoolConfig;

public class DtoolsLauncher {

    private static final String CONFIG_PATH = "./test/";

    public static void main(String[] args) {
        final DtoolConfig config = new DtoolConfig(CONFIG_PATH, "system.dtool");
    }

}
