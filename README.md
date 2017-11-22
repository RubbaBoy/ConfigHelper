# ConfigHelper
Provides an easy to use API to make custom Spigot configs, with other features.


## Features
+ Easy to use custom/default config generator
+ Infividual config and default options
+ Auto save
+ Auto reload
+ Annotation-bound config getters


## How To Use
For reference of any methods, please check the [JavaDoc](https://rubbaboy.me/confighelp/).

### Default options
Options for configs can be set as default, incase you want to make many configs with the same options.
```Java
Config.getDefaultOptions()
        .enableAutoReload(true)
        .enableAutoSave(true)
        .setDefaultLocation(getDataFolder());
```

### Creating a Config object
To start using the API you need to get a Config object. Leave the config name blank to set it as the default `config.yml` file. Setting options are not required, and override default options set above, if set.
```Java
Config customConfig = new Config("customconfig.yml");
customConfig.getOptions().setDefaults("customconfig.yml");
customConfig.initialize();
```

### Setting objects
Setting things and getting thigns is the same as a standard config. Here's an example of setting a string to the path `my.example.path`
```Java
customConfig.set("my.example.path", "Example message!");
```

### Using annotation-bound getters
Annotation-bound getters are meant for very fast retrieval of objects from a config. Due to how Java works, these values can only be fetched, and do not update when the config is externally modified.
To use annotation-bound getters, first you need to register the class with the annotations in it, with `this` as its parameter, to get an instance of the class.
```Java
Config.registerAnnotatedClass(this);
```

Next, you need to set the actual variable. This should be outside of any method, and accepts a `path` to the value, and (If needed) a config file name (With .yml extension). If you need to use the default config, don't set a `config` argument. Set this variable equal to whatever its default value should be if the path is not found/set in the specified config. These can be any object, not just a string.
```Java
@ConfigSync(config = "customconfig.yml", path = "my.example.path")
private String syncedOtherWelcomeMessage = "A default message!";
```

## Full example class
Here is a full example class, to show the functionality of the API.
```Java
public class Main extends JavaPlugin {

    @ConfigSync(path = "my.welcome.message")
    private String syncedWelcomeMessage = "Default welcome message!"; // In the config `config.yml` the path `my.welcome.message` says: "Welcome User!"

    @ConfigSync(path = "not.existant")
    private String nonExistantThing = "DEFAULT";

    @ConfigSync(config = "customconfig.yml", path = "my.other.welcome.message")
    private String syncedOtherWelcomeMessage = "Another default welcome message!"; // In the config `customconfig.yml` the path `my.other.welcome.message` says: "Sup Dude!"

    @Override
    public void onEnable() {
        Config.registerAnnotatedClass(this);

        Config.getDefaultOptions()
                .enableAutoReload(true)
                .enableAutoSave(true)
                .setDefaultLocation(getDataFolder());

        Config defaultConfig = new Config();
        defaultConfig.getOptions().setDefaults("config.yml");
        defaultConfig.initialize();

        Config customConfig = new Config("customconfig.yml");
        customConfig.getOptions().setDefaults("customconfig.yml");
        customConfig.initialize();

        System.out.println("syncedWelcomeMessage = " + syncedWelcomeMessage); // Outputs "Welcome User!"
        System.out.println("syncedOtherWelcomeMessage = " + syncedOtherWelcomeMessage); // Outputs "Sup Dude!"


        defaultConfig.set("my.welcome.message", "Huh hope this works");
        customConfig.set("my.other.welcome.message", "Wow it really worked!");


        System.out.println("syncedWelcomeMessage = " + syncedWelcomeMessage); // Outputs "Huh hope this works"
        System.out.println("syncedOtherWelcomeMessage = " + syncedOtherWelcomeMessage); // Outputs "Wow it really worked!"
        System.out.println("nonExistantThing = " + nonExistantThing); // Outputs "DEFAULT"
    }
}
```
