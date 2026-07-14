package media.jlt.minecraft.engine.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.FieldProperty;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Persists a config type {@code T} as nested, human-edited YAML shaped like {@code P}, with
 * {@code T <-> P} conversion supplied by the caller since the on-disk grouping is mod-specific.
 * Dumps preserve field declaration order (SnakeYAML's default bean representer does not) and
 * attach a block comment above each key found in {@code docsByDottedPath}, keyed by
 * dot-joined field path (e.g. {@code "economy.durability.multiplier"}).
 */
public final class YamlConfigStore<T, P> {
    private final Path configFile;
    private final Class<P> persistedType;
    private final Supplier<T> defaults;
    private final UnaryOperator<T> sanitizer;
    private final Function<T, P> toPersisted;
    private final Function<P, T> fromPersisted;
    private final Yaml yaml;

    public YamlConfigStore(
        Path configDirectory,
        String fileName,
        Class<P> persistedType,
        Map<String, String> docsByDottedPath,
        Supplier<T> defaults,
        UnaryOperator<T> sanitizer,
        Function<T, P> toPersisted,
        Function<P, T> fromPersisted
    ) {
        this.configFile = Objects.requireNonNull(configDirectory, "configDirectory")
            .resolve(Objects.requireNonNull(fileName, "fileName"));
        this.persistedType = Objects.requireNonNull(persistedType, "persistedType");
        this.defaults = Objects.requireNonNull(defaults, "defaults");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.toPersisted = Objects.requireNonNull(toPersisted, "toPersisted");
        this.fromPersisted = Objects.requireNonNull(fromPersisted, "fromPersisted");
        this.yaml = createYaml(persistedType, Objects.requireNonNull(docsByDottedPath, "docsByDottedPath"));
    }

    public Path configFile() {
        return configFile;
    }

    public ReloadResult<T> load() {
        return reload();
    }

    public ReloadResult<T> reload() {
        try (Reader reader = Files.newBufferedReader(configFile)) {
            Object loaded = yaml.load(reader);
            T config = loaded == null ? newDefaults() : fromPersisted.apply(persistedType.cast(loaded));
            config = sanitize(config);
            write(config);
            return ReloadResult.success(config);
        } catch (Exception exception) {
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

    /** Writes {@code config} through the same sanitize-then-dump path {@link #reload()} uses. */
    public void save(T config) throws IOException {
        write(sanitize(config));
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
            yaml.dump(toPersisted.apply(config), writer);
        }
    }

    private static <P> Yaml createYaml(Class<P> persistedType, Map<String, String> docsByDottedPath) {
        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(persistedType, loaderOptions);
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setBeanAccess(BeanAccess.FIELD);
        propertyUtils.setSkipMissingProperties(true);
        constructor.setPropertyUtils(propertyUtils);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setProcessComments(true);

        DocumentedRepresenter representer = new DocumentedRepresenter(dumperOptions, docsByDottedPath);
        representer.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);

        return new Yaml(constructor, representer, dumperOptions);
    }

    /**
     * Dumps bean properties in field-declaration order (instead of SnakeYAML's default, which
     * does not preserve it) and attaches a block comment above each key present in
     * {@code docsByDottedPath}, tracking nesting depth so nested beans resolve dotted paths
     * like {@code "section.field"}.
     */
    private static final class DocumentedRepresenter extends Representer {
        private static final Mark BLANK_MARK = new Mark("", 0, 0, 0, new char[0], 0);

        private final Map<String, String> docsByDottedPath;
        private final Deque<String> path = new ArrayDeque<>();

        DocumentedRepresenter(DumperOptions dumperOptions, Map<String, String> docsByDottedPath) {
            super(dumperOptions);
            this.docsByDottedPath = docsByDottedPath;
        }

        @Override
        protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
            Set<Property> declarationOrder = new LinkedHashSet<>();
            for (Field field : javaBean.getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    declarationOrder.add(new FieldProperty(field));
                }
            }
            MappingNode node = super.representJavaBean(declarationOrder, javaBean);
            // Bean-typed nodes otherwise carry a Java class tag (e.g. "!!com.example.Foo");
            // every nested group here is a plain mapping as far as the file is concerned.
            node.setTag(Tag.MAP);
            return node;
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
                Object propertyValue, Tag customTag) {
            path.addLast(property.getName());
            String dottedPath = String.join(".", path);
            NodeTuple tuple;
            try {
                tuple = super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
            } finally {
                path.removeLast();
            }
            if (tuple != null) {
                String doc = docsByDottedPath.get(dottedPath);
                if (doc != null) {
                    tuple.getKeyNode().setBlockComments(
                        List.of(new CommentLine(BLANK_MARK, BLANK_MARK, " " + doc, CommentType.BLOCK)));
                }
            }
            return tuple;
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
