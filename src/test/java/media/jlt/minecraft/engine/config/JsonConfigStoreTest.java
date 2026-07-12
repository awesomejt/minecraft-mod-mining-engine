package media.jlt.minecraft.engine.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonConfigStoreTest {
    @TempDir
    Path tempDirectory;

    @Test
    void missingConfigIsCreatedFromSanitizedDefaults() throws IOException {
        AtomicInteger sanitizations = new AtomicInteger();
        JsonConfigStore<TestConfig> store = store("nested/test.json", sanitizations);

        JsonConfigStore.ReloadResult<TestConfig> result = store.load();

        assertTrue(result.success());
        assertEquals(5, result.config().value);
        assertTrue(result.config().sanitized);
        assertEquals(1, sanitizations.get());
        assertTrue(Files.exists(store.configFile()));
        assertTrue(Files.readString(store.configFile()).contains("\"sanitized\": true"));
    }

    @Test
    void validConfigIsSanitizedAndRewritten() throws IOException {
        AtomicInteger sanitizations = new AtomicInteger();
        JsonConfigStore<TestConfig> store = store("test.json", sanitizations);
        Files.writeString(store.configFile(), "{ \"value\": 999 }");

        JsonConfigStore.ReloadResult<TestConfig> result = store.reload();

        assertTrue(result.success());
        assertEquals(10, result.config().value);
        assertTrue(result.config().sanitized);
        assertEquals(1, sanitizations.get());
        String rewritten = Files.readString(store.configFile());
        assertTrue(rewritten.contains("\"value\": 10"));
        assertTrue(rewritten.contains("\"sanitized\": true"));
    }

    @Test
    void malformedConfigReturnsFailureWithoutOverwritingInput() throws IOException {
        JsonConfigStore<TestConfig> store = store("test.json", new AtomicInteger());
        String malformed = "{ \"value\": tru";
        Files.writeString(store.configFile(), malformed);

        JsonConfigStore.ReloadResult<TestConfig> result = store.reload();

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertEquals(malformed, Files.readString(store.configFile()));
    }

    @Test
    void emptyAndJsonNullConfigsRegenerateDefaults() throws IOException {
        JsonConfigStore<TestConfig> emptyStore = store("empty.json", new AtomicInteger());
        JsonConfigStore<TestConfig> nullStore = store("null.json", new AtomicInteger());
        Files.writeString(emptyStore.configFile(), "");
        Files.writeString(nullStore.configFile(), "null");

        JsonConfigStore.ReloadResult<TestConfig> emptyResult = emptyStore.reload();
        JsonConfigStore.ReloadResult<TestConfig> nullResult = nullStore.reload();

        assertTrue(emptyResult.success());
        assertTrue(nullResult.success());
        assertEquals(5, emptyResult.config().value);
        assertEquals(5, nullResult.config().value);
        assertTrue(Files.readString(emptyStore.configFile()).contains("\"value\": 5"));
        assertTrue(Files.readString(nullStore.configFile()).contains("\"value\": 5"));
    }

    @Test
    void blockingParentReturnsFailureWithoutChangingIt() throws IOException {
        Path blockingFile = tempDirectory.resolve("not-a-directory");
        Files.writeString(blockingFile, "keep me");
        JsonConfigStore<TestConfig> store = new JsonConfigStore<>(
            blockingFile,
            "test.json",
            TestConfig.class,
            TestConfig::new,
            JsonConfigStoreTest::sanitize
        );

        JsonConfigStore.ReloadResult<TestConfig> result = store.reload();

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertEquals("keep me", Files.readString(blockingFile));
    }

    @Test
    void failureFactoryAlwaysProvidesUsefulErrorText() {
        JsonConfigStore.ReloadResult<TestConfig> nullMessage = JsonConfigStore.ReloadResult.failure(null);
        JsonConfigStore.ReloadResult<TestConfig> blankMessage = JsonConfigStore.ReloadResult.failure("  ");

        assertEquals("Unknown configuration error", nullMessage.errorMessage());
        assertEquals("Unknown configuration error", blankMessage.errorMessage());
        assertFalse(nullMessage.success());
    }

    private JsonConfigStore<TestConfig> store(String fileName, AtomicInteger sanitizations) {
        return new JsonConfigStore<>(
            tempDirectory,
            fileName,
            TestConfig.class,
            TestConfig::new,
            config -> {
                sanitizations.incrementAndGet();
                return sanitize(config);
            }
        );
    }

    private static TestConfig sanitize(TestConfig config) {
        config.value = Math.max(0, Math.min(10, config.value));
        config.sanitized = true;
        return config;
    }

    static final class TestConfig {
        int value = 5;
        boolean sanitized;
    }
}
