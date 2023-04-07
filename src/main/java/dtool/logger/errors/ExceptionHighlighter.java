package dtool.logger.errors;

import dtool.logger.PositionedError;
import language.frontend.lexer.token.Position;

import java.util.List;

public class ExceptionHighlighter {

    private String createError(PositionedError error, String message) {
        int errorLine = error.languageException().positionEnd().line() + 1;
        int errorStart = error.languageException().positionStart().column();
        int errorEnd = error.languageException().positionEnd().column();
        int columns = Math.max(1, errorEnd - errorStart);
        int padSize = Math.max(1, (int) Math.floor(Math.log10(errorLine)) + 1);

        String numPadding = " ".repeat(padSize);
        String numFormat = "%" + padSize + "d";
        String errPadding = " ".repeat(errorStart);

        StringBuilder details = new StringBuilder();
        for (String detail : error.languageException().details()) {
            details.append(detail);
        }

        StringBuilder sb = new StringBuilder();

        if (error.source() != null) {
            List<String> lines = error.source().lines().toList();
            String errString = lines.get(errorLine - 1);

            sb.append('\n');
            sb.append("     %s\n".formatted(details));
            sb.append("%s\n".formatted("-".repeat(70)));
            sb.append("%s | \n".formatted(numPadding));
            sb.append("%s | %s\n".formatted(numFormat.formatted(errorLine), errString));
            sb.append("%s | %s%s %s\n".formatted(numPadding, errPadding, "^".repeat(columns), message));
            sb.append("%s | \n".formatted(numPadding));
        } else {
            sb.append('\n');
            sb.append("%s | %s".formatted(numFormat.formatted(errorLine), message));
        }

        return sb.toString();
    }

    public String createFullError(PositionedError error) {
        StringBuilder sb = new StringBuilder();

        String[] fileSplit = error.file().split("/\\./");
        String fileSegment = fileSplit.length > 1 ? fileSplit[1] : error.file();

        Position position = error.languageException().positionStart();
        sb.append(" --> (").append(fileSegment).append(") (line: ").append(position.line() + 1)
                .append(", column: ").append(position.column() + 1).append(") ")
                .append(createError(error, error.languageException().errorName()));

        return sb.toString();
    }

}
