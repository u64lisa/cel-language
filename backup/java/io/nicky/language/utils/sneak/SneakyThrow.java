package language.utils.sneak;

import language.utils.sneak.functions.*;

import java.util.function.Supplier;
import java.util.function.*;

public final class SneakyThrow {

    public static <Value> Value sneak(SneakySupplier<Value> supplier) {
        return sneaky(supplier).get();
    }

    public static <T, U, R> BiFunction<T, U, R> sneaky(SneakyBiFunction<T, U, R> biFunction) {
        return (t, u) -> {
            try {
                return biFunction.apply(t, u);
            } catch (final Exception exception) {
                throw new SneakyException(exception);
            }
        };
    }

    public static <T> BinaryOperator<T> sneaky(SneakyBinaryOperator<T> binaryOperator) {
        return (t1, t2) -> {
            try {
                return binaryOperator.apply(t1, t2);
            } catch (final Exception exception) {
                throw new SneakyException(exception);
            }
        };
    }

    public static <T, U> BiPredicate<T, U> sneaky(SneakyBiPredicate<T, U> biPredicate) {
        return (t, u) -> {
            try {
                return biPredicate.test(t, u);
            } catch (final Exception exception) {
                throw new SneakyException(exception);
            }
        };
    }

    public static <T> Consumer<T> sneaky(SneakyConsumer<T> consumer) {
        return (t) -> {
            try {
                consumer.accept(t);
            } catch (final Exception var3) {
                throw new SneakyException(var3);
            }
        };
    }

    public static <T, R> Function<T, R> sneaky(SneakyFunction<T, R> function) {
        return (t) -> {
            try {
                return function.apply(t);
            } catch (final Exception var3) {
                throw new SneakyException(var3);
            }
        };
    }

    public static <T> Predicate<T> sneaky(SneakyPredicate<T> predicate) {
        return (t) -> {
            try {
                return predicate.test(t);
            } catch (final Exception var3) {
                throw new SneakyException(var3);
            }
        };
    }

    public static Runnable sneaky(SneakyRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Exception var2) {
                throw new SneakyException(var2);
            }
        };
    }

    public static <T> Supplier<T> sneaky(SneakySupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (final Exception var2) {
                throw new SneakyException(var2);
            }
        };
    }

    public static <T> UnaryOperator<T> sneaky(SneakyUnaryOperator<T> unaryOperator) {
        return (t) -> {
            try {
                return unaryOperator.apply(t);
            } catch (final Exception var3) {
                throw new SneakyException(var3);
            }
        };
    }

}
