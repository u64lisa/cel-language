package dtool.config.syntax.tree;

public class ConfigTree {

    // project properties
    private final ProjectProperties projectProperties = new ProjectProperties();
    // dependency properties
    private final DependTag dependTag = new DependTag();
    // development properties
    private final DevelopmentTag developmentTag = new DevelopmentTag();
    // plugins
    private final PluginsTag pluginsTag = new PluginsTag();

    @Override
    public String toString() {
        return "ConfigTree{" +
                "projectProperties=" + projectProperties +
                ", dependTag=" + dependTag +
                ", developmentTag=" + developmentTag +
                ", pluginsTag=" + pluginsTag +
                '}';
    }

    public ProjectProperties getProjectProperties() {
        return projectProperties;
    }

    public DependTag getDependTag() {
        return dependTag;
    }

    public DevelopmentTag getDevelopmentTag() {
        return developmentTag;
    }

    public PluginsTag getPluginsTag() {
        return pluginsTag;
    }

}