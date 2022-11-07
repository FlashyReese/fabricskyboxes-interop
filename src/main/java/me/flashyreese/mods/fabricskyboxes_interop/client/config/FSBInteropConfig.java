package me.flashyreese.mods.fabricskyboxes_interop.client.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class FSBInteropConfig {
    private static final Logger logger = LogManager.getLogger("FSB-Interop Config");
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static final FSBInteropConfig INSTANCE = FSBInteropConfig.load(FabricLoader.getInstance().getConfigDir().resolve("fsb-interop.json").toFile());

    private File file;

    public boolean interoperability = true;
    public boolean debugMode = false;
    public boolean preferFSBNative = true;
    public boolean processOptiFine = true;
    public boolean processMCPatcher = false;

    public static FSBInteropConfig load(File file) {
        FSBInteropConfig config;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = GSON.fromJson(reader, FSBInteropConfig.class);
            } catch (Exception e) {
                logger.error("Could not parse config, falling back to defaults!", e);
                config = new FSBInteropConfig();
            }
        } else {
            config = new FSBInteropConfig();
        }

        config.file = file;
        config.writeChanges();

        return config;
    }

    public void writeChanges() {
        File dir = this.file.getParentFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create parent directories");
            }
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("The parent file is not a directory");
        }

        try (FileWriter writer = new FileWriter(this.file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException("Could not save configuration file", e);
        }
    }
}
