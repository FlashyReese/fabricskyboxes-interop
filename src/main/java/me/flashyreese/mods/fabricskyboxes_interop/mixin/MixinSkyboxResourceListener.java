package me.flashyreese.mods.fabricskyboxes_interop.mixin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.amerebagatelle.fabricskyboxes.SkyboxManager;
import io.github.amerebagatelle.fabricskyboxes.resource.SkyboxResourceListener;
import io.github.amerebagatelle.fabricskyboxes.util.object.MinMaxEntry;
import me.flashyreese.mods.fabricskyboxes_interop.client.config.FSBInteropConfig;
import me.flashyreese.mods.fabricskyboxes_interop.utils.ResourceManagerHelper;
import me.flashyreese.mods.fabricskyboxes_interop.utils.Utils;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(SkyboxResourceListener.class)
public class MixinSkyboxResourceListener {
    private final Logger logger = LoggerFactory.getLogger("FSB-Interop");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String OPTIFINE_SKY_PARENT = "optifine/sky";
    private static final Pattern OPTIFINE_SKY_PATTERN = Pattern.compile("optifine/sky/(?<world>\\w+)/(?<name>\\w+).properties$");
    private static final String MCPATCHER_SKY_PARENT = "mcpatcher/sky";
    private static final Pattern MCPATCHER_SKY_PATTERN = Pattern.compile("mcpatcher/sky/(?<world>\\w+)/(?<name>\\w+).properties$");

    @Inject(method = "reload", at = @At(value = "TAIL"))
    public void reload(ResourceManager manager, CallbackInfo ci) {
        if (FSBInteropConfig.INSTANCE.interoperability) {
            if (FSBInteropConfig.INSTANCE.clearFSBFormatSky) {
                this.logger.warn("Removing existing FSB skies...");
                SkyboxManager.getInstance().clearSkyboxes();
            }
            this.logger.info("Looking for OptiFine/MCPatcher Skies...");
            this.convert(new ResourceManagerHelper(manager));
        }
    }

    public void convert(ResourceManagerHelper managerAccessor) {
        this.convertNamespace(managerAccessor, OPTIFINE_SKY_PARENT, OPTIFINE_SKY_PATTERN);
        this.convertNamespace(managerAccessor, MCPATCHER_SKY_PARENT, MCPATCHER_SKY_PATTERN);
    }

    /**
     * Converts a specific namespace
     *
     * @param resourceManagerHelper The resource manager helper
     * @param skyParent             The parent namespace
     * @param pattern               The pattern for namespace
     */
    private void convertNamespace(ResourceManagerHelper resourceManagerHelper, String skyParent, Pattern pattern) {
        resourceManagerHelper.getNamespaces().stream()
                .map(namespace -> new Identifier(namespace, skyParent))
                .forEach(parent -> resourceManagerHelper.searchIn(parent)
                        .filter(id -> id.getPath().endsWith(".properties"))
                        .forEach(id -> {
                            Matcher matcher = pattern.matcher(id.getPath());
                            if (matcher.find()) {
                                String world = matcher.group("world");
                                String name = matcher.group("name");

                                if (world == null || name == null)
                                    return;

                                this.logger.info("Converting {} to FSB Format...", id);

                                InputStream inputStream = resourceManagerHelper.getInputStream(id);
                                if (inputStream == null) {
                                    if (FSBInteropConfig.INSTANCE.debugMode) {
                                        this.logger.error("Error trying to read namespaced identifier: {}", id);
                                    }
                                    return;
                                }

                                Properties properties = new Properties();
                                try {
                                    properties.load(inputStream);
                                } catch (IOException e) {
                                    if (FSBInteropConfig.INSTANCE.debugMode) {
                                        this.logger.error("Error trying to read namespaced identifier: {}", id);
                                        e.printStackTrace();
                                    }
                                    return;
                                } finally {
                                    try {
                                        inputStream.close();
                                    } catch (IOException e) {
                                        if (FSBInteropConfig.INSTANCE.debugMode) {
                                            this.logger.error("Error trying to close input stream at namespaced identifier: {}", id);
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                Identifier textureId;
                                if (properties.containsKey("source")) {
                                    String source = properties.getProperty("source");
                                    try {
                                        if (source.startsWith("./")) {
                                            textureId = new Identifier(id.getNamespace(), parent.getPath() + String.format("/%s/%s", world, source.substring(2)));
                                        } else if (source.startsWith("assets/")) {
                                            int firstIndex = source.indexOf("/") + 1;
                                            int secondIndex = source.indexOf("/", firstIndex);
                                            String sourceNamespace = source.substring(firstIndex, secondIndex);
                                            textureId = new Identifier(sourceNamespace, source.substring(secondIndex + 1));
                                        } else {
                                            int firstIndex = source.indexOf("/") + 1;
                                            String sourceNamespace = source.substring(0, firstIndex - 1);
                                            textureId = new Identifier(sourceNamespace, source.substring(firstIndex));
                                        }
                                    } catch (InvalidIdentifierException e) {
                                        if (FSBInteropConfig.INSTANCE.debugMode) {
                                            this.logger.error("Illegal character in namespaced identifier: {}", source);
                                        }
                                        return;
                                    }
                                } else {
                                    textureId = new Identifier(id.getNamespace(), parent.getPath() + String.format("/%s/%s.png", world, name));
                                }

                                InputStream textureInputStream = resourceManagerHelper.getInputStream(textureId);
                                if (textureInputStream == null) {
                                    if (FSBInteropConfig.INSTANCE.debugMode) {
                                        this.logger.error("Unable to find/read namespaced identifier: {}", textureId);
                                    }
                                    return;
                                }

                                this.convert(id, textureId, properties, world);
                            }
                        }));
    }

    /**
     * Converts one MCPatcher file to FSB format.
     *
     * @param propertiesId The OptiFine metadata file identifier.
     * @param textureId    The texture file identifier.
     * @param properties   The MCPatcher properties file.
     * @param world        The world name
     */
    private void convert(Identifier propertiesId, Identifier textureId, Properties properties, String world) {
        JsonObject json = new JsonObject();

        json.addProperty("schemaVersion", 2);
        json.addProperty("type", "single-sprite-square-textured");


        JsonObject blend = new JsonObject();
        if (properties.containsKey("blend")) {
            blend.addProperty("type", properties.getProperty("blend"));
        } else {
            blend.addProperty("type", "add");
        }
        json.add("blend", blend);

        json.addProperty("texture", textureId.toString());

        JsonObject propertiesObject = new JsonObject();
        this.processProperties(propertiesObject, properties);
        json.add("properties", propertiesObject);

        JsonObject conditionsObject = new JsonObject();
        this.processConditions(conditionsObject, properties, world);
        json.add("conditions", conditionsObject);

        if (FSBInteropConfig.INSTANCE.debugMode) {
            this.logger.info("Output for {} conversion:\n{}", propertiesId, GSON.toJson(json));
        }

        SkyboxManager.getInstance().addSkybox(propertiesId, json);
        this.logger.info("Converted & Added Skybox from {}!", propertiesId);
    }

    /**
     * Converts MCPatcher Sky Properties to FabricSkyboxes properties
     *
     * @param json       The properties object for FabricSkyboxes
     * @param properties The sky properties
     */
    private void processProperties(JsonObject json, Properties properties) {
        // Convert fade
        JsonObject fade = new JsonObject();
        if (properties.containsKey("startFadeIn") && properties.containsKey("endFadeIn") && properties.containsKey("endFadeOut")) {
            int startFadeIn = Objects.requireNonNull(Utils.toTickTime(properties.getProperty("startFadeIn"))).intValue();
            int endFadeIn = Objects.requireNonNull(Utils.toTickTime(properties.getProperty("endFadeIn"))).intValue();
            int endFadeOut = Objects.requireNonNull(Utils.toTickTime(properties.getProperty("endFadeOut"))).intValue();
            int startFadeOut;
            if (properties.containsKey("startFadeOut")) {
                startFadeOut = Objects.requireNonNull(Utils.toTickTime(properties.getProperty("startFadeOut"))).intValue();
            } else {
                startFadeOut = endFadeOut - (endFadeIn - startFadeIn);
                if (startFadeIn <= startFadeOut && endFadeIn >= startFadeOut) {
                    startFadeOut = endFadeOut;
                }
            }
            fade.addProperty("startFadeIn", Utils.normalizeTickTime(startFadeIn));
            fade.addProperty("endFadeIn", Utils.normalizeTickTime(endFadeIn));
            fade.addProperty("startFadeOut", Utils.normalizeTickTime(startFadeOut));
            fade.addProperty("endFadeOut", Utils.normalizeTickTime(endFadeOut));
        } else {
            fade.addProperty("startFadeIn", 0);
            fade.addProperty("endFadeIn", 0);
            fade.addProperty("startFadeOut", 0);
            fade.addProperty("endFadeOut", 0);
            fade.addProperty("alwaysOn", true);
        }
        json.add("fade", fade);

        // Convert rotation
        if (properties.containsKey("rotate")) {
            json.addProperty("shouldRotate", Boolean.parseBoolean(properties.getProperty("rotate")));
        }

        JsonObject rotation = new JsonObject();
        JsonArray jsonAxis = new JsonArray();
        if (properties.containsKey("axis")) {
            String[] axis = properties.getProperty("axis").split(" ");
            List<String> rev = Arrays.asList(axis);
            Collections.reverse(rev);
            axis = rev.toArray(axis);
            for (String a : axis) {
                jsonAxis.add(Float.parseFloat(a) * 90);
            }
        } else {
            //Default South
            jsonAxis.add(0f);
            jsonAxis.add(0f);
            jsonAxis.add(180f);
        }
        rotation.add("axis", jsonAxis);

        json.add("rotation", rotation);

        if (properties.containsKey("speed")) {
            json.addProperty("rotationSpeed", Float.parseFloat(properties.getProperty("speed")));
        }
    }


    /**
     * Converts properties into conditions object
     *
     * @param json       The conditions object
     * @param properties The sky properties
     * @param world      The world string
     */
    private void processConditions(JsonObject json, Properties properties, String world) {
        // Weather
        if (properties.containsKey("weather")) {
            String[] weathers = properties.getProperty("weather").split(" ");
            if (weathers.length > 0) {
                JsonArray jsonWeather = new JsonArray();
                for (String weather : weathers) {
                    jsonWeather.add(weather);
                }
                json.add("weather", jsonWeather);
            }
        }

        // Biomes
        if (properties.containsKey("biomes")) {
            String[] biomes = properties.getProperty("biomes").split(" ");
            if (biomes.length > 0) {
                JsonArray jsonBiomes = new JsonArray();
                for (String biome : biomes) {
                    jsonBiomes.add(biome);
                }
                json.add("biomes", jsonBiomes);
            }
        }

        // World location -> worlds
        JsonArray worlds = new JsonArray();
        worlds.add(world.equals("world0") ? "minecraft:overworld" : world.equals("world1") ? "minecraft:the_end" : world);
        json.add("worlds", worlds);

        // Heights -> yRanges
        if (properties.containsKey("heights")) {
            List<MinMaxEntry> minMaxEntries = Utils.parseMinMaxEntries(properties.getProperty("heights"));

            if (minMaxEntries.size() > 0) {
                JsonArray jsonYRanges = new JsonArray();
                minMaxEntries.forEach(minMaxEntry -> {
                    JsonObject minMax = new JsonObject();
                    minMax.addProperty("min", minMaxEntry.getMin());
                    minMax.addProperty("max", minMaxEntry.getMax());
                    jsonYRanges.add(minMax);
                });
                json.add("yRanges", jsonYRanges);
            }
        }
    }
}
