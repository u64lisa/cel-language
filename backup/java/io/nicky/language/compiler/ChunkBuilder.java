package language.backend.compiler;

import language.backend.compiler.bytecode.types.GenericType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.objects.*;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.ValueArray;
import language.backend.compiler.bytecode.values.enums.LanguageEnum;
import language.backend.compiler.bytecode.values.enums.LanguageEnumChild;
import language.backend.compiler.bytecode.values.bytecode.ByteCode;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class ChunkBuilder {

    public static final int ASYNC   = 0x0001;
    public static final int CATCHER = 0x0010;
    public static final int VARARGS = 0x0100;
    public static final int KW_ARGS = 0x1000;

    int position = 0;
    int[] code;
    final TypeReader reader;

    private ChunkBuilder(byte[] code) {
        this.code = new int[code.length / 4];
        this.reader = new TypeReader();
        for (int index = 0; index < this.code.length; index++) {
            int value = 0;
            value |= (code[index * 4] & 0xFF) << 24;
            value |= (code[index * 4 + 1] & 0xFF) << 16;
            value |= (code[index * 4 + 2] & 0xFF) << 8;
            value |= (code[index * 4 + 3] & 0xFF);
            this.code[index] = value;
        }
    }

    private class TypeReader {

        private Map<String, Type> readAttributes() throws IOException {
            int size = code[position++];
            Map<String, Type> attributes = new HashMap<>();
            for (int j = 0; j < size; j++) {
                String name = readString();
                Type type = readType();
                attributes.put(name, type);
            }
            return attributes;
        }

        private Type readClass() throws IOException {
            if (code[position++] != TypeCodes.CLASS)
                throw new IOException("Invalid type code");
            String name = readString();
            ClassType parent = null;
            if (code[position++] == 1) {
                parent = (ClassType) readType();
            }
            ClassObjectType constructor = (ClassObjectType) readType();

            Map<String, Type> fields = new HashMap<>();
            Set<String> privates = new HashSet<>();
            int size = code[position++];
            for (int j = 0; j < size; j++) {
                String fieldName = readString();
                Type type = readType();
                fields.put(fieldName, type);
                if (code[position++] == 1) {
                    privates.add(fieldName);
                }
            }
            Map<String, Type> staticFields = readAttributes();
            Map<String, Type> operators = readAttributes();
            return new ClassType(name, parent, constructor, fields, privates, staticFields, operators, constructor.generics);
        }

        public Type readEnumChild() throws IOException {
            if (code[position++] != TypeCodes.ENUMCHILD)
                throw new IOException("Invalid type code");

            String name = readString();
            int proptypeCount = code[position++];
            Type[] proptype = new Type[proptypeCount];
            GenericType[] generics = readArgs(proptypeCount, proptype);
            int propCount = code[position++];
            String[] props = new String[propCount];
            for (int j = 0; j < propCount; j++) {
                props[j] = readString();
            }
            return new EnumChildType(name, proptype, generics, props);
        }

        public Type readEnum() throws IOException {
            if (code[position++] != TypeCodes.ENUM)
                throw new IOException("Invalid type code");
            String name = readString();
            int childCount = code[position++];
            EnumChildType[] children = new EnumChildType[childCount];
            for (int j = 0; j < childCount; j++) {
                children[j] = (EnumChildType) readType();
            }
            return new EnumType(name, children);
        }

        public Type readFunc() throws IOException {
            if (code[position++] != TypeCodes.FUNC)
                throw new IOException("Invalid type code");
            Type returnType = readType();
            int argCount = code[position++];
            Type[] args = new Type[argCount];
            GenericType[] generics = readArgs(argCount, args);
            boolean isVararg = code[position++] == 1;
            int defaultCount = code[position++];
            return new ClassObjectType(returnType, args, generics, isVararg, defaultCount);
        }

        GenericType[] readArgs(int argCount, Type[] args) throws IOException {
            for (int j = 0; j < argCount; j++) {
                args[j] = readType();
            }
            int genericCount = code[position++];
            GenericType[] generics = new GenericType[genericCount];
            for (int j = 0; j < genericCount; j++) {
                generics[j] = (GenericType) readType();
            }
            return generics;
        }

        public Type readInstance() throws IOException {
            if (code[position++] != TypeCodes.INSTANCE)
                throw new IOException("Invalid type code");
            ClassType type = (ClassType) readType();
            int genericCount = code[position++];
            Type[] generics = new Type[genericCount];
            for (int j = 0; j < genericCount; j++) {
                generics[j] = readType();
            }
            return new InstanceType(type, generics);
        }

        public Type readNamespace() throws IOException {
            if (code[position++] != TypeCodes.NAMESPACE)
                throw new IOException("Invalid type code");
            return new NamespaceType(readAttributes());
        }

        public Type readReference() throws IOException {
            if (code[position] != TypeCodes.REFERENCE)
                throw new IOException("Invalid type code");
            position++;
            return new ReferenceType(readType());
        }
    }

    private String readString() throws IOException {
        if (code[position] != ChunkCode.String)
            throw new IOException("not string");
        position++;
        int len = code[position++];
        byte[] bytes = new byte[len];
        for (int j = 0; j < len; j++) {
            bytes[j] = (byte) (code[position++] >> 2);
        }
        return new String(bytes);
    }

    private Type readType() throws IOException {
        if (code[position] != ChunkCode.Type)
            throw new IOException("not type");
        position++;
        int typeType = code[position];
        switch (typeType) {
            // Objects
            case TypeCodes.CLASS:
                return reader.readClass();
            case TypeCodes.ENUMCHILD:
                return reader.readEnumChild();
            case TypeCodes.ENUM:
                return reader.readEnum();
            case TypeCodes.FUNC:
                return reader.readFunc();
            case TypeCodes.INSTANCE:
                return reader.readInstance();
            case TypeCodes.NAMESPACE:
                return reader.readNamespace();
            case TypeCodes.REFERENCE:
                return reader.readReference();

            // Primitives
            case TypeCodes.BOOL:
                position++;
                return Types.BOOL;
            case TypeCodes.BYTES:
                position++;
                return Types.BYTES;
            case TypeCodes.MAP:
                position++;
                return Types.MAP;

            case TypeCodes.FLOAT:
                position++;
                return Types.FLOAT;
            case TypeCodes.INT:
                position++;
                return Types.INT;

            case TypeCodes.LONG:
                position++;
                return Types.LONG;
            case TypeCodes.DOUBLE:
                position++;
                return Types.DOUBLE;
            case TypeCodes.BYTE:
                position++;
                return Types.BYTE;
            case TypeCodes.SHORT:
                position++;
                return Types.SHORT;


            case TypeCodes.LIST:
                position++;
                return Types.LIST;
            case TypeCodes.RESULT:
                position++;
                return Types.RESULT;
            case TypeCodes.STRING:
                position++;
                return Types.STRING;
            case TypeCodes.VOID:
                position++;
                return Types.VOID;
            case TypeCodes.ANY:
                position++;
                return Types.ANY;

            // Generic
            case TypeCodes.GENERIC:
                position++;
                return new GenericType(readString());
        }
        throw new IOException("unknown type");
    }

    private boolean readBoolean() throws IOException {
        if (code[position] != ChunkCode.Boolean)
            throw new IOException("not boolean");
        position++;
        return code[position++] != 0;
    }

    private double readDouble() throws IOException {
        if (code[position] != ChunkCode.Number)
            throw new IOException("not double");
        position++;
        int a = code[position++];
        int b = code[position++];
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(a);
        bb.putInt(b);
        return bb.getDouble(0);
    }

    private LanguageEnum readEnum() throws IOException {
        if (code[position] != ChunkCode.Enum)
            throw new IOException("not enum");
        position++;
        String name = readString();
        int len = code[position++];
        Map<String, LanguageEnumChild> map = new HashMap<>();
        for (int j = 0; j < len; j++) {
            String childName = readString();
            LanguageEnumChild child = readEnumChild();
            map.put(childName, child);
        }
        return new LanguageEnum(name, map);
    }

    private LanguageEnumChild readEnumChild() throws IOException {
        if (code[position] != ChunkCode.EnumChild)
            throw new IOException("not enum child");
        position++;
        int val = code[position++];
        int propsSize = code[position++];
        List<String> props = new ArrayList<>();
        for (int j = 0; j < propsSize; j++) {
            props.add(readString());
        }
        return new LanguageEnumChild(val, props);
    }

    private ByteCode readFunc() throws IOException {
        if (code[position] != ChunkCode.Func)
            throw new IOException("not func");

        position++;
        int arity = code[position++];
        int totalArity = code[position++];

        String name = null;
        if (code[position] == 0) position++;
        else name = readString();

        int upvalueCount = code[position++];

        int operation = code[position++];

        boolean async = (operation & ASYNC) != 0;
        boolean catcher = (operation & CATCHER) != 0;
        boolean varargs = (operation & VARARGS) != 0;
        boolean kwargs = (operation & KW_ARGS) != 0;

        Chunk chunk = readChunk();

        ByteCode func = new ByteCode(chunk.source);
        func.name = name;
        func.arity = arity;
        func.totarity = totalArity;
        func.upvalueCount = upvalueCount;
        func.async = async;
        func.catcher = catcher;
        func.varargs = varargs;
        func.kwargs = kwargs;
        func.chunk = chunk;
        return func;
    }

    private Value readValue() throws IOException {
        switch (code[position]) {
            case ChunkCode.Boolean:
                return new Value(readBoolean());
            case ChunkCode.Number:
                return new Value(readDouble());
            case ChunkCode.String:
                return new Value(readString());
            case ChunkCode.Enum:
                return new Value(readEnum());
            case ChunkCode.Func:
                return new Value(readFunc());
            default:
                return null;
        }
    }

    private Chunk readChunk() throws IOException {
        if (code[position] != ChunkCode.Chunk)
            throw new IOException("not chunk");
        position++;

        String source = readString();

        String packageName = null;
        if (code[position] == 0) {
            position++;
        } else {
            packageName = readString();
        }

        String target = null;
        if (code[position] == 0) {
            position++;
        } else {
            target = readString();
        }

        int constantCount = code[position++];
        Value[] constants = new Value[constantCount];
        for (int j = 0; j < constantCount; j++) {
            constants[j] = readValue();
        }
        ValueArray values = new ValueArray(constants);

        Map<String, Type> globals = reader.readAttributes();

        int bytecodeCount = code[position++];
        int[] bytecodes = new int[bytecodeCount];
        for (int j = 0; j < bytecodeCount; j++) {
            bytecodes[j] = code[position++];
        }

        Chunk chunk = new Chunk(source);
        chunk.packageName = packageName;
        chunk.target = target;
        chunk.codeArray = bytecodes;
        List<Integer> code = new ArrayList<>();
        for (int j = 0; j < bytecodes.length; j++) {
            code.add(bytecodes[j]);
        }
        chunk.code = code;
        chunk.constants = values;
        chunk.globals = globals;
        return chunk;
    }

    public static ByteCode build(byte[] code) {
        try {
            return new ChunkBuilder(code).readFunc();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
