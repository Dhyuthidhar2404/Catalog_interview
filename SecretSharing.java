import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretSharing {

    // Decode value based on base
    public static BigInteger decodeValue(String base, String value) {
        return new BigInteger(value, Integer.parseInt(base));
    }

    // Lagrange interpolation to find the constant term (secret)
    public static BigInteger lagrangeInterpolation(List<Integer> x, List<BigInteger> y, int xEval) {
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < x.size(); i++) {
            BigInteger term = y.get(i);
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < x.size(); j++) {
                if (i != j) {
                    numerator = numerator.multiply(BigInteger.valueOf(xEval - x.get(j)));
                    denominator = denominator.multiply(BigInteger.valueOf(x.get(i) - x.get(j)));
                }
            }

            term = term.multiply(numerator).divide(denominator);
            result = result.add(term);
        }

        return result;
    }

    // Function to find the secret
    public static BigInteger findSecret(Map<String, Object> data) {
        // Extract 'keys' data
        Object keysObj = data.get("keys");
        if (!(keysObj instanceof Map)) {
            throw new IllegalArgumentException("'keys' should be a JSON object.");
        }
        Map<String, Object> keys = (Map<String, Object>) keysObj;

        // Extract 'n' and 'k'
        Object nObj = keys.get("n");
        Object kObj = keys.get("k");
        if (!(nObj instanceof Number) || !(kObj instanceof Number)) {
            throw new IllegalArgumentException("'n' and 'k' should be numbers.");
        }
        int n = ((Number) nObj).intValue();
        int k = ((Number) kObj).intValue();

        List<Integer> x = new ArrayList<>();
        List<BigInteger> y = new ArrayList<>();

        // Iterate through the expected number of roots
        for (int i = 1; i <= n && x.size() < k; i++) {
            String key = String.valueOf(i);
            if (data.containsKey(key)) {
                Object pointObj = data.get(key);
                if (pointObj instanceof Map) {
                    Map<String, Object> point = (Map<String, Object>) pointObj;
                    Object baseObj = point.get("base");
                    Object valueObj = point.get("value");
                    if (baseObj instanceof String && valueObj instanceof String) {
                        x.add(i);
                        y.add(decodeValue((String) baseObj, (String) valueObj));
                    } else {
                        System.out.println("Invalid 'base' or 'value' for key: " + key);
                    }
                } else {
                    System.out.println("Expected a Map for key: " + key + ", but found: " + pointObj.getClass().getSimpleName());
                }
            } else {
                System.out.println("Missing key: " + key); // Debugging output
            }
        }

        // Ensure we have enough points to calculate the secret
        if (x.size() < k) {
            throw new IllegalArgumentException("Not enough points to determine the polynomial.");
        }

        return lagrangeInterpolation(x, y, 0);
    }

    // Improved manual JSON parser that handles nested objects
    public static Map<String, Object> parseJson(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format.");
        }
        return parseObject(json.substring(1, json.length() - 1));
    }

    // Parses a JSON object string into a Map
    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> map = new HashMap<>();
        List<String> keyValuePairs = splitTopLevel(json);

        for (String pair : keyValuePairs) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid JSON key-value pair: " + pair);
            }
            String key = removeQuotes(parts[0].trim());
            String value = parts[1].trim();

            if (value.startsWith("{") && value.endsWith("}")) {
                map.put(key, parseObject(value.substring(1, value.length() - 1)));
            } else if (value.startsWith("\"") && value.endsWith("\"")) {
                map.put(key, removeQuotes(value));
            } else {
                // Attempt to parse as number
                try {
                    if (value.contains(".")) {
                        map.put(key, Double.parseDouble(value));
                    } else {
                        map.put(key, Long.parseLong(value));
                    }
                } catch (NumberFormatException e) {
                    map.put(key, value); // Store as string if it's not a number
                }
            }
        }

        return map;
    }

    // Splits a JSON object string into top-level key-value pairs
    private static List<String> splitTopLevel(String json) {
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }

            if (!inQuotes) {
                if (c == '{') {
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                } else if (c == ',' && braceDepth == 0) {
                    pairs.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            pairs.add(current.toString());
        }

        return pairs;
    }

    // Removes leading and trailing quotes from a string
    private static String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    public static void main(String[] args) {
        try {
            // Process input1.json
            String jsonContent1 = new String(Files.readAllBytes(Paths.get("C:\\Users\\S.S.S.Dhyuthidhar\\Documents\\input.json")));
            Map<String, Object> jsonData1 = parseJson(jsonContent1);
            BigInteger secret1 = findSecret(jsonData1);

            // Process input.json
            String jsonContent2 = new String(Files.readAllBytes(Paths.get("C:\\Users\\S.S.S.Dhyuthidhar\\Documents\\input1.json")));
            Map<String, Object> jsonData2 = parseJson(jsonContent2);
            BigInteger secret2 = findSecret(jsonData2);

            // Print results on separate lines
            System.out.println("The secret (constant term c) for input1.json is: " + secret1);
            System.out.println("The secret (constant term c) for input.json is: " + secret2);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (ClassCastException e) {
            System.err.println("Unexpected JSON structure: " + e.getMessage());
        }
    }
}