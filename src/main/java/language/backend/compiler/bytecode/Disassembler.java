package language.backend.compiler.bytecode;

import language.backend.compiler.bytecode.values.bytecode.ByteCode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Disassembler {
    private File outputFile = Paths.get("./output.txt").toFile();

    private final List<String> instructions = new ArrayList<>();

    public Disassembler(final String id) {
        this.outputFile = Paths.get("./output-" + id + ".txt").toFile();
    }

    public void disassembleChunk(Chunk chunk, String string) {
        writeElement(String.format("== %s ==%n", string));
        int offset = 0;

        while (offset < chunk.code.size()) {
            offset = disassembleInstruction(chunk, offset);
        }
    }

    public int disassembleInstruction(Chunk chunk, int offset) {
        writeElement(String.format("%04d ", offset));

        int instruction = chunk.code.get(offset);
        switch (instruction) {
            case OpCode.Return -> {
                return simpleInstruction("OP_RETURN", offset);
            }
            case OpCode.Pop -> {
                return simpleInstruction("OP_POP", offset);
            }
            case OpCode.PatternVars -> {
                return constantInstruction("OP_PATTERN_VARS", chunk, offset);
            }
            case OpCode.Pattern -> {
                int args = chunk.code.get(offset + 1);
                writeElement(String.format("%-16s %04d%n", "OP_PATTERN", args));
                return offset + 2 + args;
            }
            case OpCode.Header -> {
                int constant = chunk.code.get(offset + 1);
                int args = chunk.code.get(offset + 2);
                writeElement(String.format("%-16s %04d %04d%n", "OP_HEADER", constant, args));
                return offset + 3 + args;
            }
            case OpCode.Destruct -> {
                int count = chunk.code.get(offset + 1);
                if (count == -1) {
                    writeElement(String.format("%-16s %-16s%n", "OP_DESTRUCT", "GLOB"));
                    return offset + 2;
                }
                writeElement(String.format("%-16s %04d%n", "OP_DESTRUCT", count));
                return offset + 2 + count;
            }
            case OpCode.Constant -> {
                return constantInstruction("OP_CONSTANT", chunk, offset);
            }
            case OpCode.SetGlobal -> {
                return constantInstruction("OP_SET_GLOBAL", chunk, offset);
            }
            case OpCode.GetGlobal -> {
                return constantInstruction("OP_GET_GLOBAL", chunk, offset);
            }
            case OpCode.DefineGlobal -> {
                return declInstruction("OP_DEFINE_GLOBAL", chunk, offset, false);
            }
            case OpCode.Add -> {
                return simpleInstruction("OP_ADD", offset);
            }
            case OpCode.Subtract -> {
                return simpleInstruction("OP_SUBTRACT", offset);
            }
            case OpCode.Multiply -> {
                return simpleInstruction("OP_MULTIPLY", offset);
            }
            case OpCode.Divide -> {
                return simpleInstruction("OP_DIVIDE", offset);
            }
            case OpCode.Power -> {
                return simpleInstruction("OP_POWER", offset);
            }
            case OpCode.Modulo -> {
                return simpleInstruction("OP_MODULO", offset);
            }
            case OpCode.Negate -> {
                return simpleInstruction("OP_NEGATE", offset);
            }
            case OpCode.Not -> {
                return simpleInstruction("OP_NOT", offset);
            }
            case OpCode.Increment -> {
                return simpleInstruction("OP_INCREMENT", offset);
            }
            case OpCode.Decrement -> {
                return simpleInstruction("OP_DECREMENT", offset);
            }
            case OpCode.EQUAL -> {
                return simpleInstruction("OP_EQUAL", offset);
            }
            case OpCode.GreaterThan -> {
                return simpleInstruction("OP_GREATER_THAN", offset);
            }
            case OpCode.LessThan -> {
                return simpleInstruction("OP_LESS_THAN", offset);
            }
            case OpCode.SetLocal -> {
                return byteInstruction("OP_SET_LOCAL", chunk, offset);
            }
            case OpCode.GetLocal -> {
                return byteInstruction("OP_GET_LOCAL", chunk, offset);
            }
            case OpCode.DefineLocal -> {
                return declInstruction("OP_DEFINE_LOCAL", chunk, offset, true);
            }
            case OpCode.MakeVar -> {
                int arg = chunk.code.get(offset + 1);
                String constant = chunk.code.get(offset + 2) == 1 ? "CONSTANT" : "MUTABLE";
                writeElement(String.format("%-16s %-16s %04d%n", constant, "OP_MAKE_VAR", arg));
                return offset + 3;
            }
            case OpCode.Throw -> {
                return simpleInstruction("OP_THROW", offset);
            }
            case OpCode.Assert -> {
                return simpleInstruction("OP_ASSERT", offset);
            }
            case OpCode.Copy -> {
                return simpleInstruction("OP_COPY", offset);
            }
            case OpCode.MakeArray -> {
                return byteInstruction("OP_MAKE_ARRAY", chunk, offset);
            }
            case OpCode.MakeMap -> {
                return byteInstruction("OP_MAKE_MAP", chunk, offset);
            }
            case OpCode.Enum -> {
                int constant = chunk.code.get(offset + 1);
                int isPublic = chunk.code.get(offset + 2);
                writeElement(String.format("%-16s %-16s %-16s %n", "OP_ENUM",
                        isPublic == 1 ? "PUBLIC" : "", chunk.constants.values.get(constant)));
                return offset + 3;
            }
            case OpCode.GetUpvalue -> {
                return byteInstruction("OP_GET_UPVALUE", chunk, offset);
            }
            case OpCode.SetUpvalue -> {
                return byteInstruction("OP_SET_UPVALUE", chunk, offset);
            }
            case OpCode.FromBytes -> {
                return simpleInstruction("OP_FROM_BYTES", offset);
            }
            case OpCode.ToBytes -> {
                return simpleInstruction("OP_TO_BYTES", offset);
            }
            case OpCode.Deref -> {
                return simpleInstruction("OP_DEREF", offset);
            }
            case OpCode.Ref -> {
                return simpleInstruction("OP_REF", offset);
            }
            case OpCode.SetRef -> {
                return simpleInstruction("OP_SET_REF", offset);
            }
            case OpCode.Jump -> {
                return jumpInstruction("OP_JUMP", 1, chunk, offset);
            }
            case OpCode.JumpIfFalse -> {
                return jumpInstruction("OP_JUMP_IF_FALSE", 1, chunk, offset);
            }
            case OpCode.JumpIfTrue -> {
                return jumpInstruction("OP_JUMP_IF_TRUE", 1, chunk, offset);
            }
            case OpCode.Loop -> {
                return jumpInstruction("OP_LOOP", -1, chunk, offset);
            }
            case OpCode.Method -> {
                constantInstruction("OP_METHOD", chunk, offset);
                return offset + 5;
            }
            case OpCode.Access -> {
                return constantInstruction("OP_ACCESS", chunk, offset);
            }
            case OpCode.StartCache -> {
                return simpleInstruction("OP_START_CACHE", offset);
            }
            case OpCode.CollectLoop -> {
                return simpleInstruction("OP_COLLECT_LOOP", offset);
            }
            case OpCode.FlushLoop -> {
                return simpleInstruction("OP_FLUSH_LOOP", offset);
            }
            case OpCode.For -> {
                return forInstruction(chunk, offset);
            }
            case OpCode.Class -> {
                int end = constantInstruction("OP_CLASS", chunk, offset);
                int attributeCount = chunk.code.get(offset + 2);
                // CONSTANT ISSTATIC ISPRIVATE TYPE
                int totalAttributeOffset = 1 + attributeCount * 3;
                return end + totalAttributeOffset;
            }
            case OpCode.Call -> {
                int argCount = chunk.code.get(offset + 1);
                int kwargCount = chunk.code.get(offset + 2);
                writeElement(String.format("%-16s %04d %04d%n", "OP_CALL", argCount, kwargCount));
                return offset + 3 + kwargCount;
            }
            case OpCode.Closure -> {
                offset++;
                int constant = chunk.code.get(offset++);
                int defaults = chunk.code.get(offset++);
                writeElement(String.format("%-16s %04d %04d ", "OP_CLOSURE", constant, defaults));
                writeElement(chunk.constants.values.get(constant));
                writeElement("\n");

                ByteCode func = chunk.constants.values.get(constant).asFunc();
                for (int i = 0; i < func.upvalueCount; i++) {
                    int isLocal = chunk.code.get(offset++);
                    int index = chunk.code.get(offset++);
                    writeElement(String.format("%04d      |                     %s %d\n",
                            offset - 2, isLocal == 1 ? "local" : isLocal == 2 ? "global" : "upvalue", index));
                }

                return offset;
            }
            case OpCode.Null -> {
                return simpleInstruction("OP_NULL", offset);
            }
            case OpCode.SetAttr -> {
                return byteInstruction("OP_SET_ATTR", chunk, offset);
            }
            case OpCode.GetAttr -> {
                return byteInstruction("OP_GET_ATTR", chunk, offset);
            }
            case OpCode.Import -> {
                int fromConstant = chunk.code.get(offset + 1);
                int asConstant = chunk.code.get(offset + 2);
                writeElement(String.format("%-16s %-16s as %-16s%n", "OP_IMPORT", chunk.constants.values.get(fromConstant), chunk.constants.values.get(asConstant)));
                return offset + 3;
            }
            case OpCode.BitAnd -> {
                return simpleInstruction("OP_BIT_AND", offset);
            }
            case OpCode.BitOr -> {
                return simpleInstruction("OP_BIT_OR", offset);
            }
            case OpCode.BitXor -> {
                return simpleInstruction("OP_BIT_XOR", offset);
            }
            case OpCode.BitCompl -> {
                return simpleInstruction("OP_BIT_NOT", offset);
            }
            case OpCode.LeftShift -> {
                return simpleInstruction("OP_BIT_SHIFT_LEFT", offset);
            }
            case OpCode.RightShift -> {
                return simpleInstruction("OP_BIT_SHIFT_RIGHT", offset);
            }
            case OpCode.SignRightShift -> {
                return simpleInstruction("OP_BIT_SHIFT_RIGHT_SIGNED", offset);
            }
            case OpCode.DropGlobal -> {
                return constantInstruction("OP_DROP_GLOBAL", chunk, offset);
            }
            case OpCode.DropLocal -> {
                return byteInstruction("OP_DROP_LOCAL", chunk, offset);
            }
            case OpCode.DropUpvalue -> {
                return byteInstruction("OP_DROP_UPVALUE", chunk, offset);
            }
            case OpCode.Spread -> {
                return simpleInstruction("OP_SPREAD", offset);
            }
            case OpCode.Get -> {
                return simpleInstruction("OP_GET", offset);
            }
            case OpCode.Index -> {
                return simpleInstruction("OP_INDEX", offset);
            }
            case OpCode.Iter -> {
                int slot = chunk.code.get(offset + 1);
                int slot2 = chunk.code.get(offset + 2);
                int jump = chunk.code.get(offset + 3);

                writeElement(String.format("%-16s %04d %04d %04d -> %04d\n", "OP_ITER", slot, slot2, offset, offset + jump));

                return offset + 4;
            }
            case OpCode.Chain -> {
                return simpleInstruction("OP_CHAIN", offset);
            }
            default -> {
                writeElement(String.format("Unknown opcode %d%n", instruction));
                return offset + 1;
            }
        }
    }
    
    int simpleInstruction(String name, int offset) {
        writeElement(String.format("%-16s%n", name));
        return offset + 1;
    }
    
    int constantInstruction(String name, Chunk chunk, int offset) {
        int constant = chunk.code.get(offset + 1);
        writeElement(String.format("%-16s %04d '", name, constant));
        writeElement(chunk.constants.values.get(constant));
        writeElement("'\n");
        return offset + 2;
    }
    
    int byteInstruction(String name, Chunk chunk, int offset) {
        int local = chunk.code.get(offset + 1);
        writeElement(String.format("%-16s %04d%n", name, local));
        return offset + 2;
    }

    int declInstruction(String name, Chunk chunk, int offset, boolean isLocal) {
        int arg = !isLocal ? chunk.code.get(offset + 1) : 0;
        int localOffset = !isLocal ? 1 : 0;
        String constant = chunk.code.get(offset + 1 + localOffset) == 1 ? "CONSTANT" : "MUTABLE";
        boolean hasRange = chunk.code.get(offset + 2 + localOffset) == 1;

        if (isLocal)
            writeElement(String.format("%-16s %-16s%n", constant, name));
        else
            writeElement(String.format("%-16s %-16s %04d '%s'%n", constant, name, arg,
                    chunk.constants.values.get(arg)));
        return offset + 3 + localOffset + (hasRange ? 2 : 0);
    }
    
    int jumpInstruction(String name, int sign, Chunk chunk, int offset) {
        int jump = sign * chunk.code.get(offset + 1);

        writeElement(String.format("%-16s %04d -> %04d%n", name, offset, offset + 2 + jump));
        
        return offset + 2;
    }

    int forInstruction(Chunk chunk, int offset) {
        int constant = chunk.code.get(offset + 1);
        int jump = chunk.code.get(offset + 2);

        writeElement(String.format("%-16s %04d %04d -> %04d%n", "OP_FOR", constant, offset + 3, offset + 3 + jump));
        return offset + 3;
    }

    void writeElement(final Object raw) {
        writeElement(String.valueOf(raw));
    }

    void writeElement(final String message) {
        instructions.add(message);
    }

    public void finish() {
        //Collections.reverse(instructions);

        final StringBuilder content = new StringBuilder();
        for (String instruction : instructions) {
            content.append(instruction);//.append('\n');
        }

        final String instructionList = content.toString();
        final String[] elements = instructionList.split("\n");
        final String[] flipped = new String[elements.length];

        int index = 0;
        for (int i = flipped.length - 1; i >= 0; i--) {
            flipped[index] = elements[i];
            index++;
        }

        final StringBuilder finalized = new StringBuilder();
        for (String s : flipped) {
            finalized.append(s).append("\n");
        }

        boolean deleted = outputFile.delete();

        if (deleted) {
            try {
                final FileWriter fileWriter = new FileWriter(outputFile);
                fileWriter.write(finalized.toString());
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.instructions.clear();
    }
}
