package language.backend.precompiler;

import language.frontend.lexer.token.Position;

import java.util.List;

public class Ir {

    public static record Instruction(Position start, Position end,
                                     List<InstructionParameter> parameters, int opcode) {}

    public static interface InstructionParameter {}


}
