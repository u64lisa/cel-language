package language.backend.compiler;

import language.backend.compiler.bytecode.values.bytecode.ByteCode;
import language.frontend.parser.nodes.Node;

import java.util.List;

public abstract class AbstractCompiler {

    public abstract byte[] compile(final String source, List<Node> ast);

    public ByteCode getByteCode() {
        return null;
    }

}
