package com.jjencoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: maa
 * Date: 20.09.13
 */
public class JJencoder {
    public final static String BASE_SYMBOL = "$";

    private static HashMap<String, String> tokens;
    private static String sGlobalVariableName;
    private static HashMap<String, String> variables;

    static {
        tokens = new HashMap<String, String>();
        tokens.put("~[]", "-1");
        tokens.put("[]", "0");
        tokens.put("(![]+\"\")", "false");
        tokens.put("({}+\"\")", "[object Object]");
        tokens.put("($[$]+\"\")", "undefined");
        tokens.put("++$", "1");
        tokens.put("(!\"\"+\"\")", "true");
        tokens.put("$+\"\"", "[object Object]");
    }


    public static String decode(String str) {
        variables = new HashMap<String, String>();
//        parseInitialSet(str);
        parseAdditionalVariables(str);
        return null;
    }

    private static void parseAdditionalVariables(String str) {
        String[] array = str.split(";");
        parseAllVariables(array);
    }

    private static int countCharMatches(String str, String c) {
        return (str.length() - str.replaceAll(c, "").length());
    }

    private static void parseInitialSet(String str) {
        String header = str.split(";")[0];
        String[] mainVariableData = header.split("=");

        sGlobalVariableName = mainVariableData[0];
        variables.put(mainVariableData[0], tokens.get(mainVariableData[1]));

        String propertiesString = str.split(";")[1];

        propertiesString = propertiesString.split("=")[1];
        propertiesString = extractData(propertiesString);

        String[] propertiesArray = propertiesString.split(",");
        fullFillProperties(propertiesArray);
    }

    private static String extractData(String str) {
        return str.substring(1, str.length() - 1);
    }

    private static void printCharArray(char[] array) {
        System.out.println("--------Printing Array-------------");
        for (int i = 0; i < array.length; i++) {
            System.err.println("[" + i + "] = " + array[i]);
        }
    }

    public static void fullFillProperties(String[] properties) {
        for (String property : properties) {
            String propertyName = property.split(":")[0];
            String propertyValue = property.split(":")[1];
            variables.put(propertyName, getValue(propertyValue));
        }
    }

    private static String getValue(String rawValue) {
        if (rawValue.contains("(") && rawValue.contains(")")) {
            String expression = rawValue.substring(0, rawValue.indexOf(")") + 1);
            String arrayIndexString = rawValue.substring(rawValue.indexOf(")") + 1, rawValue.length());

            expression = expression.replace(sGlobalVariableName, BASE_SYMBOL);

            String array = tokens.get(expression);

            int startIndex = arrayIndexString.indexOf("[");
            int endIndex = arrayIndexString.indexOf("]");
            String indexValue = arrayIndexString.substring(startIndex + 1, endIndex);

            int index = Integer.parseInt(variables.get(indexValue));
            return String.valueOf(array.charAt(index));
        } else {
            if (rawValue.contains("\"")){

            } else {
                rawValue = rawValue.replace(sGlobalVariableName, BASE_SYMBOL);
                String realValue = tokens.get(rawValue);
                if (realValue == null) return "";
                int intValue = Integer.valueOf(variables.get(sGlobalVariableName)) + Integer.valueOf(realValue);
                variables.put(sGlobalVariableName, String.valueOf(intValue));
                return String.valueOf(intValue);
            }
        }
    }

    private static void parseAllVariables(String[] source) {
        String[] mainVariableData = source[0].split("=");

        sGlobalVariableName = mainVariableData[0];
        variables.put(mainVariableData[0], tokens.get(mainVariableData[1]));

        if (source[1].contains("{") && source[1].contains("}")) {
            String[] propertiesArray = source[1].split("=")[1].split(",");
            fullFillProperties(propertiesArray);
        }

        for (int i = 2; i < source.length - 1; i++) {
            String variableName = source[i].split("=")[0];
            int valueStartPosition = source[i].indexOf("=");
            String rawValue = source[i].substring(valueStartPosition + 1, source[i].length() - 1);
            String variableValue = parseVariableValue(rawValue);
        }
    }

    private static String parseVariableValue(String rawValue) {
        String[] argsArray = parseArguments(rawValue);
        for (String arg : argsArray) {
            if (arg.contains("=")){
                Pattern pattern = Pattern.compile("\\(([^)]*)\\)");
                Matcher m = pattern.matcher(arg);
                String insideArgument = "";
                if (m.find()) {
                    insideArgument = m.group(1);
                }
                String variableName = insideArgument.split("=")[0];
                String variableValue = getValue(insideArgument.split("=")[1]);
                System.out.println(variableName + " = " + variableValue);
            }
        }
        return null;
    }

    private static String[] parseArguments(String rawValue) {
        ArrayList<String> result = new ArrayList<String>();
        int startIndex = 0;
        int endIndex = 0;
        while (endIndex < rawValue.length()) {
            endIndex = rawValue.indexOf("+", endIndex);
            if (endIndex == -1) break;
            if (rawValue.charAt(endIndex + 1) == '"') {
                endIndex++;
                continue;
            }
            result.add(rawValue.substring(startIndex, endIndex));
            startIndex = endIndex + 1;
            endIndex++;
        }
        String[] array = new String[result.size()];
        return result.toArray(array);
    }

}