package com.uddernetworks.config;

import java.io.File;

public class ConfigOptions {

    private boolean enableAutoReload = false;
    private boolean enableAutoSave = false;
    private String setDefaults = null;
    private File defaultLocation = null;

    public ConfigOptions() {}

    public ConfigOptions(ConfigOptions configOptions) {
        this.enableAutoReload = configOptions.enableAutoReload;
        this.enableAutoSave = configOptions.enableAutoSave;
        this.setDefaults = configOptions.setDefaults;
    }

    /**
     * @return If auto reload is enabled
     */
    public boolean getEnableAutoReload() {
        return enableAutoReload;
    }

    /**
     * Automatically reloads the config when getting any value
     * @param enableAutoReload Weather auto reload should be enabled
     * @return The current ConfigOptions object
     */
    public ConfigOptions enableAutoReload(boolean enableAutoReload) {
        this.enableAutoReload = enableAutoReload;
        return this;
    }

    /**
     * @return If auto save is enabled
     */
    public boolean getEnableAutoSave() {
        return enableAutoSave;
    }

    /**
     * Automatically save saves the config when any value is set to it.
     * @param enableAutoSave Weather auto save should be enabled
     * @return The current ConfigOptions object
     */
    public ConfigOptions enableAutoSave(boolean enableAutoSave) {
        this.enableAutoSave = enableAutoSave;
        return this;
    }


    /**
     * @return Gets the name of the file which the config is getting defaults from
     */
    public String getSetDefaults() {
        return setDefaults;
    }

    /**
     * Sets the defaults of the current config to the internal config labeled by its name (With .yml extension)
     * @param setDefaults Name of internal config to set defaults of
     * @return The current ConfigOptions object
     */
    public ConfigOptions setDefaults(String setDefaults) {
        this.setDefaults = setDefaults;
        return this;
    }

    /**
     * @return The default location to create/get configs in
     */
    public File getDefaultLocation() {
        return defaultLocation;
    }

    /**
     * Sets the default folder to create/get configs in
     * @param defaultLocation Default folder path
     * @return The current ConfigOptions object
     */
    public ConfigOptions setDefaultLocation(File defaultLocation) {
        this.defaultLocation = defaultLocation;
        return this;
    }
}