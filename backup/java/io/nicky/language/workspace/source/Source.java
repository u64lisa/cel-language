package io.nicky.language.workspace.source;

import java.nio.file.Path;

public interface Source {

    Source extract();
    Source readSource();

    String getDirectory();
    String getFile();

    String getSource();

    Path getFilePath();



}
