package language.frontend.parser.units;

import language.frontend.parser.results.ParseResult;

public interface Executable<T> {
    ParseResult<T> execute();
}
