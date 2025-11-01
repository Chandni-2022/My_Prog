import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = 0;
        if (scanner.hasNextInt()) {
            n = scanner.nextInt();
            scanner.nextLine(); // consume newline
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < n && scanner.hasNextLine(); i++) {
            text.append(scanner.nextLine()).append("\n");
        }
        String commandLine = "";
        if (scanner.hasNextLine()) {
            commandLine = scanner.nextLine().trim();
        }
        scanner.close();
        String[] command = commandLine.isEmpty() ? new String[0] : commandLine.split("\\s+");
        FormatOptions options = parseCommand(command);
        String formattedText = formatText(text.toString(), options);
        System.out.print(formattedText);
    }

    private static FormatOptions parseCommand(String[] command) {
        FormatOptions options = new FormatOptions();
        for (int i = 0; i < command.length; i++) {
            String tok = command[i];
            try {
                if (tok.equals("-w")) {
                    if (i + 1 < command.length && !command[i + 1].startsWith("-")) {
                        int w = Integer.parseInt(command[++i]);
                        options.evenWidth = options.oddWidth = options.defaultWidth = Math.max(1, w);
                    }
                } else if (tok.equals("-w-e")) {
                    if (i + 1 < command.length) {
                        options.evenWidth = Math.max(1, Integer.parseInt(command[++i]));
                    }
                } else if (tok.equals("-w-o")) {
                    if (i + 1 < command.length) {
                        options.oddWidth = Math.max(1, Integer.parseInt(command[++i]));
                    }
                } else if (tok.equals("h")) {
                    options.hyphenation = true;
                }
            } catch (NumberFormatException ex) {
                // ignore malformed numbers
            }
        }
        return options;
    }

    private static String formatText(String text, FormatOptions options) {
        String[] paragraphs = text.split("\n\n");
        StringBuilder formattedText = new StringBuilder();
        for (int i = 0; i < paragraphs.length; i++) {
            formattedText.append(formatParagraph(paragraphs[i].trim(), options));
            if (i < paragraphs.length - 1) {
                formattedText.append("\n\n");
            }
        }
        return formattedText.toString();
    }

    private static String formatParagraph(String paragraph, FormatOptions options) {
        String[] words = paragraph.split("\\s+");
        StringBuilder out = new StringBuilder();
        int lineNumber = 0;
        StringBuilder line = new StringBuilder();
        int width = options.getWidth(lineNumber);
        for (String word : words) {
            if (word.matches("^(\\d+\\.|\\*|-)\\s*.*")) {
                if (line.length() > 0) {
                    out.append("\n");
                    out.append(line.toString());
                    line.setLength(0);
                    lineNumber++;
                    width = options.getWidth(lineNumber);
                }
                out.append("\n");
                out.append(word);
                lineNumber++;
                line.setLength(0);
                width = options.getWidth(lineNumber);
                continue;
            }
            if (isUrlOrEmail(word)) {
                if (line.length() > 0 && line.length() + word.length() + 1 > width) {
                    out.append("\n");
                    out.append(line.toString());
                    line.setLength(0);
                    lineNumber++;
                    width = options.getWidth(lineNumber);
                }
                if (line.length() > 0) {
                    line.append(" ");
                }
                line.append(word);
            } else {
                if (line.length() == 0) {
                    line.append(word);
                } else if (line.length() + word.length() + 1 <= width) {
                    line.append(" ").append(word);
                } else {
                    out.append("\n");
                    out.append(line.toString());
                    line.setLength(0);
                    lineNumber++;
                    width = options.getWidth(lineNumber);
                    line.append(word);
                }
            }
            if (line.length() >= width) {
                if (options.hyphenation && line.length() > width) {
                    String chunk = line.substring(0, width - 1);
                    out.append("\n");
                    out.append(chunk).append("-");
                    line.setLength(0);
                    line.append(line.substring(width - 1));
                } else {
                    out.append("\n");
                    out.append(line.toString());
                    line.setLength(0);
                }
                lineNumber++;
                width = options.getWidth(lineNumber);
            }
        }
        if (line.length() > 0) {
            out.append("\n");
            out.append(line.toString());
        }
        return out.toString().trim();
    }

    private static boolean isUrlOrEmail(String word) {
        return word.contains("@") || word.startsWith("http://") || word.startsWith("https://");
    }

    private static class FormatOptions {
        int defaultWidth = 75;
        int evenWidth = 75;
        int oddWidth = 75;
        boolean hyphenation = false;

        int getWidth(int lineNumber) {
            return lineNumber % 2 == 0 ? evenWidth : oddWidth;
        }
    }
}