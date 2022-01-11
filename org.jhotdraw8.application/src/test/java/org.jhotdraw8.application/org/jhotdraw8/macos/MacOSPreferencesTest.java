package org.jhotdraw8.macos;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.ImmutableMaps;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.xml.datatype.DatatypeFactory;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class MacOSPreferencesTest {
    @NonNull
    @TestFactory
    public List<DynamicTest> dynamicTestsPreferences() {
        List<DynamicTest> list = new ArrayList<>();
        for (String file : Arrays.asList("SmallXmlPropertyList.plist",
                "SmallBinaryPropertyList.plist")) {
            list.addAll(Arrays.asList(
                    dynamicTest("nonexistent key", () -> testPreferences(file, "key", null)),
                    dynamicTest("array", () -> testPreferences(file, "a array", Arrays.asList("the item 0 value", "the item 1 value"))),
                    dynamicTest("dict", () -> testPreferences(file, "a dict", ImmutableMaps.of("a child 1", "the child 1 value", "a child 2", "the child 2 value").asMap())),
                    dynamicTest("sub-dict access", () -> testPreferences(file, "a dict\ta child 2", "the child 2 value")),

                    dynamicTest("boolean false", () -> testPreferences(file, "a boolean false", false)),
                    dynamicTest("boolean true", () -> testPreferences(file, "a boolean true", true)),
                    dynamicTest("data", () -> testPreferences(file, "a data", new byte[]{(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe})),
                    dynamicTest("date", () -> testPreferences(file, "a date", DatatypeFactory.newInstance().newXMLGregorianCalendar("2019-11-09T11:39:03Z"))),
                    dynamicTest("float", () -> testPreferences(file, "a float", 0.42)),
                    dynamicTest("integer", () -> testPreferences(file, "a integer", 42L)),
                    dynamicTest("long", () -> testPreferences(file, "a long", 4294967296L)),
                    dynamicTest("string", () -> testPreferences(file, "a string", "The String Value"))
            ));
        }
        return list;

    }

    private void testPreferences(String filename, @NonNull String key, Object expectedValue) throws URISyntaxException {
        URL resource = getClass().getResource(filename);
        if (resource == null) {
            throw new IllegalArgumentException("Could not find resource with filename=\"" + filename + "\" for class=" + getClass() + ".");
        }
        File file = new File(resource.toURI());
        // System.out.println(filename + ", " + key.replaceAll("\t", "→") + " = " + expectedValue);
        final Object actualValue = MacOSPreferences.get(file, key);
        if (expectedValue instanceof byte[]) {
            assertArrayEquals((byte[]) expectedValue, (byte[]) actualValue, "key=" + key);
        } else {
            assertEquals(expectedValue, actualValue, "key=" + key);
        }
    }
}