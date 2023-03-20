package language.utils.arguments;

import java.util.Arrays;
import java.util.List;

public final class OptionParser implements IOptionParser {

    private final String[] arguments;

    private final List<Option> global;

    public OptionParser(String[] arguments, Option... global) {
        this.arguments = arguments;
        this.global = List.of(global);
    }

    @Override
    public List<Option> parse() {
        for (String option : arguments) {
            if (!option.startsWith("--"))
                throw new RuntimeException("Please use argument prefix: \"--\"");

            final String[] split = option.replace("--", "").split("=");
            String value = split[1];
            final String optionName = split[0];

            final Option resolved = global.stream()
                    .filter(current -> current.name().equalsIgnoreCase(optionName) ||
                            Arrays.stream(current.getAliases()).anyMatch(s -> s.equalsIgnoreCase(optionName)))
                    .findFirst().orElse(null);

            if (resolved == null)
                throw new RuntimeException("Option not found: \"" + optionName + "\"!");


            if (option.contains("=")) {
                resolved.setPresent(true);
                continue;
            }

            if (resolved.isPresent())
                throw new RuntimeException("Cant set value of: \"" + optionName + "\" duplicated!");

            resolved.setElement(value);
        }
        return global;
    }

}
