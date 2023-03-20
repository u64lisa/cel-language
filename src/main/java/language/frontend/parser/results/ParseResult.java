package language.frontend.parser.results;

import dtool.logger.errors.LanguageException;

public class ParseResult<T> {
    protected T node = null;
    protected LanguageException languageException = null;
    protected int advanceCount = 0;
    protected int toReverseCount = 0;

    public void registerAdvancement() {
        advanceCount++;
    }

    public <U> U register(ParseResult<U> res) {
        advanceCount += res.advanceCount;
        if (res.languageException != null) {
            languageException = res.languageException;
        }
        return res.node;
    }

    public <U> U tryRegister(ParseResult<U> res) {
        if (res.languageException != null) {
            languageException = res.languageException;
            toReverseCount += res.advanceCount;
            return null;
        }
        return register(res);
    }

    public ParseResult<T> success(T node) {
        this.node = node;
        return this;
    }

    public ParseResult<T> failure(LanguageException languageException) {
        if (this.languageException == null || advanceCount == 0) {
            this.languageException = languageException;
        }
        return this;
    }

    public T getNode() {
        return node;
    }

    public void setNode(T node) {
        this.node = node;
    }

    public LanguageException getLanguageError() {
        return languageException;
    }

    public void setLanguageError(LanguageException languageException) {
        this.languageException = languageException;
    }

    public int getAdvanceCount() {
        return advanceCount;
    }

    public void setAdvanceCount(int advanceCount) {
        this.advanceCount = advanceCount;
    }

    public int getToReverseCount() {
        return toReverseCount;
    }

    public void setToReverseCount(int toReverseCount) {
        this.toReverseCount = toReverseCount;
    }
}
