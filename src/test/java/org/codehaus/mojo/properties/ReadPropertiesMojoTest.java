package org.codehaus.mojo.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class ReadPropertiesMojoTest {
    private static final String NEW_LINE = System.getProperty("line.separator");

    private Map<String, String> represent;

    private Map<String, String> notRepresent;
    private MavenProject projectStub;
    private ReadPropertiesMojo readPropertiesMojo;

    @Before
    public void setUp() {
        represent = new HashMap<>();
        notRepresent = new HashMap<>();
        represent.put("test.property1", "value1®");
        represent.put("test.property2", "value2");

        notRepresent.put("test.property3", "value3™");
        notRepresent.put("test.property4", "κόσμε");
        projectStub = new MavenProject();
        readPropertiesMojo = new ReadPropertiesMojo();
        readPropertiesMojo.setProject(projectStub);
    }

    @Test
    public void readPropertiesWithoutKeyprefix() throws Exception {
        final Map<String, String> propMap = new HashMap<>();
        propMap.putAll(represent);
        propMap.putAll(notRepresent);

        final String encoding = ReadPropertiesMojo.DEFAULT_ENCODING;
        File referenceFile = getPropertyFileForTesting(encoding, propMap);
        try (FileInputStream fileStream = new FileInputStream(referenceFile);
                InputStreamReader fr = new InputStreamReader(fileStream, encoding)) {
            // load properties directly for comparison later
            Properties testProperties = new Properties();
            testProperties.load(fr);

            // do the work
            readPropertiesMojo.setFiles(new File[] {getPropertyFileForTesting(encoding, propMap)});
            readPropertiesMojo.execute();

            // check results
            Properties projectProperties = projectStub.getProperties();
            assertNotNull(projectProperties);
            // it should not be empty
            assertNotEquals(0, projectProperties.size());

            // we are not adding prefix, so properties should be same as in file
            assertEquals(testProperties.size(), projectProperties.size());
            assertEquals(testProperties, projectProperties);

            // strings which are representable in ISO-8859-1 should match the source data
            for (String sourceKey : represent.keySet()) {
                assertEquals(represent.get(sourceKey), projectProperties.getProperty(sourceKey));
            }

            // strings which are not representable in ISO-8859-1 underwent a conversion
            // which lost data
            for (String sourceKey : notRepresent.keySet()) {
                String sourceValue = notRepresent.get(sourceKey);
                String converted = new String(sourceValue.getBytes(encoding), StandardCharsets.UTF_8);
                String propVal = projectProperties.getProperty(sourceKey);
                assertNotEquals(sourceValue, propVal);
                assertEquals(converted, propVal);
            }
        }
    }

    @Test
    public void readPropertiesWithKeyprefix() throws Exception {
        String keyPrefix = "testkey-prefix.";
        final String encoding = ReadPropertiesMojo.DEFAULT_ENCODING;

        final Map<String, String> propMap = new HashMap<>();
        propMap.putAll(represent);
        propMap.putAll(notRepresent);

        try (FileInputStream fs1 = new FileInputStream(getPropertyFileForTesting(keyPrefix, encoding, propMap));
                FileInputStream fs2 = new FileInputStream(getPropertyFileForTesting(encoding, propMap));
                InputStreamReader fr1 = new InputStreamReader(fs1, encoding);
                InputStreamReader fr2 = new InputStreamReader(fs2, encoding)) {
            Properties testPropertiesWithoutPrefix = new Properties();
            testPropertiesWithoutPrefix.load(fr2);
            // do the work
            readPropertiesMojo.setKeyPrefix(keyPrefix);
            readPropertiesMojo.setFiles(new File[] {getPropertyFileForTesting(encoding, propMap)});
            readPropertiesMojo.execute();

            // load properties directly and add prefix for comparison later
            Properties testPropertiesPrefix = new Properties();
            testPropertiesPrefix.load(fr1);
            // check results
            Properties projectProperties = projectStub.getProperties();
            assertNotNull(projectProperties);
            // it should not be empty
            assertNotEquals(0, projectProperties.size());

            // we are adding prefix, so prefix properties should be same as in
            // projectProperties
            assertEquals(testPropertiesPrefix.size(), projectProperties.size());
            assertEquals(testPropertiesPrefix, projectProperties);

            // properties with and without prefix shouldn't be same
            assertNotEquals(testPropertiesPrefix, testPropertiesWithoutPrefix);
            assertNotEquals(testPropertiesWithoutPrefix, projectProperties);

            // strings which are representable in ISO-8859-1 should match the source data
            for (String sourceKey : represent.keySet()) {
                assertEquals(represent.get(sourceKey), projectProperties.getProperty(keyPrefix + sourceKey));
            }

            // strings which are not representable in ISO-8859-1 underwent a conversion
            // which lost data
            for (String sourceKey : notRepresent.keySet()) {
                String sourceValue = notRepresent.get(sourceKey);
                String converted = new String(sourceValue.getBytes(encoding), StandardCharsets.UTF_8);
                String propVal = projectProperties.getProperty(keyPrefix + sourceKey);
                assertNotEquals(sourceValue, propVal);
                assertEquals(converted, propVal);
            }
        }
    }

    @Test
    public void readPropertiesWithEncoding() throws Exception {
        final Map<String, String> propMap = new HashMap<>();
        propMap.putAll(represent);
        propMap.putAll(notRepresent);

        final String encoding = StandardCharsets.UTF_8.name();
        File referenceFile = getPropertyFileForTesting(encoding, propMap);
        try (FileInputStream fileStream = new FileInputStream(referenceFile);
                InputStreamReader fr = new InputStreamReader(fileStream, encoding)) {
            // load properties directly for comparison later
            Properties testProperties = new Properties();
            testProperties.load(fr);

            // do the work
            readPropertiesMojo.setFiles(new File[] {getPropertyFileForTesting(encoding, propMap)});
            readPropertiesMojo.setEncoding(encoding);
            readPropertiesMojo.execute();

            // check results
            Properties projectProperties = projectStub.getProperties();
            assertNotNull(projectProperties);
            // it should not be empty
            assertNotEquals(0, projectProperties.size());

            // we are not adding prefix, so properties should be same as in file
            assertEquals(testProperties.size(), projectProperties.size());
            assertEquals(testProperties, projectProperties);

            // all test values are representable in UTF-8 and should match original source
            // data
            for (String sourceKey : propMap.keySet()) {
                assertEquals(propMap.get(sourceKey), projectProperties.getProperty(sourceKey));
            }
        }
    }

    @Test
    public void testDefaultValueForUnresolvedPropertyWithEnabledFlag()
            throws MojoFailureException, MojoExecutionException, IOException {
        Properties properties = new Properties();
        properties.setProperty("p1", "${unknown:}");
        properties.setProperty("p2", "${unknown:defaultValue}");
        properties.setProperty("p3", "http://${uhost:localhost}:${uport:8080}");
        properties.setProperty("p4", "http://${host:localhost}:${port:8080}");
        properties.setProperty("p5", "${unknown:${fallback}}");
        properties.setProperty("p6", "${unknown:${double.unknown}}");
        properties.setProperty("p7", "${unknown:with space}");
        properties.setProperty("p8", "${unknown:with extra :}");
        properties.setProperty("p9", "${malformed:defVal");
        properties.setProperty("p10", "${malformed:with space");
        properties.setProperty("p11", "${malformed:with extra :");
        properties.setProperty("p12", "${unknown::}");
        properties.setProperty("p13", "${unknown:  }");

        properties.setProperty("host", "example.com");
        properties.setProperty("port", "9090");
        properties.setProperty("fallback", "fallback value");

        Model model = new Model();
        model.setProperties(properties);
        MavenProject project = new MavenProject(model);
        readPropertiesMojo.setProject(project);
        readPropertiesMojo.setUseDefaultValues(true);
        readPropertiesMojo.execute();

        Properties processed = readPropertiesMojo.getProject().getProperties();

        String value1 = processed.getProperty("p1");
        String value2 = processed.getProperty("p2");
        String value3 = processed.getProperty("p3");
        String value4 = processed.getProperty("p4");
        String value5 = processed.getProperty("p5");
        String value6 = processed.getProperty("p6");
        String value7 = processed.getProperty("p7");
        String value8 = processed.getProperty("p8");
        String value9 = processed.getProperty("p9");
        String value10 = processed.getProperty("p10");
        String value11 = processed.getProperty("p11");
        String value12 = processed.getProperty("p12");
        String value13 = processed.getProperty("p13");

        assertEquals("${unknown}", value1);
        assertEquals("defaultValue", value2);
        assertEquals("http://localhost:8080", value3);
        assertEquals("http://example.com:9090", value4);
        assertEquals("fallback value", value5);
        assertEquals("${double.unknown}", value6);
        assertEquals("with space", value7);
        assertEquals("with extra :", value8);
        assertEquals("${malformed:defVal", value9);
        assertEquals("${malformed:with space", value10);
        assertEquals("${malformed:with extra :", value11);
        assertEquals(":", value12);
        assertEquals("  ", value13);
    }

    /**
     * with the flag disabled (default behavior) nothing gets replaced ':' is
     * treated as a regular character and part of the property name
     */
    @Test
    public void testDefaultValueForUnresolvedPropertyWithDisabledFlag()
            throws MojoFailureException, MojoExecutionException, IOException {
        Properties properties = new Properties();
        properties.setProperty("p1", "${unknown:}");
        properties.setProperty("p2", "${unknown:defaultValue}");
        properties.setProperty("p3", "http://${uhost:localhost}:${uport:8080}");
        properties.setProperty("p4", "http://${host:localhost}:${port:8080}");
        properties.setProperty("p5", "${unknown:${fallback}}");
        properties.setProperty("p6", "${unknown:${double.unknown}}");
        properties.setProperty("p7", "${unknown:with space}");
        properties.setProperty("p8", "${unknown:with extra :}");
        properties.setProperty("p9", "${malformed:defVal");
        properties.setProperty("p10", "${malformed:with space");
        properties.setProperty("p11", "${malformed:with extra :");
        properties.setProperty("p12", "${unknown::}");
        properties.setProperty("p13", "${unknown:  }");

        properties.setProperty("host", "example.com");
        properties.setProperty("port", "9090");
        properties.setProperty("fallback", "fallback value");

        Model model = new Model();
        model.setProperties(properties);
        MavenProject project = new MavenProject(model);
        readPropertiesMojo.setProject(project);
        readPropertiesMojo.execute();

        Properties processed = readPropertiesMojo.getProject().getProperties();

        String value1 = processed.getProperty("p1");
        String value2 = processed.getProperty("p2");
        String value3 = processed.getProperty("p3");
        String value4 = processed.getProperty("p4");
        String value5 = processed.getProperty("p5");
        String value6 = processed.getProperty("p6");
        String value7 = processed.getProperty("p7");
        String value8 = processed.getProperty("p8");
        String value9 = processed.getProperty("p9");
        String value10 = processed.getProperty("p10");
        String value11 = processed.getProperty("p11");
        String value12 = processed.getProperty("p12");
        String value13 = processed.getProperty("p13");

        assertEquals("${unknown:}", value1);
        assertEquals("${unknown:defaultValue}", value2);
        assertEquals("http://${uhost:localhost}:${uport:8080}", value3);
        assertEquals("http://${host:localhost}:${port:8080}", value4);
        assertEquals("${unknown:${fallback}}", value5);
        assertEquals("${unknown:${double.unknown}}", value6);
        assertEquals("${unknown:with space}", value7);
        assertEquals("${unknown:with extra :}", value8);
        assertEquals("${malformed:defVal", value9);
        assertEquals("${malformed:with space", value10);
        assertEquals("${malformed:with extra :", value11);
        assertEquals("${unknown::}", value12);
        assertEquals("${unknown:  }", value13);
    }

    @Test
    public void testInvalidEncoding() throws Exception {
        readPropertiesMojo.setFiles(new File[] {getPropertyFileForTesting(StandardCharsets.UTF_8.name(), represent)});
        readPropertiesMojo.setEncoding("invalid-encoding");
        MojoExecutionException thrown = assertThrows(MojoExecutionException.class, () -> readPropertiesMojo.execute());
        assertEquals(thrown.getMessage(), "Invalid encoding 'invalid-encoding'");
    }

    private File getPropertyFileForTesting(String encoding, Map<String, String> properties) throws IOException {
        return getPropertyFileForTesting(null, encoding, properties);
    }

    private File getPropertyFileForTesting(String keyPrefix, String encoding, Map<String, String> properties)
            throws IOException {
        File f = File.createTempFile("prop-test", ".properties");
        f.deleteOnExit();
        try (FileOutputStream fileStream = new FileOutputStream(f); //
                OutputStreamWriter writer = new OutputStreamWriter(fileStream, encoding)) {
            String prefix = keyPrefix;
            if (prefix == null) {
                prefix = "";
            }
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                writer.write(prefix + entry.getKey() + "=" + entry.getValue() + NEW_LINE);
            }
            writer.flush();
        }
        return f;
    }
}
