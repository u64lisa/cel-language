package dtool.io.walker;

import dtool.io.ProjectFolder;

public abstract class FileWalker {

    public abstract void walk(ProjectFolder folder, String[] structure);

    public static FileWalker create() {
        return new DefaultFileWalker();
    }

}
