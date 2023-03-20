package dtool.logger.profiler;

import dtool.logger.Logger;
import dtool.logger.ImplLogger;
import language.utils.TimeUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;


public class LanguageProfiler {

    private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.##");

    private final Queue<Segment> profiled = new ConcurrentLinkedDeque<>();

    public Collection<Segment> profiledSegments() {
        return new ArrayList<>(profiled);
    }

    public <T> T profileSegment(final String name, Supplier<T> segment) {
        final Segment currentSegment = new Segment(name);
        final T result = segment.get();
        currentSegment.close();
        return result;
    }

    public void profileSegment(final String name, Runnable segment) {
        final Segment currentSegment = new Segment(name);
        segment.run();
        currentSegment.close();
    }


    public void printProfileResults() {
        final Logger logger = ImplLogger.getInstance();

        long totalTime = 0;
        for (Segment segment : profiledSegments()) {
            totalTime += segment.getDeltaTimeNSec();
        }

        logger.printObject("=".repeat(60));
        logger.printObject(String.format("%-20s %-20s %10s", "Segment", "Time (Millis)", "% of Total"));
        logger.printObject("=".repeat(60));
        for (Segment segment : profiledSegments()) {
            if (segment.isSpacer()) {
                logger.printObject("=".repeat(60));
                continue;
            }
            long delta = segment.getDeltaTimeNSec();
            double percentage = 0;
            if (totalTime > 0) {
                percentage = (((double) delta / (double) totalTime) * 100);
            }

            logger.printObject(String.format("%-20s %13s %16s%%", segment.getName(), TimeUtils.toMilliseconds(delta), DECIMAL_FORMAT.format(percentage)));

        }
        logger.printObject("=".repeat(60));
        logger.printObject(String.format("%s %s", "Total Time:", TimeUtils.toMilliseconds(totalTime)));
        logger.printObject("=".repeat(60));
    }

    public Queue<Segment> getProfiled() {
        return profiled;
    }

    public void separator() {
        Segment segment = new Segment("spacer", true);
        segment.close();
    }
}