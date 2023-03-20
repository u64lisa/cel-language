package language.vm.library;

import language.backend.compiler.bytecode.types.GenericType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.objects.ClassObjectType;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.Var;
import language.backend.compiler.bytecode.values.bytecode.Native;
import language.backend.compiler.bytecode.values.bytecode.NativeResult;
import language.vm.library.impl.StdMemory;
import language.vm.library.impl.StdSystem;
import language.vm.library.impl.conllection.StdList;
import language.vm.library.impl.conllection.StdMap;
import language.vm.library.impl.math.StdMath;
import language.vm.library.impl.types.StdString;
import language.vm.library.impl.types.StdType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// not an actual java classloader!!
public class LibraryClassLoader {

    public static final LibraryClassLoader LIBRARY_LOADER;
    public static final Map<String, Type> JAVA_TO_CUSTOM = new HashMap<>();
    public static final Map<String, String> CUSTOM_TO_JAVA = new HashMap<>();

    public static final Map<String, Var> LIBRARY_VAR_ARGS = new HashMap<>();
    public static final Map<String, Type> LIBRARY_TYPES = new HashMap<>();

    static {
        JAVA_TO_CUSTOM.put("void", Types.VOID);
        JAVA_TO_CUSTOM.put("Object", Types.ANY);
        JAVA_TO_CUSTOM.put("short", Types.SHORT);
        JAVA_TO_CUSTOM.put("Short", Types.SHORT);
        JAVA_TO_CUSTOM.put("int", Types.INT);
        JAVA_TO_CUSTOM.put("Integer", Types.INT);
        JAVA_TO_CUSTOM.put("Long", Types.LONG);
        JAVA_TO_CUSTOM.put("long", Types.LONG);
        JAVA_TO_CUSTOM.put("float", Types.FLOAT);
        JAVA_TO_CUSTOM.put("Float", Types.FLOAT);
        JAVA_TO_CUSTOM.put("byte", Types.BYTE);
        JAVA_TO_CUSTOM.put("Byte", Types.BYTE);
        JAVA_TO_CUSTOM.put("Double", Types.DOUBLE);
        JAVA_TO_CUSTOM.put("double", Types.DOUBLE);
        JAVA_TO_CUSTOM.put("boolean", Types.BOOL);
        JAVA_TO_CUSTOM.put("Boolean", Types.BOOL);
        JAVA_TO_CUSTOM.put("String", Types.STRING);
        JAVA_TO_CUSTOM.put("byte[]", Types.BYTES);

        JAVA_TO_CUSTOM.forEach((s, type) -> CUSTOM_TO_JAVA.put(type.name, s));

        LIBRARY_LOADER = new LibraryClassLoader();
    }

    public void indexLibraryClasses() {
        this.registerClassesAsLibrary(
                new StdMemory(), new StdSystem(), new StdList(),
                new StdMath(), new StdMap(), new StdType(), new StdString()
        );
    }

    public void registerClassesAsLibrary(final Object... instances) {
        for (Object instance : instances) {
            this.registerClassAsLibrary(instance);
        }
    }

    public void registerClassAsLibrary(final Object instance) {
        final Class<?> clazz = instance.getClass();

        if (clazz.getDeclaredAnnotation(LibraryClass.class) == null)
            throw new IllegalStateException("Can't register class as language library (missing @LibraryClass annotation!");

        final String libraryName = clazz.getDeclaredAnnotation(LibraryClass.class).className();
        final List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(current -> current.getDeclaredAnnotation(LibraryMethod.class) != null)
                .peek(current -> current.setAccessible(true)).toList();

        for (Method method : methods) {
            final Type returnType = JAVA_TO_CUSTOM
                    .getOrDefault(method.getReturnType().getSimpleName(), Types.VOID);

            final Type[] arguments = new Type[method.getGenericParameterTypes().length];

            for (int i = 0; i < method.getGenericParameterTypes().length; i++) {
                final java.lang.reflect.Type javaArgumentType = method.getGenericParameterTypes()[i];
                final String[] split = javaArgumentType.getTypeName().split("\\.");
                final String javaSimpleName = split[split.length - 1];
                final Type customType = JAVA_TO_CUSTOM.getOrDefault(javaSimpleName, Types.VOID);
                arguments[i] = customType;
            }

            LIBRARY_VAR_ARGS.put(method.getName(), new Var(
                    new Value(new Native(method.getName(), stack -> {

                        try {
                            // this is kinda bad code more like a temp fix for value matching
                            Object[] value = mapCustomToJava(stack);
                            for (int i = 0; i < value.length; i++) {
                                final Object object = value[i];

                                if (object.getClass().getSimpleName().equals("Double") &&
                                        (method.getParameters()[i].getType().getSimpleName().equals("int") ||
                                        method.getParameters()[i].getType().getSimpleName().equals("Integer"))) {
                                    Double doubleValue = (Double) object;
                                    value[i] = doubleValue.intValue();
                                }
                                if (object.getClass().getSimpleName().equals("Double") &&
                                        (method.getParameters()[i].getType().getSimpleName().equals("long") ||
                                        method.getParameters()[i].getType().getSimpleName().equals("Long"))) {
                                    Double doubleValue = (Double) object;
                                    value[i] = doubleValue.longValue();
                                }

                            }

                            Object returnValue = method.invoke(instance, value);

                            if (returnType.equals(Types.VOID))
                                return NativeResult.Ok();
                            else
                                return NativeResult.Ok(mapJavaToCustom(returnValue)[0]);

                        } catch (IllegalAccessException | InvocationTargetException e) {
                            return NativeResult.Err(e.getClass().getSimpleName(), e.getMessage());
                        }
                    }, arguments.length, arguments)),
                    true
            ));

            LIBRARY_TYPES.put(method.getName(),
                    new ClassObjectType(returnType, arguments, new GenericType[0], false));

        }

    }

    public Object[] mapCustomToJava(final Value... stack) {
        final Object[] arguments = new Object[stack.length];

        for (int i = 0; i < stack.length; i++) {
            arguments[i] = stack[i].asObject();
        }

        return arguments;
    }

    public Value[] mapJavaToCustom(final Object... stack) {
        final Value[] arguments = new Value[stack.length];

        for (int i = 0; i < stack.length; i++) {
            arguments[i] = Value.fromObject(stack[i]);
        }

        return arguments;
    }

}
