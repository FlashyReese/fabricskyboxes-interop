package me.flashyreese.mods.fabricskyboxes_interop.client.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FSBInteropConfigScreen extends Screen {
    private final Screen parent;
    private final FSBInteropConfig config;

    public FSBInteropConfigScreen(Screen parent, FSBInteropConfig config) {
        super(Text.translatable(getTranslationKey("title")));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 - 110, this.height / 2 - 10 - 12, 200, 20, "interoperability", value -> config.interoperability = value, () -> config.interoperability, () -> MinecraftClient.getInstance().reloadResources()));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 + 110, this.height / 2 - 10 - 12, 200, 20, "prefer_fsb_native", value -> config.preferFSBNative = value, () -> config.preferFSBNative, this::reloadResourcesIfInterop));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 - 110, this.height / 2 - 10 + 12, 200, 20, "process_optifine", value -> config.processOptiFine = value, () -> config.processOptiFine, this::reloadResourcesIfInterop));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 + 110, this.height / 2 - 10 + 12, 200, 20, "process_mcpatcher", value -> config.processMCPatcher = value, () -> config.processMCPatcher, this::reloadResourcesIfInterop));
        addDrawableChild(createBooleanOptionButton(this.width / 2 - 100 - 110, this.height / 2 - 10 + 36, 200, 20, "debug_mode", value -> config.debugMode = value, () -> config.debugMode, () -> {}));

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    private void reloadResourcesIfInterop() {
        if (this.config.interoperability) {
            this.client.reloadResources();
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
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

    private static String getTranslationKey(String optionKey) {
        return "options.fsb-interop." + optionKey;
    }

    private static String getTooltipKey(String translationKey) {
        return translationKey + ".tooltip";
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
