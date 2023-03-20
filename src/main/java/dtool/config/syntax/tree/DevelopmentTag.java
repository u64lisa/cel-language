package dtool.config.syntax.tree;

import java.util.HashMap;

public class DevelopmentTag extends HashMap<String, String> {

    public void put(String owner, String key, String value) {
        this.put(owner + ":" + key, value);
    }
}
