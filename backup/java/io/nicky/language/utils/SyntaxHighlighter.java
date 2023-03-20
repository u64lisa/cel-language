package language.utils;

public class SyntaxHighlighter {


    private SyntaxHighlighter() {
    }

    public static int leftPadding(String str) {
        int tabs = 0;
        while (tabs < str.length() && Character.isWhitespace(str.charAt(tabs))) {
            tabs++;
        }
        return tabs;
    }

    public static String highlightFlat(String source, int index, int len) {
        StringBuilder sb = new StringBuilder();

        String[] lines = source.split("\n");

        int line = 0;
        while (index >= lines[line].length() && line < lines.length - 1) {
            index -= lines[line].length() + 1;
            line++;
        }

        int tabs = leftPadding(lines[line]);
        index -= tabs;
        len += tabs;

        while (len > 0 && line < lines.length) {
            String lineStr = lines[line];
            int lineLen = lineStr.length();

            tabs = leftPadding(lineStr);
            len -= tabs;
            lineLen -= tabs;
            String text = lineStr.substring(tabs);

            if (lineLen == 0) {
                sb.append("\n");
                line++;
                continue;
            }

            int end = Math.min(index + len, lineLen);
            String highlight;
            if (end - index >= 2) {
                highlight = repeat(end - index, "^");
            } else {
                highlight = "^";
            }

            sb.append(text)
                    .append("\n")
                    .append(String.join("", repeat(index, " ")))
                    .append(highlight)
                    .append("\n");

            len -= lineLen - index;

            line++;
            index = 0;
        }

        return sb.toString();
    }

    public static int nonWhitespace(String string) {
        char[] characters = string.toCharArray();
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isWhitespace(characters[i])) {
                return i;
            }
        }
        return 0;
    }

    public static String repeat(String str, int times) {
        return new String(new char[times]).replace("\0", str);
    }

    public static String repeat(int times, String str) {
        return new String(new char[times]).replace("\0", str);
    }

}
