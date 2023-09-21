package me.flashyreese.mods.fabricskyboxes_interop.client.config;

import me.flashyreese.mods.fabricskyboxes_interop.FSBInterop;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FSBInteropConfigScreen extends Screen {
    private final Screen parent;
    private final FSBInteropConfig config;
    private final Logger logger = LoggerFactory.getLogger("FSB-Interop");

    public FSBInteropConfigScreen(Screen parent, FSBInteropConfig config) {
        super(Text.translatable(getTranslationKey("title")));
        this.parent = parent;
        this.config = config;
    }

    private static String getTranslationKey(String optionKey) {
        return "options.fsb-interop." + optionKey;
    }

    private static String getTooltipKey(String translationKey) {
        return translationKey + ".tooltip";
    }

    @Override
    protected void init() {
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 - 110, this.height / 2 - 10 - 12, 200, 20, "interoperability", value -> config.interoperability = value, () -> config.interoperability, () -> MinecraftClient.getInstance().reloadResources()));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 + 110, this.height / 2 - 10 - 12, 200, 20, "prefer_fsb_native", value -> config.preferFSBNative = value, () -> config.preferFSBNative, this::reloadResourcesIfInterop));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 - 110, this.height / 2 - 10 + 12, 200, 20, "process_optifine", value -> config.processOptiFine = value, () -> config.processOptiFine, this::reloadResourcesIfInterop));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 + 110, this.height / 2 - 10 + 12, 200, 20, "process_mcpatcher", value -> config.processMCPatcher = value, () -> config.processMCPatcher, this::reloadResourcesIfInterop));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 - 110, this.height / 2 - 10 + 36, 200, 20, "debug_mode", value -> config.debugMode = value, () -> config.debugMode, () -> {
        }));
        addDrawableChild(ButtonWidget
                .builder(
                        Text.translatable(getTranslationKey("dump_data")),
                        button -> {
                            Path path = FabricLoader.getInstance().getGameDir().resolve("fsb-interop-dump");

                            try {
                                // Delete the directory and its contents recursively
                                if (Files.exists(path)) {
                                    if (FSBInteropConfig.INSTANCE.debugMode) {
                                        this.logger.info("Existing dump directory detected. Recursively deleting the contents...");
                                    }
                                    Files.walk(path)
                                            .sorted(Comparator.reverseOrder())
                                            .map(Path::toFile)
                                            .filter(File::delete)
                                            .forEach(file -> {
                                                if (FSBInteropConfig.INSTANCE.debugMode) {
                                                    this.logger.info("Deleted: {}", file.getAbsolutePath());
                                                }
                                            });
                                }
                            } catch (IOException e) {
                                this.logger.error("Error while deleting existing dump directory: {}", e.getMessage());
                                e.printStackTrace();
                            }

                            try {
                                // Create the directory if it doesn't exist
                                if (!Files.exists(path)) {
                                    Files.createDirectories(path);
                                    if (FSBInteropConfig.INSTANCE.debugMode) {
                                        this.logger.info("Dump directory created: {}", path.toAbsolutePath());
                                    }
                                }

                                FSBInterop.getInstance()
                                        .getConvertedSkyMap()
                                        .forEach((identifier, json) -> {
                                            String filename = identifier.toString();
                                            // Fixme: Replace all characters that are not allowed in file names with underscores
                                            filename = filename.replaceAll("[^a-zA-Z0-9-_\\.]", "_");

                                            // Write the JSON data to a file
                                            Path output = path.resolve(filename + ".json");
                                            try {
                                                Files.write(output, json.getBytes());
                                                if (FSBInteropConfig.INSTANCE.debugMode) {
                                                    this.logger.info("Successfully dumped {} to {}", identifier, output.toAbsolutePath());
                                                }
                                            } catch (IOException e) {
                                                this.logger.error("Error while dumping {} to {}: {}", identifier, output.toAbsolutePath(), e.getMessage());
                                                e.printStackTrace();
                                            }
                                        });

                                if (FSBInteropConfig.INSTANCE.debugMode) {
                                    this.logger.info("Opening dump directory: {}", path.toAbsolutePath());
                                }
                                Util.getOperatingSystem().open(path.toUri());
                            } catch (IOException e) {
                                this.logger.error("Error while creating dump directory: {}", e.getMessage());
                                e.printStackTrace();
                            }
                        }
                )
                .dimensions(this.width / 2 - 100 + 110, this.height / 2 - 10 + 36, 200, 20)
                .tooltip(Tooltip.of(Text.translatable(getTooltipKey(getTranslationKey("dump_data")))))
                .build()
        );

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    private void reloadResourcesIfInterop() {
        if (this.config.interoperability) {
            this.client.reloadResources();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 30, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void removed() {
        this.config.writeChanges();
    }

    private ButtonWidget createBooleanOptionButton(int x, int y, int width, int height, String key, Consumer<Boolean> consumer, Supplier<Boolean> supplier, Runnable onChange) {
        String translationKey = getTranslationKey(key);
        Text text = Text.translatable(translationKey);
        Text tooltipText = Text.translatable(getTooltipKey(translationKey));
        return ButtonWidget.builder(ScreenTexts.composeToggleText(text, supplier.get()), button -> {
            boolean newValue = !supplier.get();
            button.setMessage(ScreenTexts.composeToggleText(text, newValue));
            consumer.accept(newValue);
            onChange.run();
        }).dimensions(x, y, width, height).tooltip(Tooltip.of(tooltipText)).build();
    }
}
