package language.backend.compiler.llvm;

import language.backend.compiler.AbstractCompiler;
import language.frontend.parser.nodes.Node;

import java.util.List;

public class LLVMCompiler extends AbstractCompiler {

    @Override
    public byte[] compile(String source, List<Node> ast) {

        return new byte[0];
    }

}
