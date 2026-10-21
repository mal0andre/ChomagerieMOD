package tech.maloandre.chomagerie.blockstate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import tech.maloandre.chomagerie.Chomagerie;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Server policy adapted from JustS-js/debugStickSurvival (CC0). Denials always win. */
public final class BlockToolsConfig {
    private static BlockToolsConfig instance = new BlockToolsConfig();
    public DebugStick debugStick = new DebugStick();
    public Locking blockStateLocking = new Locking();

    public static BlockToolsConfig get() { return instance; }

    public static void load() {
        var path = FabricLoader.getInstance().getConfigDir().resolve("chomagerie/block-tools.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                BlockToolsConfig loaded = gson.fromJson(Files.readString(path), BlockToolsConfig.class);
                Objects.requireNonNull(loaded).validate();
                instance = loaded;
            } else {
                Files.writeString(path, gson.toJson(instance));
            }
        } catch (Exception e) {
            // Never replace a broken restriction file with a more permissive configuration.
            instance = new BlockToolsConfig();
            instance.debugStick.enabled = false;
            instance.debugStick.craftingEnabled = false;
            instance.blockStateLocking.enabled = false;
            Chomagerie.LOGGER.error("Invalid block-tools configuration; block tools disabled. File preserved: {}", path, e);
        }
    }

    private void validate() {
        Objects.requireNonNull(debugStick);
        Objects.requireNonNull(blockStateLocking);
        debugStick.allowedBlocks.forEach(Entry::validate);
        debugStick.forbiddenBlocks.forEach(Entry::validate);
        debugStick.allowedTags.forEach(Entry::validate);
        debugStick.forbiddenTags.forEach(Entry::validate);
        validateProperties(debugStick.allowedProperties);
        validateProperties(debugStick.forbiddenProperties);
    }

    private static void validateProperties(Map<String, List<String>> properties) {
        properties.forEach((key, values) -> {
            Objects.requireNonNull(key);
            values.forEach(Objects::requireNonNull);
        });
    }

    public record Entry(String id, Map<String, List<String>> properties) {
        public Entry(String id) { this(id, Map.of()); }
        void validate() { Identifier.parse(id); validateProperties(properties); }
        boolean matches(BlockState state, boolean tag) {
            return tag ? state.is(TagKey.create(Registries.BLOCK, Identifier.parse(id)))
                    : state.typeHolder().getRegisteredName().equals(Identifier.parse(id).toString());
        }
    }

    public static final class DebugStick {
        public boolean enabled = true;
        public boolean craftingEnabled = true;
        public boolean whitelistMode = false;
        public List<Entry> allowedBlocks = List.of();
        public List<Entry> forbiddenBlocks = List.of();
        public List<Entry> allowedTags = List.of();
        public List<Entry> forbiddenTags = List.of();
        public Map<String, List<String>> allowedProperties = Map.of();
        public Map<String, List<String>> forbiddenProperties = Map.of();

        public boolean allows(BlockState state, String property, String value) {
            if (denies(forbiddenProperties, property, value)) return false;
            if (forbiddenBlocks.stream().anyMatch(e -> e.matches(state, false) && entryDenies(e, property, value))) return false;
            if (forbiddenTags.stream().anyMatch(e -> e.matches(state, true) && entryDenies(e, property, value))) return false;
            // Explicit block rules take precedence over tag grants. Matching allow lists constrain values,
            // including in blacklist mode; an unrelated grant cannot widen an explicit value list.
            var blocks = allowedBlocks.stream().filter(e -> e.matches(state, false)).toList();
            if (!blocks.isEmpty()) return blocks.stream().allMatch(e -> grants(e.properties, property, value, true));
            var tags = allowedTags.stream().filter(e -> e.matches(state, true)).toList();
            if (!tags.isEmpty()) return tags.stream().allMatch(e -> grants(e.properties, property, value, true));
            if (allowedProperties.containsKey(property) || allowedProperties.containsKey("all"))
                return grants(allowedProperties, property, value, false);
            return !whitelistMode;
        }

        private static boolean entryDenies(Entry e, String property, String value) {
            return e.properties.isEmpty() || denies(e.properties, property, value);
        }
        private static boolean denies(Map<String, List<String>> rules, String property, String value) {
            return rules.containsKey("all") || rules.getOrDefault(property, List.of()).stream()
                    .anyMatch(v -> v.equals("all") || v.equals(value));
        }
        private static boolean grants(Map<String, List<String>> rules, String property, String value, boolean emptyGrants) {
            return (emptyGrants && rules.isEmpty()) || rules.containsKey("all")
                    || rules.getOrDefault(property, List.of()).stream().anyMatch(v -> v.equals("all") || v.equals(value));
        }
    }

    public static final class Locking {
        public boolean enabled = true;
        public boolean consumeHoneycomb = true;
        public boolean allowAxeUnlock = true;
        public boolean allowDebugStickEditing = true;
        public boolean showFeedbackMessages = true;
        public boolean playEffects = true;
        // Only states with a value the Debug Stick can cycle need a new lock.
        public boolean allows(BlockState state) {
            return !state.isAir() && state.getProperties().stream()
                    .anyMatch(property -> property.getPossibleValues().size() > 1);
        }
    }
}
