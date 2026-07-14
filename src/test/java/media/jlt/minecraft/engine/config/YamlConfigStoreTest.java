package media.jlt.minecraft.engine.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlConfigStoreTest {
    @TempDir
    Path tempDirectory;

    @Test
    void missingConfigIsCreatedFromSanitizedDefaults() throws IOException {
        AtomicInteger sanitizations = new AtomicInteger();
        YamlConfigStore<TestConfig, TestPersisted> store = store("nested/test.yaml", sanitizations);

        YamlConfigStore.ReloadResult<TestConfig> result = store.load();

        assertTrue(result.success());
        assertEquals(5, result.config().value);
        assertTrue(result.config().sanitized);
        assertEquals(1, sanitizations.get());
        assertTrue(Files.exists(store.configFile()));
        assertTrue(Files.readString(store.configFile()).contains("value: 5"));
    }

    @Test
    void validConfigIsSanitizedAndRewritten() throws IOException {
        AtomicInteger sanitizations = new AtomicInteger();
        YamlConfigStore<TestConfig, TestPersisted> store = store("test.yaml", sanitizations);
        Files.writeString(store.configFile(), "value: 999\nnested:\n  flag: false\n");

        YamlConfigStore.ReloadResult<TestConfig> result = store.reload();

        assertTrue(result.success());
        assertEquals(10, result.config().value);
        assertFalse(result.config().nestedFlag);
        assertTrue(result.config().sanitized);
        assertEquals(1, sanitizations.get());
        String rewritten = Files.readString(store.configFile());
        assertTrue(rewritten.contains("value: 10"));
    }

    @Test
    void malformedConfigReturnsFailureWithoutOverwritingInput() throws IOException {
        YamlConfigStore<TestConfig, TestPersisted> store = store("test.yaml", new AtomicInteger());
        String malformed = "value: [1, 2\n";
        Files.writeString(store.configFile(), malformed);

        YamlConfigStore.ReloadResult<TestConfig> result = store.reload();

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertEquals(malformed, Files.readString(store.configFile()));
    }

    @Test
    void emptyAndYamlNullConfigsRegenerateDefaults() throws IOException {
        YamlConfigStore<TestConfig, TestPersisted> emptyStore = store("empty.yaml", new AtomicInteger());
        YamlConfigStore<TestConfig, TestPersisted> nullStore = store("null.yaml", new AtomicInteger());
        Files.writeString(emptyStore.configFile(), "");
        Files.writeString(nullStore.configFile(), "null\n");

        YamlConfigStore.ReloadResult<TestConfig> emptyResult = emptyStore.reload();
        YamlConfigStore.ReloadResult<TestConfig> nullResult = nullStore.reload();

        assertTrue(emptyResult.success());
        assertTrue(nullResult.success());
        assertEquals(5, emptyResult.config().value);
        assertEquals(5, nullResult.config().value);
    }

    @Test
    void blockingParentReturnsFailureWithoutChangingIt() throws IOException {
        Path blockingFile = tempDirectory.resolve("not-a-directory");
        Files.writeString(blockingFile, "keep me");
        YamlConfigStore<TestConfig, TestPersisted> store = new YamlConfigStore<>(
            blockingFile,
            "test.yaml",
            TestPersisted.class,
            Map.of(),
            TestConfig::new,
            YamlConfigStoreTest::sanitize,
            YamlConfigStoreTest::toPersisted,
            YamlConfigStoreTest::fromPersisted
        );

        YamlConfigStore.ReloadResult<TestConfig> result = store.reload();

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertEquals("keep me", Files.readString(blockingFile));
    }

    @Test
    void failureFactoryAlwaysProvidesUsefulErrorText() {
        YamlConfigStore.ReloadResult<TestConfig> nullMessage = YamlConfigStore.ReloadResult.failure(null);
        YamlConfigStore.ReloadResult<TestConfig> blankMessage = YamlConfigStore.ReloadResult.failure("  ");

        assertEquals("Unknown configuration error", nullMessage.errorMessage());
        assertEquals("Unknown configuration error", blankMessage.errorMessage());
        assertFalse(nullMessage.success());
    }

    @Test
    void dumpPreservesFieldDeclarationOrderRegardlessOfSourceOrder() throws IOException {
        YamlConfigStore<TestConfig, TestPersisted> store = store("order.yaml", new AtomicInteger());
        Files.writeString(store.configFile(), "nested:\n  flag: false\nvalue: 7\n");

        store.reload();

        String dumped = Files.readString(store.configFile());
        assertTrue(dumped.indexOf("value:") < dumped.indexOf("nested:"),
            "expected 'value' (declared first) before 'nested' (declared second):\n" + dumped);
    }

    @Test
    void dumpAttachesDocCommentsAboveDocumentedKeysIncludingNestedOnes() throws IOException {
        AtomicInteger sanitizations = new AtomicInteger();
        Path directory = tempDirectory.resolve("docs");
        YamlConfigStore<TestConfig, TestPersisted> store = new YamlConfigStore<>(
            directory,
            "test.yaml",
            TestPersisted.class,
            Map.of("value", "The test value.", "nested.flag", "The nested flag."),
            TestConfig::new,
            config -> sanitize(config, sanitizations),
            YamlConfigStoreTest::toPersisted,
            YamlConfigStoreTest::fromPersisted
        );

        store.load();

        String dumped = Files.readString(store.configFile());
        assertTrue(dumped.contains("# The test value.\nvalue:"), dumped);
        assertTrue(dumped.contains("# The nested flag.\n  flag:"), dumped);
    }

    private YamlConfigStore<TestConfig, TestPersisted> store(String fileName, AtomicInteger sanitizations) {
        return new YamlConfigStore<>(
            tempDirectory,
            fileName,
            TestPersisted.class,
            Map.of(),
            TestConfig::new,
            config -> sanitize(config, sanitizations),
            YamlConfigStoreTest::toPersisted,
            YamlConfigStoreTest::fromPersisted
        );
    }

    private static TestConfig sanitize(TestConfig config) {
        return sanitize(config, new AtomicInteger());
    }

    private static TestConfig sanitize(TestConfig config, AtomicInteger sanitizations) {
        sanitizations.incrementAndGet();
        config.value = Math.max(0, Math.min(10, config.value));
        config.sanitized = true;
        return config;
    }

    private static TestPersisted toPersisted(TestConfig config) {
        TestPersisted persisted = new TestPersisted();
        persisted.value = config.value;
        persisted.nested.flag = config.nestedFlag;
        return persisted;
    }

    private static TestConfig fromPersisted(TestPersisted persisted) {
        TestConfig config = new TestConfig();
        config.value = persisted.value;
        config.nestedFlag = persisted.nested.flag;
        return config;
    }

    static final class TestConfig {
        int value = 5;
        boolean nestedFlag = true;
        boolean sanitized;
    }

    static final class TestPersisted {
        int value = 5;
        NestedSection nested = new NestedSection();
    }

    static final class NestedSection {
        boolean flag = true;
    }
}
