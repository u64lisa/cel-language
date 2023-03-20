package language.frontend.parser.units;

import language.frontend.lexer.token.Token;

import java.util.List;

public class EnumChild {
    Token token;
    List<String> params;
    List<List<String>> types;
    List<String> generics;

    public EnumChild(Token token, List<String> params, List<List<String>> types, List<String> generics) {
        this.token = token;
        this.params = params;
        this.types = types;
        this.generics = generics;
    }

    public List<List<String>> types() {
        return types;
    }

    public List<String> params() {
        return params;
    }

    public List<String> generics() {
        return generics;
    }

    public Token token() {
        return token;
    }
}
