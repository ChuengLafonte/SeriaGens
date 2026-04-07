package id.seria.gens.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&x&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])");

    /**
     * Parses a string into a Component, supporting:
     * 1. MiniMessage: <red>text</red>
     * 2. Hex: &#rrggbb and <#rrggbb>
     * 3. Legacy: &a and §a
     * 4. Bungee Hex: &x&r&r&g&g&b&b
     */
    public static @NotNull Component parse(@Nullable String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Convert &#rrggbb (Hex) to <#rrggbb> for MiniMessage
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();

        // 2. Convert &x&r&r&g&g&b&b (Bungee Hex) to <#rrggbb>
        Matcher legacyHexMatcher = LEGACY_HEX_PATTERN.matcher(text);
        sb = new StringBuilder();
        while (legacyHexMatcher.find()) {
            legacyHexMatcher.appendReplacement(sb, "<#" + 
                legacyHexMatcher.group(1) + legacyHexMatcher.group(2) + 
                legacyHexMatcher.group(3) + legacyHexMatcher.group(4) + 
                legacyHexMatcher.group(5) + legacyHexMatcher.group(6) + ">");
        }
        legacyHexMatcher.appendTail(sb);
        text = sb.toString();

        // 3. MiniMessage handles <#rrggbb> and standard tags.
        // To support legacy &a, we replace & with legacy tags if MiniMessage isn't enough.
        // Paper's MiniMessage doesn't natively handle &a unless we use TagResolvers.
        
        // A common trick is to use Legacy serializer to Component first, then serialize to MiniMessage tags, then parse.
        // Or just replace standard & codes with MiniMessage tags.
        
        text = text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&l", "<bold>")
                   .replace("&m", "<strikethrough>")
                   .replace("&n", "<underline>")
                   .replace("&o", "<italic>")
                   .replace("&r", "<reset>");

        // Handle § as well for completeness
        text = text.replace("§0", "<black>")
                   .replace("§1", "<dark_blue>")
                   .replace("§2", "<dark_green>")
                   .replace("§3", "<dark_aqua>")
                   .replace("§4", "<dark_red>")
                   .replace("§5", "<dark_purple>")
                   .replace("§6", "<gold>")
                   .replace("§7", "<gray>")
                   .replace("§8", "<dark_gray>")
                   .replace("§9", "<blue>")
                   .replace("§a", "<green>")
                   .replace("§b", "<aqua>")
                   .replace("§c", "<red>")
                   .replace("§d", "<light_purple>")
                   .replace("§e", "<yellow>")
                   .replace("§f", "<white>")
                   .replace("§l", "<bold>")
                   .replace("§m", "<strikethrough>")
                   .replace("§n", "<underline>")
                   .replace("§o", "<italic>")
                   .replace("§r", "<reset>");

        return MINI_MESSAGE.deserialize(text);
    }
    
    public static String toLegacy(String text) {
        return LEGACY_SERIALIZER.serialize(parse(text));
    }

    public static List<Component> parseList(List<String> list) {
        if (list == null) return new ArrayList<>();
        List<Component> components = new ArrayList<>();
        for (String line : list) {
            components.add(parse(line));
        }
        return components;
    }
}
