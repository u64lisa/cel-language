package dtool.config.syntax.tree;

import java.util.HashMap;

public class ProjectProperties extends HashMap<String, String> {

    public boolean hasAttribute(String key) {
        return this.containsKey(key);
    }

}
