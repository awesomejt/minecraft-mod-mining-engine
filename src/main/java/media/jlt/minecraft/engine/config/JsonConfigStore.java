package media.jlt.minecraft.engine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class JsonConfigStore<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;
    private final Class<T> configType;
    private final Supplier<T> defaults;
    private final UnaryOperator<T> sanitizer;

    public JsonConfigStore(
        Path configDirectory,
        String fileName,
        Class<T> configType,
        Supplier<T> defaults,
        UnaryOperator<T> sanitizer
    ) {
        this.configFile = Objects.requireNonNull(configDirectory, "configDirectory")
            .resolve(Objects.requireNonNull(fileName, "fileName"));
        this.configType = Objects.requireNonNull(configType, "configType");
        this.defaults = Objects.requireNonNull(defaults, "defaults");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
    }

    public Path configFile() {
        return configFile;
    }

    public ReloadResult<T> load() {
        return reload();
    }

    public ReloadResult<T> reload() {
        try (Reader reader = Files.newBufferedReader(configFile)) {
            T config = GSON.fromJson(reader, configType);
            if (config == null) {
                config = newDefaults();
            }
            config = sanitize(config);
            write(config);
            return ReloadResult.success(config);
        } catch (IOException | JsonParseException exception) {
            if (!Files.exists(configFile)) {
                T config = sanitize(newDefaults());
                try {
                    write(config);
                    return ReloadResult.success(config);
                } catch (IOException writeException) {
                    return ReloadResult.failure(writeException.getMessage());
                }
            }
            return ReloadResult.failure(exception.getMessage());
        }
    }

    private T newDefaults() {
        return Objects.requireNonNull(defaults.get(), "defaults result");
    }

    private T sanitize(T config) {
        return Objects.requireNonNull(sanitizer.apply(config), "sanitizer result");
    }

    private void write(T config) throws IOException {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(config, writer);
        }
    }

    public record ReloadResult<T>(T config, String errorMessage) {
        public static <T> ReloadResult<T> success(T config) {
            return new ReloadResult<>(Objects.requireNonNull(config, "config"), null);
        }

        public static <T> ReloadResult<T> failure(String errorMessage) {
            return new ReloadResult<>(
                null,
                errorMessage == null || errorMessage.isBlank()
                    ? "Unknown configuration error"
                    : errorMessage
            );
        }

        public boolean success() {
            return config != null;
        }
    }
}
