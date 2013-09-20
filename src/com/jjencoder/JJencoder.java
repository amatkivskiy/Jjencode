package com.jjencoder;

import java.util.HashMap;

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
    }


    public static String decode(String str) {
        variables = new HashMap<String, String>();
        parseInitialSet(str);
        return null;
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

        System.err.println(variables.toString());
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
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].split(":")[0];
            String propertyValue = properties[i].split(":")[1];
            variables.put(propertyName, getValue(propertyValue));
        }
    }

    public static String getValue(String rawValue) {
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
            rawValue = rawValue.replace(sGlobalVariableName, BASE_SYMBOL);
            String realValue = tokens.get(rawValue);
            if (realValue == null) return "";
            int intValue = Integer.valueOf(variables.get(sGlobalVariableName)) + Integer.valueOf(realValue);
            variables.put(sGlobalVariableName, String.valueOf(intValue));
            return String.valueOf(intValue);
        }
    }
}