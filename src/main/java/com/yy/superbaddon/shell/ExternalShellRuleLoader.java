package com.yy.superbaddon.shell;

import com.yy.superbaddon.SuperbAddonMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class ExternalShellRuleLoader {
    public static final Path DIRECTORY = FMLPaths.CONFIGDIR.get().resolve(SuperbAddonMod.MODID).resolve("shell_ejection");
    public static final String GENERATED_FILE_NAME = "generated_scan.json";
    public static final String GENERATED_DIRECTORY_NAME = "generated_scan";
    public static final Path GENERATED_DIRECTORY = DIRECTORY.resolve(GENERATED_DIRECTORY_NAME);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ExternalShellRuleLoader() {
    }

    public static List<ShellRule> loadExternalRules() {
        ensureDirectory();
        ArrayList<ShellRule> loaded = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(DIRECTORY)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(ExternalShellRuleLoader::isJsonFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            for (Path file : files) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    loaded.addAll(ShellRuleParser.parse(ruleId(file), element));
                } catch (RuntimeException | IOException exception) {
                    SuperbAddonMod.LOGGER.error("Failed to load external shell ejection config {}", file, exception);
                }
            }
        } catch (IOException exception) {
            SuperbAddonMod.LOGGER.error("Failed to scan external shell ejection config folder {}", DIRECTORY, exception);
        }

        return List.copyOf(loaded);
    }

    public static Path writeGeneratedScan(Map<String, JsonObject> files) throws IOException {
        ensureDirectory();
        deleteGeneratedOutput();
        Files.createDirectories(GENERATED_DIRECTORY);

        Path generatedRoot = GENERATED_DIRECTORY.toAbsolutePath().normalize();
        for (Map.Entry<String, JsonObject> entry : files.entrySet()) {
            Path output = GENERATED_DIRECTORY.resolve(entry.getKey()).toAbsolutePath().normalize();
            if (!output.startsWith(generatedRoot)) {
                throw new IOException("Generated shell ejection path escaped output folder: " + entry.getKey());
            }
            if (!output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                throw new IOException("Generated shell ejection path is not a json file: " + entry.getKey());
            }
            Path parent = output.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                GSON.toJson(entry.getValue(), writer);
            }
        }
        return GENERATED_DIRECTORY;
    }

    public static void ensureDirectory() {
        try {
            Files.createDirectories(DIRECTORY);
        } catch (IOException exception) {
            SuperbAddonMod.LOGGER.error("Failed to create shell ejection config folder {}", DIRECTORY, exception);
        }
    }

    private static boolean isJsonFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static void deleteGeneratedOutput() throws IOException {
        Files.deleteIfExists(DIRECTORY.resolve(GENERATED_FILE_NAME));
        if (!Files.exists(GENERATED_DIRECTORY)) return;

        try (Stream<Path> stream = Files.walk(GENERATED_DIRECTORY)) {
            List<Path> paths = stream
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static ResourceLocation ruleId(Path file) {
        String rawPath = DIRECTORY.relativize(file).toString().replace('\\', '/');
        if (rawPath.endsWith(".json")) rawPath = rawPath.substring(0, rawPath.length() - ".json".length());

        StringBuilder sanitized = new StringBuilder("external/");
        for (int i = 0; i < rawPath.length(); i++) {
            char c = Character.toLowerCase(rawPath.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '_' || c == '-' || c == '.') {
                sanitized.append(c);
            } else {
                sanitized.append('_');
            }
        }
        return new ResourceLocation(SuperbAddonMod.MODID, sanitized.toString());
    }
}
