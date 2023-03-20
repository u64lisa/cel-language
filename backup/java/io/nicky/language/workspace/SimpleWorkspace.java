package io.nicky.language.workspace;

import io.nicky.language.workspace.config.ProjectConfiguration;

import java.nio.file.Path;

public class SimpleWorkspace extends AbstractWorkspace {

    public SimpleWorkspace(Path sourceDirectory, Path binaryDirectory) {
        super(new ProjectConfiguration(sourceDirectory), sourceDirectory, binaryDirectory);
    }

}
