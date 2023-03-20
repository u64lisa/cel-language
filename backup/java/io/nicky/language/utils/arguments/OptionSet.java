package language.utils.arguments;

import java.util.Arrays;
import java.util.List;

public final class OptionSet implements IOptionSet {

    private final List<Option> options;

    public OptionSet(List<Option> options) {
        this.options = options;
    }

    @Override
    public boolean isPresent(final String name) {
        final Option option = this.resolve(name);

        if (option == null)
            return false;

        return option.isPresent();
    }

    @Override
    public Option getOption(final String name) {
        final Option option = this.resolve(name);

        if (option == null)
            throw new RuntimeException("Option is null!");

        return option;
    }

    @Override
    public String getValueOfObject(final String name) {
        final Option option = this.resolve(name);

        if (option  == null)
            throw new RuntimeException("Option is null!");

        return option.value();
    }

    @Override
    public boolean getState(final String name) {
        final Option option = this.resolve(name);

        if (option  == null)
            throw new RuntimeException("Option is null!");

        return option.isPresent();
    }

    private Option resolve(final String name) {
        return options.stream()
                .filter(current -> current.name().equalsIgnoreCase(name) ||
                        Arrays.stream(current.getAliases()).anyMatch(s -> s.equalsIgnoreCase(name)))
                .findFirst().orElse(null);
    }

    public List<Option> getOptions() {
        return options;
    }
}
