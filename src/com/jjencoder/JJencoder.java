package com.jjencoder;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JJencoder {
    public final static String UNDEFINED = "undefined";
    public final static String OBJECT_OBJECT = "[object Object]";
    private static HashMap<String, String> tokens;

    private VariablesMap variables;

    public JJencoder() {
        variables = new VariablesMap();
        tokens = new HashMap<String, String>();
        tokens.put("^~\\[]$", "-1"); //
        tokens.put("^\\[]$", "0"); //
        tokens.put("^!\\[]\\+\"\"$", "false"); //
        tokens.put("^\\{\\}\\+\"\"$", OBJECT_OBJECT); //
        tokens.put("^.+\\[[^.]+]\\+\"\"$", UNDEFINED); //
        tokens.put("^\\+\\+.+", "1"); //
        tokens.put("^!\"\"\\+\"\"$", "true"); //
        tokens.put("^[^\\[\\]\\.\\(\\)]+\\+\"\"$", OBJECT_OBJECT); //
        tokens.put("^((.+)\\.(.+))\\+\"\"", UNDEFINED); //
        tokens.put("^\\(!.+\\)\\+\"\"$", "false"); //

    }

    private String parseRawValue(String rawValue) {
        String result = null;
        rawValue = replaceInitialAndFinalParentheses(rawValue);
        Set<String> keys = tokens.keySet();
        for (String key : keys) {
            if (rawValue.matches(key)) {
                result = tokens.get(key);
                break;
            }
        }

        if (result != null && result.equals("1")) {
            int value = variables.getGlobalVariableValue() + 1;
            result = String.valueOf(value);
            variables.setGlobalVariableValue(result);
        }
        if (variables.containsKey(rawValue)) {
            result = OBJECT_OBJECT;
        } else if (result == null) {
            result = rawValue;
        }
        return result;
    }

    private int getIndexFromString(String indexString) {
        if (indexString.contains("[") && indexString.contains("]")) {
            indexString = indexString.replaceAll("\\[", "").replaceAll("]", "");
        }

        String value = variables.get(indexString);

        if (value == null) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String parseExpression(String rawValue) {
        String pattern = "";
        String index = "";

        if (!rawValue.contains("[") && !rawValue.contains("]")) {
            if (variables.get(rawValue) != null) return variables.get(rawValue);
            return parseRawValue(rawValue);
        }
        if (!rawValue.contains("(") && !rawValue.contains(")")) {
            int splitIndex = rawValue.indexOf("[");
            pattern = rawValue.substring(0, splitIndex);
            index = rawValue.substring(splitIndex, rawValue.length());
        } else {
            int splitIndex = rawValue.lastIndexOf(")") + 1;

            pattern = rawValue.substring(0, splitIndex);
            index = rawValue.substring(splitIndex, rawValue.length());
        }

        String value = parseRawValue(pattern);
        int arrayIndex = getIndexFromString(index);

        if (value.equals(rawValue)) return rawValue;
        if (arrayIndex == -1) {
            throw new IllegalStateException("Something went wrong. There is no " + index + " variable in stack");
        }

        return String.valueOf(value.charAt(arrayIndex));
    }

    private String replaceInitialAndFinalParentheses(String str) {
        if (str.indexOf("(") != -1 && str.indexOf("(") == 0) {
            if (str.lastIndexOf(")") != -1 && str.lastIndexOf(")") == (str.length() - 1)) {
                str = str.substring(1, str.length() - 1);
            }
        }
        return str;
    }

    private String processCompoundVariable(String rawValue) {
        if (!rawValue.contains("=")) return parseExpression(rawValue);

        if (rawValue.lastIndexOf(")") != rawValue.length() - 1) {
            Matcher matcher = Pattern.compile("\\((.*?)\\)").matcher(rawValue);
            String insideValue = "";
            if (matcher.find()) {
                insideValue = matcher.group(1);
            } else {
                return null;
            }

            String value = parseRawValue(insideValue.split("=")[1]);
            variables.put(insideValue.split("=")[0], value);

            String indexString = rawValue.substring(rawValue.indexOf("[") + 1, rawValue.indexOf("]"));
            return String.valueOf(value.charAt(getIndexFromString(indexString)));
        } else {
            rawValue = replaceInitialAndFinalParentheses(rawValue);
            String value = parseExpression(rawValue.split("=")[1]);
            variables.put(rawValue.split("=")[0], value);
            return value;
        }
    }

    private String parseArgument(String argument) {
        if (variables.get(argument) != null) return variables.get(argument);
        return parseExpression(argument);
    }

    private void parseGlobalVariable(String str) {
        variables.put(str.split("=")[0], parseRawValue(str.split("=")[1]));
    }

    private void parseProperties(String str) {
        String[] properties = str.split(",");
        String globalVariableName = variables.getGlobalVariable();
        for (String property : properties) {
            String propertyName = property.split(":")[0];
            String propertyValue = property.split(":")[1];
            variables.putProperty(globalVariableName, propertyName, processCompoundVariable(propertyValue));
        }
    }

    private static String[] parseArguments(String rawValue) {
        ArrayList<String> result = new ArrayList<String>();
        int startIndex = 0;
        int endIndex = 0;
        while (endIndex < rawValue.length()) {
            endIndex = rawValue.indexOf("+", endIndex);
            if (endIndex == -1) {
                result.add(rawValue.substring(startIndex, rawValue.length()));
                break;
            }
            if (rawValue.charAt(endIndex + 1) == '"') {
                if (rawValue.charAt(endIndex + 2) == '"') {
                    endIndex++;
                    continue;
                }
            }
            result.add(rawValue.substring(startIndex, endIndex));
            startIndex = endIndex + 1;
            endIndex++;
        }
        String[] array = new String[result.size()];
        return result.toArray(array);
    }

    public String decode(String input) {
        if (input == null) throw new IllegalArgumentException("input can't be null");
        String[] args = input.split(";");

        ArrayList<String> list = convertRows(args);

        parseGlobalVariable(list.get(0));

        String buffer = list.get(1).split("=")[1];
        String properties = buffer.substring(1, buffer.length() - 1);

        parseProperties(properties);

        for (int i = 2; i < list.size() - 1; i++) {
            parseVariableArguments(list.get(i));
        }

        String code = findCode(list.get(list.size() - 1));

        StringBuilder builder = new StringBuilder();
        for (String s : parseArguments(code)) {
            s = replaceInitialAndFinalQuotes(s);
            builder.append(parseArgument(s));
        }
        String result = convertOctalFormat(builder.toString());
        result = result.replaceFirst("return", "");

        return replaceInitialAndFinalQuotes(result);
    }

    private String replaceInitialAndFinalQuotes(String str) {
        if (str.length() <= 1) return str;
        if (str.indexOf("\"") != -1 && str.indexOf("\"") == 0) {
            if (str.lastIndexOf("\"") != -1 && str.lastIndexOf("\"") == (str.length() - 1)) {
                str = str.substring(1, str.length() - 1);
            }
        }
        return str;
    }

    private String findCode(String s) {
        int startIndex;
        int endIndex;
        startIndex = s.indexOf("(");
        startIndex = s.indexOf("(", startIndex + 1) + 1;
        endIndex = s.lastIndexOf(")())()");
        return s.substring(startIndex, endIndex);
    }

    private void parseVariableArguments(String str) {
        String variableName = str.split("=")[0];
        int valueStartPosition = str.indexOf("=");
        String rawValue = str.substring(valueStartPosition + 1, str.length());
        String[] args = parseArguments(rawValue);
        StringBuilder builder = new StringBuilder("");
        for (String arg : args) {
            builder.append(processCompoundVariable(arg));
        }
        variables.put(variableName, builder.toString());
    }

    private ArrayList<String> convertRows(String[] args) {
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();

        int i = 0;
        while (!args[i].matches(".+=\\(.+\\)\\[.+\\]\\[.+\\]")) { // fix case when there is '=' in quotes
            list.add(args[i]);
            i++;
        }
        i++;
        while (i < args.length) {
            builder.append(args[i]).append(";");
            i++;
        }

        list.add(builder.toString());
        return list;
    }

    private String convertOctalFormat(String str) {
        // Some kind of magic but after two circles it works good =)
        str = StringEscapeUtils.unescapeJava(str);
        str = StringEscapeUtils.unescapeJava(str);
        return str;
    }

    public void reset() {
        variables.clear();
    }


    class VariablesMap extends HashMap<String, String> {
        public VariablesMap() {
            super();
        }

        public String putProperty(String globalVariableName, String key, String value) {
            return super.put(globalVariableName + "." + key, value);
        }

        public String getGlobalVariable() {
            Set<String> iterator = keySet();
            for (String s : iterator) {
                if (!s.contains(".")) return s;
            }
            return null;
        }

        public int getGlobalVariableValue() {
            return Integer.parseInt(get(getGlobalVariable()));
        }

        public void setGlobalVariableValue(String value) {
            put(getGlobalVariable(), value);
        }

    }
}
