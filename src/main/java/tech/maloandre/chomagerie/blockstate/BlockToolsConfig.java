package tech.maloandre.chomagerie.blockstate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import tech.maloandre.chomagerie.Chomagerie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Server policy adapted from JustS-js/debugStickSurvival (CC0). Denials always win. */
public final class BlockToolsConfig {
    private static BlockToolsConfig instance = new BlockToolsConfig();
    public DebugStick debugStick = new DebugStick();
    public Locking blockStateLocking = new Locking();

    public static BlockToolsConfig get() { return instance; }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("chomagerie/block-tools.json");
    }

    public static void load() {
        try {
            Files.createDirectories(path().getParent());
            if (!Files.exists(path())) {
                Files.writeString(path(), new GsonBuilder().setPrettyPrinting().create().toJson(new BlockToolsConfig()));
            }
            apply(read(path()));
        } catch (Exception e) {
            // Startup has no known-good policy: fail closed without overwriting the broken file.
            instance = new BlockToolsConfig();
            instance.debugStick.enabled = false;
            instance.debugStick.craftingEnabled = false;
            instance.blockStateLocking.enabled = false;
            Chomagerie.LOGGER.error("Invalid block-tools configuration; block tools disabled. File preserved: {}", path(), e);
        }
    }

    /** Parse and validate a candidate without modifying the active policy or the file. */
    public static BlockToolsConfig read(Path path) throws IOException {
        var gson = new GsonBuilder().setStrictness(Strictness.STRICT).create();
        JsonElement json = gson.fromJson(Files.readString(path), JsonElement.class);
        JsonObject root = object(json, "$");
        fields(root, "$", Set.of("debugStick", "blockStateLocking"));
        if (root.has("debugStick")) {
            JsonObject debug = object(root.get("debugStick"), "debugStick");
            fields(debug, "debugStick", Set.of("enabled", "craftingEnabled", "whitelistMode", "allowedBlocks",
                    "forbiddenBlocks", "allowedTags", "forbiddenTags", "allowedProperties", "forbiddenProperties"));
            booleans(debug, "debugStick", "enabled", "craftingEnabled", "whitelistMode");
            for (String name : List.of("allowedBlocks", "forbiddenBlocks", "allowedTags", "forbiddenTags")) {
                if (!debug.has(name)) continue;
                JsonElement entries = debug.get(name);
                String at = "debugStick." + name;
                require(entries.isJsonArray(), at, "une liste de règles est attendue");
                int index = 0;
                for (JsonElement entry : entries.getAsJsonArray()) {
                    String rulePath = at + "[" + index++ + "]";
                    JsonObject rule = object(entry, rulePath);
                    fields(rule, rulePath, Set.of("id", "properties"));
                    String id = string(rule.get("id"), rulePath + ".id");
                    try { Identifier.parse(id); }
                    catch (RuntimeException e) { throw invalid(rulePath + ".id", "identifiant invalide : " + id); }
                    properties(rule.get("properties"), rulePath + ".properties");
                }
            }
            for (String name : List.of("allowedProperties", "forbiddenProperties")) {
                if (debug.has(name)) properties(debug.get(name), "debugStick." + name);
            }
        }
        if (root.has("blockStateLocking")) {
            JsonObject locking = object(root.get("blockStateLocking"), "blockStateLocking");
            // allowedBlocks was removed in 1.6.0; tolerate it in existing configuration files.
            fields(locking, "blockStateLocking", Set.of("enabled", "consumeHoneycomb", "allowAxeUnlock",
                    "allowDebugStickEditing", "showFeedbackMessages", "playEffects", "allowedBlocks"));
            booleans(locking, "blockStateLocking", "enabled", "consumeHoneycomb", "allowAxeUnlock",
                    "allowDebugStickEditing", "showFeedbackMessages", "playEffects");
        }
        return gson.fromJson(root, BlockToolsConfig.class);
    }

    /** Called on the server thread after the entire candidate has been validated. */
    public static void apply(BlockToolsConfig candidate) { instance = Objects.requireNonNull(candidate); }

    private static JsonObject object(JsonElement value, String at) {
        require(value != null && value.isJsonObject(), at, "un objet JSON est attendu");
        return value.getAsJsonObject();
    }

    private static void fields(JsonObject object, String at, Set<String> known) {
        for (String name : object.keySet()) require(known.contains(name), at + "." + name, "option inconnue");
    }

    private static void booleans(JsonObject object, String at, String... names) {
        for (String name : names) {
            if (!object.has(name)) continue;
            var value = object.get(name);
            require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean(),
                    at + "." + name, "true ou false est attendu (sans guillemets)");
        }
    }

    private static String string(JsonElement value, String at) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(),
                at, "une chaîne de caractères est attendue");
        return value.getAsString();
    }

    private static void properties(JsonElement value, String at) {
        for (var entry : object(value, at).entrySet()) {
            String propertyPath = at + "." + entry.getKey();
            require(!entry.getKey().isBlank(), propertyPath, "le nom de propriété est vide");
            require(entry.getValue().isJsonArray(), propertyPath, "une liste de valeurs est attendue");
            int index = 0;
            for (JsonElement item : entry.getValue().getAsJsonArray()) string(item, propertyPath + "[" + index++ + "]");
        }
    }

    private static void require(boolean condition, String at, String reason) {
        if (!condition) throw invalid(at, reason);
    }

    private static IllegalArgumentException invalid(String at, String reason) {
        return new IllegalArgumentException(at + " : " + reason);
    }

    public record Entry(String id, Map<String, List<String>> properties) {
        public Entry(String id) { this(id, Map.of()); }
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
