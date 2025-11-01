import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuturisVerifier {
    // Collect outputs here to control final formatting exactly
    private static final List<String> OUTPUTS = new ArrayList<>();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<String> all = new ArrayList<>();
        while (sc.hasNextLine()) all.add(sc.nextLine());
        sc.close();

        int last = all.size() - 1;
        while (last >= 0 && all.get(last).trim().isEmpty()) last--;
        if (last < 0) return;
        int secondLast = last - 1;
        while (secondLast >= 0 && all.get(secondLast).trim().isEmpty()) secondLast--;
        if (secondLast < 0) return;

        List<String> codeLines = new ArrayList<>();
        for (int i = 0; i < secondLast; i++) codeLines.add(all.get(i));

        String varsLine = all.get(secondLast).trim();
        String valsLine = all.get(last).trim();

        Map<String, Integer> varMap = new HashMap<>();
        if (!varsLine.isEmpty()) {
            String[] vars = varsLine.split("\\s+");
            String[] vals = valsLine.isEmpty() ? new String[0] : valsLine.split("\\s+");
            for (int i = 0; i < vars.length && i < vals.length; i++) {
                try { varMap.put(vars[i], Integer.parseInt(vals[i])); }
                catch (NumberFormatException e) { varMap.put(vars[i], 0); }
            }
        }

        executeBlock(codeLines, 0, codeLines.size(), varMap);

        // Print outputs with a blank line between each output (match sample formatting).
        if (!OUTPUTS.isEmpty()) {
            System.out.print(String.join("\n", OUTPUTS));
        }
    }

    private static void executeBlock(List<String> lines, int start, int endExclusive, Map<String, Integer> varMap) {
        int idx = start;
        while (idx < endExclusive) {
            String raw = lines.get(idx);
            String line = raw.trim();
            if (line.isEmpty()) { idx++; continue; }

            if (line.startsWith("if")) {
                idx = handleIf(lines, idx, endExclusive, varMap);
            } else if (line.startsWith("for")) {
                idx = handleFor(lines, idx, endExclusive, varMap);
            } else if (line.startsWith("print")) {
                doPrint(line, varMap);
                idx++;
            } else if (line.equals("end")) {
                idx++;
            } else {
                idx++;
            }
        }
    }

    private static int handleIf(List<String> lines, int idx, int endExclusive, Map<String, Integer> varMap) {
        String header = lines.get(idx).trim();
        String condition = header.length() > 2 ? header.substring(2).trim() : "";
        boolean condValue = evalCondition(condition, varMap);

        int p = idx + 1;
        while (p < endExclusive && lines.get(p).trim().isEmpty()) p++;
        if (p >= endExclusive || !lines.get(p).trim().equals("Yes")) return skipToMatchingEnd(lines, idx, endExclusive);
        int yesStart = p + 1;

        int depth = 1;
        int noIndex = -1;
        int matchingEnd = -1;
        p = yesStart;
        while (p < endExclusive) {
            String t = lines.get(p).trim();
            if (t.startsWith("if") || t.startsWith("for")) depth++;
            else if (t.equals("end")) {
                depth--;
                if (depth == 0) { matchingEnd = p; break; }
            } else if (t.equals("No") && depth == 1) { noIndex = p; }
            p++;
        }
        if (matchingEnd == -1) return endExclusive;

        if (noIndex == -1) {
            if (condValue) executeBlock(lines, yesStart, matchingEnd, varMap);
        } else {
            if (condValue) executeBlock(lines, yesStart, noIndex, varMap);
            else executeBlock(lines, noIndex + 1, matchingEnd, varMap);
        }
        return matchingEnd + 1;
    }

    private static int handleFor(List<String> lines, int idx, int endExclusive, Map<String, Integer> varMap) {
        String header = lines.get(idx).trim();
        String rest = header.length() > 3 ? header.substring(3).trim() : "";
        String[] parts = rest.split("\\s+");
        if (parts.length < 3) return skipToMatchingEnd(lines, idx, endExclusive);
        String iterVar = parts[0];
        int startVal = evalToken(parts[1], varMap);
        int endVal = evalToken(parts[2], varMap);

        int p = idx + 1;
        int depth = 1;
        int matchingEnd = -1;
        while (p < endExclusive) {
            String t = lines.get(p).trim();
            if (t.startsWith("for") || t.startsWith("if")) depth++;
            else if (t.equals("end")) {
                depth--;
                if (depth == 0) { matchingEnd = p; break; }
            }
            p++;
        }
        if (matchingEnd == -1) return endExclusive;

        for (int v = startVal; v <= endVal; v++) {
            varMap.put(iterVar, v);
            executeBlock(lines, idx + 1, matchingEnd, varMap);
        }
        return matchingEnd + 1;
    }

    private static void doPrint(String line, Map<String, Integer> varMap) {
        String arg = line.length() > 5 ? line.substring(5).trim() : "";
        if (arg.isEmpty()) {
            // Do NOT emit anything for malformed/empty print
            return;
        }
        if (arg.matches("-?\\d+")) {
            OUTPUTS.add(String.valueOf(Integer.parseInt(arg)));
        } else {
            Integer v = varMap.get(arg);
            OUTPUTS.add(String.valueOf(v == null ? 0 : v));
        }
    }

    private static int skipToMatchingEnd(List<String> lines, int idx, int endExclusive) {
        int p = idx + 1;
        int depth = 1;
        while (p < endExclusive && depth > 0) {
            String t = lines.get(p).trim();
            if (t.startsWith("if") || t.startsWith("for")) depth++;
            else if (t.equals("end")) depth--;
            p++;
        }
        return p;
    }

    private static boolean evalCondition(String cond, Map<String, Integer> varMap) {
        if (cond == null) return false;
        cond = cond.trim();
        if (cond.isEmpty()) return false;
        Pattern opPat = Pattern.compile("(==|!=|<|>)");
        Matcher m = opPat.matcher(cond);
        if (!m.find()) return false;
        String op = m.group(1);
        int idx = m.start(1);
        String left = cond.substring(0, idx).trim();
        String right = cond.substring(idx + op.length()).trim();
        int lv = evalToken(left, varMap);
        int rv = evalToken(right, varMap);
        switch (op) {
            case "==": return lv == rv;
            case "!=": return lv != rv;
            case "<":  return lv < rv;
            case ">":  return lv > rv;
            default:   return false;
        }
    }

    private static int evalToken(String tok, Map<String, Integer> varMap) {
        if (tok == null) return 0;
        tok = tok.trim();
        if (tok.matches("-?\\d+")) {
            try { return Integer.parseInt(tok); } catch (NumberFormatException e) { return 0; }
        } else {
            Integer v = varMap.get(tok);
            return v == null ? 0 : v;
        }
    }
}
