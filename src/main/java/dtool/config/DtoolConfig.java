package dtool.config;

import dtool.config.syntax.DtoolConfigLexer;
import dtool.config.syntax.DtoolConfigParser;
import dtool.config.syntax.DtoolToken;
import dtool.config.syntax.tree.ConfigTree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class DtoolConfig {

    private ConfigTree configTree = new ConfigTree();

    private final String path;
    private final String fileName;
    private boolean loaded = false;

    public DtoolConfig(String path, String fileName) {
        this.path = path;
        this.fileName = fileName;
    }

    public void load() {
        try {
            this.init();

            if (!this.checkNeeded()) {
                loaded = false; // todo dont replace that file if one simple thing is missing

                this.createDefaultConfig();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!loaded) {
            throw new RuntimeException("DtoolConfig is not loaded.");
        }
    }

    private void init() throws Exception {
        final Path configPath = Path.of(path, fileName);
        final File configFile = configPath.toFile();

        if (configFile.exists() && configFile.isFile()) {
            final InputStream configStream = new FileInputStream(configFile);
            final Scanner scanner = new Scanner(configStream);
            final String config = scanner.useDelimiter("\\A").next();

            scanner.close();

            final DtoolConfigLexer configLexer = new DtoolConfigLexer();
            List<DtoolToken> tokenList = configLexer.parse(path, config.getBytes(StandardCharsets.UTF_8));

            final DtoolConfigParser configParser = new DtoolConfigParser(config, tokenList);

            configTree = configParser.parse();

            loaded = true;
            return;
        }

        createDefaultConfig();
        this.init();
    }

    private void createDefaultConfig() {
        final InputStream configStream = DtoolConfig.class.getClassLoader()
                .getResourceAsStream("default.dtool");

        assert configStream != null: "Unable to load default config";

        String config = new Scanner(configStream).useDelimiter("\\A").next();

        try {
            configStream.close();

            final FileWriter fileWriter = new FileWriter(Path.of(path, fileName).toFile());

            fileWriter.write(config);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkNeeded() {
        return this.configTree.getProjectProperties().containsKey("name") &&
                this.configTree.getProjectProperties().containsKey("version") &&
                this.configTree.getProjectProperties().containsKey("main") &&
                this.configTree.getProjectProperties().containsKey("id");
    }

    public ConfigTree getConfigTree() {
        return configTree;
    }
}
