/* This font is licensed under CC0 1.0
(https://creativecommons.org/publicdomain/zero/1.0).
You can use, modify and distribute it however you want,
including commercially, and without including this license. */

package utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Json {
    public static Object parse(String json) {
        Object[] stack = new Object[16];
        int stackIndex = -1;
        Object[] keys = new Object[16];
        boolean[] foundKeys = new boolean[16];
        int keysIndex = -1;
        char[] chars = json.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            Object object = null;
            boolean foundObject = false;
            if (i + 3 < chars.length && chars[i] == 'n' && chars[i + 1] == 'u' && chars[i + 2] == 'l' && chars[i + 3] == 'l' || i + 8 < chars.length && chars[i] == 'u' && chars[i + 1] == 'n' && chars[i + 2] == 'd' && chars[i + 3] == 'e' && chars[i + 4] == 'f' && chars[i + 5] == 'i' && chars[i + 6] == 'n' && chars[i + 7] == 'e' && chars[i + 8] == 'd') {
                foundObject = true;
                i += chars[i] == 'n' ? 3 : 8;
            }
            else if (i + 3 < chars.length && chars[i] == 't' && chars[i + 1] == 'r' && chars[i + 2] == 'u' && chars[i + 3] == 'e') {
                object = true;
                foundObject = true;
                i += 3;
            }
            else if (i + 4 < chars.length && chars[i] == 'f' && chars[i + 1] == 'a' && chars[i + 2] == 'l' && chars[i + 3] == 's' && chars[i + 4] == 'e') {
                object = false;
                foundObject = true;
                i += 4;
            }
            else if (chars[i] == '"' || chars[i] == '\'') {
                char strChar = chars[i];
                int start = ++i;
                int length = 0;
                while (i < chars.length && chars[i] != strChar) {
                    length++;
                    if (chars[i++] == '\\') {
                        i++;
                    }
                }
                char[] string = new char[length];
                int pos = 0;
                for (int j = start; j < i; j++) {
                    string[pos++] = chars[j] == '\\' && j + 1 < i ? switch (chars[++j]) {
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 's' -> '\s';
                        case 't' -> '\t';
                        default -> chars[j];
                    } : chars[j];
                }
                object = new String(string);
                foundObject = true;
            }
            else if (chars[i] == '.' || chars[i] == '+' || chars[i] == '-' || (chars[i] >= '0' && chars[i] <= '9')) {
                int negative = chars[i] == '-' ? -1 : 1;
                if (chars[i] == '+' || chars[i] == '-') {
                    i++;
                }
                int base = 10;
                if (i + 1 < chars.length && chars[i] == '0') {
                    base = switch (chars[i + 1]) {
                        case 'b', 'B' -> 2;
                        case 'o', 'O' -> 8;
                        case 'x', 'X' -> 16;
                        default -> 10;
                    };
                    if (Character.digit((int) chars[i + 1], 10) < 0) {
                        i += 2;
                    }
                }
                long value = 0;
                long exponent = 0;
                boolean inFraction = false;
                while (i < chars.length && (chars[i] == '.' || Character.digit((int) chars[i], base) >= 0)) {
                    if (chars[i] == '.') {
                        inFraction = true;
                    } else {
                        value = value * base + Character.digit((int) chars[i], base);
                        if (inFraction) {
                            exponent--;
                        }
                    }
                    i++;
                }
                if (i + 1 < chars.length && base < 15 && (chars[i] == 'e' || chars[i] == 'E')) {
                    int expNegative = chars[++i] == '-' ? -1 : 1;
                    if (chars[i] == '+' || chars[i] == '-') {
                        i++;
                    }
                    int exponent2 = 0;
                    while (i < chars.length && Character.digit(chars[i], base) >= 0) {
                        exponent2 = exponent2 * base + Character.digit(chars[i++], base);
                    }
                    exponent += expNegative * exponent2;
                }
                object = exponent < 0 ? negative * value / Math.pow(base, -exponent) : negative * value * (int) Math.pow(base, exponent);
                foundObject = true;
                i--;
            }
            else if (chars[i] == '[' || chars[i] == '{') {
                object = chars[i] == '[' ? new ArrayList<>() : new LinkedHashMap<>();
                foundObject = true;
            }
            else if (chars[i] == ']' || chars[i] == '}') {
                while (stackIndex >= 0 && object == null) {
                    object = stack[stackIndex] instanceof List == (chars[i] == ']') ? true : null;
                    if (stack[stackIndex--] instanceof Map) {
                        keysIndex--;
                    }
                    if (stackIndex < 0) {
                        return stack[0];
                    }
                }
            }
            else if (i + 1 < chars.length && chars[i] == '/' && (chars[i + 1] == '/' || chars[i + 1] == '*')) {
                if (chars[++i] == '/') {
                    while (++i < chars.length && chars[i] != '\n') {}
                } else {
                    while (++i < chars.length && (chars[i] != '*' || chars[i + 1] != '/')) {}
                    i++;
                }
            }
            if (foundObject) {
                if (stackIndex < 0) {
                    if (!(object instanceof List) && !(object instanceof Map)) {
                        return object;
                    }
                } else if (stack[stackIndex] instanceof List) {
                    ((List<Object>) stack[stackIndex]).add(object);
                } else if (stack[stackIndex] instanceof Map) {
                    if (foundKeys[keysIndex]) {
                        ((Map<Object, Object>) stack[stackIndex]).put(keys[keysIndex], object);
                        foundKeys[keysIndex] = false;
                    } else {
                        keys[keysIndex] = object;
                        foundKeys[keysIndex] = true;
                    }
                }
                if (object instanceof List || object instanceof Map) {
                    if (++stackIndex == stack.length) {
                        stack = Arrays.copyOf(stack, stack.length * 2);
                    }
                    stack[stackIndex] = object;
                    if (object instanceof Map) {
                        if (++keysIndex == keys.length) {
                            keys = Arrays.copyOf(keys, keys.length * 2);
                            foundKeys = Arrays.copyOf(foundKeys, foundKeys.length * 2);
                        }
                        foundKeys[keysIndex] = false;
                    }
                }
            }
        }
        return stack[0];
    }

    public static Object parseFile(Path file) {
        try {
            return parse(Files.readString(file));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static Object parseFile(File file) {
        return parseFile(file.toPath());
    }
    public static Object parseFile(Path file, Object alt) {
        try {
            return parseFile(file);
        } catch (Throwable e) {
            return alt;
        }
    }
    public static Object parseFile(File file, Object alt) {
        return parseFile(file.toPath(), alt);
    }

    public static String stringify(Object object, boolean pretty) {
        StringBuilder string = new StringBuilder();
        Iterator<Object>[] iterators = new Iterator[16];
        Boolean[] addColons = new Boolean[16];
        boolean[] areMaps = new boolean[16];
        int stackIndex = -1;
        do {
            if (stackIndex >= 0) {
                if (iterators[stackIndex].hasNext()) {
                    string.append(addColons[stackIndex] == null ? "" : (addColons[stackIndex] ? (pretty ? ": " : ":") : ","));
                    if (pretty && (addColons[stackIndex] == null || !addColons[stackIndex])) {
                        string.append("\n").append("  ".repeat(stackIndex + 1));
                    }
                    addColons[stackIndex] = (addColons[stackIndex] == null || !addColons[stackIndex]) && areMaps[stackIndex];
                    object = iterators[stackIndex].next();
                } else {
                    if (pretty) {
                        string.append("\n").append("  ".repeat(stackIndex));
                    }
                    string.append(areMaps[stackIndex--] ? "}" : "]");
                    continue;
                }
            }
            if (object instanceof Object[] || object instanceof Iterable || object instanceof Map) {
                Iterator<Object> iterator = object instanceof Object[] ? Arrays.asList((Object[]) object).iterator() : (object instanceof Iterable ? ((Iterable<Object>) object).iterator() : null);
                if (iterator == null) {
                    List<Object> list = new ArrayList<>(((Map<Object, Object>) object).size() * 2);
                    for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) object).entrySet()) {
                        list.add(entry.getKey());
                        list.add(entry.getValue());
                    }
                    iterator = list.iterator();
                }
                string.append(object instanceof Map ? (iterator.hasNext() ? "{" : "{}") : (iterator.hasNext() ? "[" : "[]"));
                if (iterator.hasNext()) {
                    if (++stackIndex == iterators.length) {
                        iterators = Arrays.copyOf(iterators, iterators.length * 2);
                        addColons = Arrays.copyOf(addColons, addColons.length * 2);
                        areMaps = Arrays.copyOf(areMaps, areMaps.length * 2);
                    }
                    iterators[stackIndex] = iterator;
                    addColons[stackIndex] = null;
                    areMaps[stackIndex] = object instanceof Map;
                }
            } else if (object instanceof String) {
                string.append("\"").append(((String) object).replace("\\", "\\\\").replace("\"", "\\\"").replace("\b", "\\b").replace("\f", "\\f").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")).append("\"");
            } else {
                string.append(object instanceof Boolean || object instanceof Number ? object : null);
            }
        } while (stackIndex >= 0);
        return string.toString();
    }

    public static String stringifyFile(Object object, boolean pretty, Path file) {
        try {
            String string = stringify(object, pretty);
            Files.writeString(file, string);
            return string;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static String stringifyFile(Object object, boolean pretty, File file) {
        return stringifyFile(object, pretty, file.toPath());
    }
}
