package language.vm.library.impl.math;

import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@LibraryClass(className = "Math")
public class StdMath {

    private final Random RANDOM = ThreadLocalRandom.current();

    @LibraryMethod
    public int round(final double value) {
        return (int) value;
    }

    //  define("round", (args) -> NativeResult.Ok(new Value(Math.round(args[0].asNumber()))), Types.INT, Types.FLOAT);
    //        define("floor", (args) -> NativeResult.Ok(new Value(Math.floor(args[0].asNumber()))), Types.INT, Types.FLOAT);
    //        define("ceil", (args) -> NativeResult.Ok(new Value(Math.ceil(args[0].asNumber()))), Types.INT, Types.FLOAT);
    @LibraryMethod
    public double abs(final double value) {
        return Math.abs(value);
    }
    //        define("arctan2",
    //                (args) -> NativeResult.Ok(new Value(Math.atan2(args[0].asNumber(), args[1].asNumber()))),
    //                Types.FLOAT, Types.FLOAT, Types.FLOAT);
    //        define("sin", (args) -> NativeResult.Ok(new Value(Math.sin(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
    //        define("cos", (args) -> NativeResult.Ok(new Value(Math.cos(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
    //        define("tan", (args) -> NativeResult.Ok(new Value(Math.tan(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
    //        define("arcsin", (args) -> NativeResult.Ok(new Value(Math.asin(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
    //        define("arccos", (args) -> NativeResult.Ok(new Value(Math.acos(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);
    //        define("arctan", (args) -> NativeResult.Ok(new Value(Math.atan(args[0].asNumber()))), Types.FLOAT, Types.FLOAT);

    @LibraryMethod
    public double min(final double first, final double last) {
        return Math.min(first, last);
    }

    @LibraryMethod
    public double max(final double first, final double last) {
        return Math.max(first, last);
    }

    //        define("min",
    //                (args) -> NativeResult.Ok(new Value(Math.min(args[0].asNumber(), args[1].asNumber()))),
    //                Types.FLOAT, Types.FLOAT, Types.FLOAT);
    //        define("max",
    //                (args) -> NativeResult.Ok(new Value(Math.max(args[0].asNumber(), args[1].asNumber()))),
    //                Types.FLOAT, Types.FLOAT, Types.FLOAT);

    //        define("log",
    //                (args) -> NativeResult.Ok(new Value(Math.log(args[0].asNumber()) / Math.log(args[1].asNumber()))),
    //                Types.FLOAT, Types.FLOAT, Types.FLOAT);
    //        define("doubleStr",
    //                (args) -> NativeResult.Ok(new Value(String.format("%." + args[1].asNumber().intValue(), args[0].asNumber()))),
    //                Types.FLOAT, Types.FLOAT, Types.FLOAT);
    //        define("parseNum",
    //                (args) -> {
    //                    try {
    //                        return NativeResult.Ok(new Value(Double.parseDouble(args[0].asString())));
    //                    } catch (NumberFormatException e) {
    //                        return NativeResult.Err("Number Format", "Could not parse number");
    //                    }
    //                },
    //                Types.FLOAT, Types.STRING);
    //
    //        // Random Functions

    @LibraryMethod
    public double random() {
        return RANDOM.nextDouble();
    }

    @LibraryMethod
    public float randomInt(final int min, final int max) {
        return (float) min + Math.round(RANDOM.nextInt() * (max - min + 1));
    }

    //        define("randint", (args) -> {
    //            double min = args[0].asNumber();
    //            double max = args[1].asNumber();
    //            return NativeResult.Ok(new Value(min + Math.round(Math.random() * (max - min + 1))));
    //        }, Types.FLOAT, Types.FLOAT, Types.FLOAT);
    //        define("choose", args -> {
    //            List<Value> list = args[0].asList();
    //            int max = list.size() - 1;
    //            int index = (int) (Math.random() * max);
    //            return NativeResult.Ok(list.get(index));
    //        }, Types.ANY, 1);

}
