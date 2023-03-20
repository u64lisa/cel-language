package dtool.logger.profiler;

import io.nicky.language.Language;

public class Segment implements AutoCloseable {

    private final String name;
    private final long startTime;
    private long endTime;

    private final boolean spacer;

    Segment(String name) {
        this.name = name;
        this.spacer = false;
        this.startTime = System.nanoTime();
    }
    Segment(String name, boolean spacer) {
        this.name = name;
        this.spacer = spacer;
        this.startTime = System.nanoTime();
    }

    @Override
    public void close() {
        this.endTime = System.nanoTime();
         Language.PROFILER.getProfiled().add(this);
    }

    public long getDeltaTimeNSec() {
        return this.endTime - this.startTime;
    }

    public String getName() {
        return name;
    }

    public boolean isSpacer() {
        return spacer;
    }
}
