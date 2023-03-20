package language.vm;

import language.backend.compiler.Chunk;

class Traceback {
    String filename;
    String context;
    int offset;
    Chunk chunk;

    public Traceback(String filename, String context, int offset, Chunk chunk) {
        this.filename = filename;
        this.context = context;
        this.offset = offset;
        this.chunk = chunk;
    }
}