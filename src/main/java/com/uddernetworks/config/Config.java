package com.uddernetworks.config;

import org.apache.commons.io.FileUtils;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * @author RubbaBoy
 */
public class Config extends FileConfiguration {

    private String name;
    private File path;

    private File fullPath;
    private ConfigOptions options;
    private static ConfigOptions defaultOptions = new ConfigOptions();
    private YamlConfiguration fileConfiguration;

    /**
     * Creates a Config object with all default values
     */
    public Config() {
        this.name = "config.yml";
        this.path = getDefaultDataFolder();
        this.options = new ConfigOptions(defaultOptions);
    }

    /**
     * Creates a Config object from just the config's name
     * @param name The name of the config file with .yml file extension
     */
    public Config(String name) {
        this.name = name;
        this.path = getDefaultDataFolder();
        this.options = new ConfigOptions(defaultOptions);
    }

    /**
     * Creates a Config object
     * @param name The name of the config file with .yml file extension
     * @param path The file path of where the config should be. By default this should be {@link org.bukkit.plugin.java.JavaPlugin#getDataFolder()}
     */
    public Config(String name, File path) {
        this.name = name;
        this.path = path;
        this.options = new ConfigOptions(defaultOptions);
    }

    /**
     * Sets the current Config object's options
     * @param options The ConfigOptions object to set
     * @return The current config object
     */
    public Config setOptions(ConfigOptions options) {
        this.options = options;
        return this;
    }

    /**
     * Gets the global default options
     * @return The global default options
     */
    public static ConfigOptions getDefaultOptions() {
        return defaultOptions;
    }

    /**
     * Gets the current Config object's options
     * @return The current Config object's options
     */
    public ConfigOptions getOptions() {
        return options;
    }

    /**
     * Gets the current config file object
     * @return The current Config file object
     */
    public File getConfigFile() {
        return fullPath;
    }

    /**
     * Gets the default data folder set in {@link com.uddernetworks.config.ConfigOptions#setDefaultLocation(File)}
     * @return The default data folder
     */
    public File getDefaultDataFolder() {
        return defaultOptions.getDefaultLocation();
    }

    /**
     * Saves the current configuration to file
     */
    public void saveConfig() {
        try {
            PrintWriter writer = new PrintWriter(fullPath);
            writer.print("");
            writer.close();
            this.fileConfiguration.save(fullPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, HashMap<String, List<FieldEntry>>> updatingFields = new HashMap<>();

    /**
     * Registers all of the annotations in the current class to the system for processing. Should only be ran once per class.
     * @param instance The instance of the current class. Should NOT be a class object.
     */
    public static void registerAnnotatedClass(Object instance) {
        Class clazz = instance.getClass();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ConfigSync.class)) {
                field.setAccessible(true);
                ConfigSync configSync = field.getAnnotation(ConfigSync.class);

                HashMap<String, List<FieldEntry>> pathsFields;
                List<FieldEntry> pathFields;
                if (updatingFields.containsKey(configSync.config())) {
                    pathsFields = updatingFields.get(configSync.config());
                    if (pathsFields.containsKey(configSync.path())) {
                        pathFields = pathsFields.get(configSync.path());
                    } else {
                        pathFields = new ArrayList<>();
                    }
                } else {
                    pathsFields = new HashMap<>();
                    pathFields = new ArrayList<>();
                }

                pathFields.add(new FieldEntry(instance, field));
                pathsFields.put(configSync.path(), pathFields);
                updatingFields.put(configSync.config(), pathsFields);
            }
        }
    }

    private void updateFields(String path, Object value) {
        if (updatingFields.containsKey(name)) {

            HashMap<String, List<FieldEntry>> paths = updatingFields.get(name);

            if (paths.containsKey(path)) {

                for (FieldEntry fieldEntry : paths.get(path)) {
                    try {
                        Field field = fieldEntry.getField();
                        field.setAccessible(true);
                        field.set(fieldEntry.getInstance(), value);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Updates all of the Annotated fields from the current config
     */
    public void updateAllFields() {
        if (updatingFields.containsKey(name)) {
            HashMap<String, List<FieldEntry>> temp = updatingFields.get(name);

            for (String path : temp.keySet()) {
                if (fileConfiguration.isSet(path)) {
                    Object value = fileConfiguration.get(path, null);
                    if (value == null) continue;
                    List<FieldEntry> fieldEntries = temp.get(path);
                    for (FieldEntry fieldEntry : fieldEntries) {
                        try {
                            Field field = fieldEntry.getField();
                            field.setAccessible(true);
                            field.set(fieldEntry.getInstance(), value);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    /**
     * Initializes and sets up files. Should be ran only once per Config object and after a Config object has been created and options have been set.
     * @param instance The instance of the class to be initialized
     */
    public void initialize(Object instance) {
        try {
            if (!path.exists()) {
                path.mkdirs();
            }

            fullPath = new File(path, name);
            if (!fullPath.exists()) {
                fullPath.createNewFile();

                if (options.getSetDefaults() != null) {
                    URL url = instance.getClass().getClassLoader().getResource(options.getSetDefaults());
                    if (url == null) return;
                    URLConnection connection = url.openConnection();
                    connection.setUseCaches(false);

                    InputStream configStream = connection.getInputStream();

                    FileUtils.copyInputStreamToFile(configStream, fullPath);

                    configStream.close();

                    fileConfiguration = YamlConfiguration.loadConfiguration(fullPath);
                    fileConfiguration.setDefaults(YamlConfiguration.loadConfiguration(fullPath));

                    updateAllFields();
                    return;
                }
            }

            fileConfiguration = YamlConfiguration.loadConfiguration(fullPath);

            updateAllFields();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reloads the current config object from file
     */
    public void reloadConfig() {
        fileConfiguration = YamlConfiguration.loadConfiguration(fullPath);
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        if (fileConfiguration == null) return new LinkedHashSet<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getKeys(deep);
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        if (fileConfiguration == null) return new LinkedHashMap<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getValues(deep);
    }

    @Override
    public boolean contains(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.contains(path);
    }

    @Override
    public boolean contains(String path, boolean ignoreDefault) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.contains(path, ignoreDefault);
    }

    @Override
    public boolean isSet(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isSet(path);
    }

    @Override
    public String getCurrentPath() {
        if (fileConfiguration == null) return "";
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getCurrentPath();
    }

    @Override
    public String getName() {
        if (fileConfiguration == null) return "";
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getName();
    }

    @Override
    public Configuration getRoot() {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getRoot();
    }

    @Override
    public ConfigurationSection getParent() {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getParent();
    }

    @Override
    public Object get(String path) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.get(path);
    }

    @Override
    public Object get(String path, Object def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.get(path, def);
    }

    @Override
    public void set(String path, Object value) {
        if (fileConfiguration != null) {
            fileConfiguration.set(path, value);
            if (options.getEnableAutoSave()) saveConfig();
            updateFields(path, value);
        }
    }

    @Override
    public ConfigurationSection createSection(String path) {
        if (fileConfiguration == null) return null;
        ConfigurationSection ret = fileConfiguration.createSection(path);
        if (options.getEnableAutoSave()) saveConfig();
        updateFields(path, ret);
        return ret;
    }

    @Override
    public ConfigurationSection createSection(String path, Map<?, ?> map) {
        if (fileConfiguration == null) return null;
        ConfigurationSection ret = fileConfiguration.createSection(path, map);
        if (options.getEnableAutoSave()) saveConfig();
        updateFields(path, ret);
        return ret;
    }

    @Override
    public String getString(String path) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getString(path);
    }

    @Override
    public String getString(String path, String def) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getString(path, def);
    }

    @Override
    public boolean isString(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isString(path);
    }

    @Override
    public int getInt(String path) {
        if (fileConfiguration == null) return 0;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getInt(path);
    }

    @Override
    public int getInt(String path, int def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getInt(path, def);
    }

    @Override
    public boolean isInt(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isInt(path);
    }

    @Override
    public boolean getBoolean(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getBoolean(path, def);
    }

    @Override
    public boolean isBoolean(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isBoolean(path);
    }

    @Override
    public double getDouble(String path) {
        if (fileConfiguration == null) return 0;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getDouble(path);
    }

    @Override
    public double getDouble(String path, double def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getDouble(path, def);
    }

    @Override
    public boolean isDouble(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isDouble(path);
    }

    @Override
    public long getLong(String path) {
        if (fileConfiguration == null) return 0;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getLong(path);
    }

    @Override
    public long getLong(String path, long def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getLong(path, def);
    }

    @Override
    public boolean isLong(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isLong(path);
    }

    @Override
    public List<?> getList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getList(path);
    }

    @Override
    public List<?> getList(String path, List<?> def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getList(path, def);
    }

    @Override
    public boolean isList(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isList(path);
    }

    @Override
    public List<String> getStringList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getStringList(path);
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getIntegerList(path);
    }

    @Override
    public List<Boolean> getBooleanList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getBooleanList(path);
    }

    @Override
    public List<Double> getDoubleList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getDoubleList(path);
    }

    @Override
    public List<Float> getFloatList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getFloatList(path);
    }

    @Override
    public List<Long> getLongList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getLongList(path);
    }

    @Override
    public List<Byte> getByteList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getByteList(path);
    }

    @Override
    public List<Character> getCharacterList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getCharacterList(path);
    }

    @Override
    public List<Short> getShortList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getShortList(path);
    }

    @Override
    public List<Map<?, ?>> getMapList(String path) {
        if (fileConfiguration == null) return new ArrayList<>();
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getMapList(path);
    }

    @Override
    public Vector getVector(String path) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getVector(path);
    }

    @Override
    public Vector getVector(String path, Vector def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getVector(path, def);
    }

    @Override
    public boolean isVector(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isVector(path);
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String path) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getOfflinePlayer(path);
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String path, OfflinePlayer def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getOfflinePlayer(path, def);
    }

    @Override
    public boolean isOfflinePlayer(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isOfflinePlayer(path);
    }

    @Override
    public ItemStack getItemStack(String path) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getItemStack(path);
    }

    @Override
    public ItemStack getItemStack(String path, ItemStack def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getItemStack(path, def);
    }

    @Override
    public boolean isItemStack(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isItemStack(path);
    }

    @Override
    public Color getColor(String path) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getColor(path);
    }

    @Override
    public Color getColor(String path, Color def) {
        if (fileConfiguration == null) return def;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getColor(path, def);
    }

    @Override
    public boolean isColor(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isColor(path);
    }

    @Override
    public ConfigurationSection getConfigurationSection(String path) {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getConfigurationSection(path);
    }

    @Override
    public boolean isConfigurationSection(String path) {
        if (fileConfiguration == null) return false;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.isConfigurationSection(path);
    }

    @Override
    public ConfigurationSection getDefaultSection() {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.getDefaultSection();
    }

    @Override
    public void addDefault(String path, Object value) {
        if (fileConfiguration != null) {
            fileConfiguration.addDefault(path, value);
        }
    }

    @Override
    public String saveToString() {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        return fileConfiguration.saveToString();
    }

    @Override
    public void loadFromString(String s) throws InvalidConfigurationException {
        if (fileConfiguration == null) return;
        if (options.getEnableAutoReload()) reloadConfig();

        fileConfiguration.loadFromString(s);
    }

    @Override
    protected String buildHeader() {
        if (fileConfiguration == null) return null;
        if (options.getEnableAutoReload()) reloadConfig();

        try {
            Method buildHeader = fileConfiguration.getClass().getDeclaredMethod("buildHeader");
            buildHeader.setAccessible(true);
            return (String) buildHeader.invoke(fileConfiguration);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class FieldEntry {
        private Object instance;
        private Field field;

        FieldEntry(Object instance, Field field) {
            this.instance = instance;
            this.field = field;
        }

        Object getInstance() {
            return instance;
        }

        Field getField() {
            return field;
        }
    }
}