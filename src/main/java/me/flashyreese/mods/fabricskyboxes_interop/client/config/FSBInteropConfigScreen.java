package me.flashyreese.mods.fabricskyboxes_interop.client.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FSBInteropConfigScreen extends Screen {
    private final Screen parent;
    private final FSBInteropConfig config;

    public FSBInteropConfigScreen(Screen parent, FSBInteropConfig config) {
        super(new TranslatableText(getTranslationKey("title")));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        addDrawableChild(createBooleanOptionButton(width / 2 - 100 - 110, height / 2 - 10 - 12, 200, 20, "interoperability", value -> config.interoperability = value, () -> config.interoperability, () -> MinecraftClient.getInstance().reloadResources()));
        addDrawableChild(createBooleanOptionButton(width / 2 - 100 + 110, height / 2 - 10 - 12, 200, 20, "clear_fsb_format_sky", value -> config.clearFSBFormatSky = value, () -> config.clearFSBFormatSky, () -> MinecraftClient.getInstance().reloadResources()));
        addDrawableChild(createBooleanOptionButton(width / 2 - 100 - 110, height / 2 - 10 + 12, 200, 20, "debug_mode", value -> config.debugMode = value, () -> config.debugMode, () -> {}));

        addDrawableChild(new ButtonWidget(width / 2 - 100, height - 40, 200, 20, ScreenTexts.DONE, button -> close()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, title, width / 2, 30, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        config.writeChanges();
    }

    private static String getTranslationKey(String optionKey) {
        return "options.fsb-interop." + optionKey;
    }

    private static String getTooltipKey(String translationKey) {
        return translationKey + ".tooltip";
    }

    private ButtonWidget.TooltipSupplier createDefaultTooltipSupplier(StringVisitable text) {
        return (button, matrices, mouseX, mouseY) -> {
            renderOrderedTooltip(matrices, textRenderer.wrapLines(text, width / 100 * 100 / 2), mouseX, mouseY);
        };
    }

    private ButtonWidget createBooleanOptionButton(int x, int y, int width, int height, String key, Consumer<Boolean> consumer, Supplier<Boolean> supplier, Runnable onChange) {
        String translationKey = getTranslationKey(key);
        Text text = new TranslatableText(translationKey);
        Text tooltipText = new TranslatableText(getTooltipKey(translationKey));
        return new ButtonWidget(x, y, width, height, ScreenTexts.composeToggleText(text, supplier.get()),
                button -> {
                    boolean newValue = !supplier.get();
                    button.setMessage(ScreenTexts.composeToggleText(text, newValue));
                    consumer.accept(newValue);
                    onChange.run();
                },
                createDefaultTooltipSupplier(tooltipText)
        );
    }
}
