package language.utils.arguments;

import language.utils.PassThrough;

public final class Option implements IOption {

    public static final PassThrough<String> BOOLEAN_PASS = current ->
    {
        if (current.equalsIgnoreCase("true") || current.equalsIgnoreCase("false")) {
            return current;
        }
        throw new RuntimeException("Value is so supposed to be \"true\" or \"false\" but got: " + current);
    };

    private boolean present;
    private boolean argument;
    private final String name;
    private final String[] aliases;

    private final PassThrough<String> optionCheck;
    private final String defaultValue;
    private String value;

    public Option(boolean argument, String name, String[] aliases, PassThrough<String> optionCheck, String defaultValue) {
        this.argument = argument;
        this.aliases = aliases;
        this.optionCheck = optionCheck;
        this.defaultValue = defaultValue;
        this.present = false;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        if (value == null)
            value = defaultValue;

        return optionCheck.pass(value);
    }

    public boolean isArgument() {
        return argument;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public String[] getAliases() {
        return aliases;
    }

    public void setElement(final String value) {
        if (value == null)
            throw new RuntimeException("Parameter for argument: \"" + name + "\" is not present");

        this.value = value;
        this.present = true;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    @Override
    public String toString() {
        return "ValueOption{" +
                "present=" + present +
                ", name='" + name + '\'' +
                ", defaultValue=" + defaultValue +
                ", value=" + value +
                '}';
    }

    public PassThrough<String> getOptionCheck() {
        return optionCheck;
    }

    public static final class Builder {

        private String defaultValue = null;
        private PassThrough<String> passThrough = current -> current;
        private String name = "no name present";
        private String[] aliases = {};
        private boolean argument;

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setAliases(final String... aliases) {
            this.aliases = aliases;
            return this;
        }

        public Builder setDefault(final String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder setPassThrough(final PassThrough<String> passThrough) {
            this.passThrough = passThrough;
            return this;
        }

        public Builder setArgument() {
            this.argument = true;
            return this;
        }

        public Option build() {
            if (name == null)
                throw new RuntimeException("Option name can't be null!");

            return new Option(argument, name, aliases, passThrough, defaultValue);
        }

    }
}
