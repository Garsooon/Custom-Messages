package org.garsooon.PLUGINNAME;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MainClass extends JavaPlugin {
    private static MainClass instance;

    @Override
    public void onEnable() {
        instance = this;
        // Register commands

        // Register listeners


        System.out.println("[Plugin] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        System.out.println("[Plugin] Plugin disabled.");
    }

    public static MainClass getInstance() {
        return instance;
    }
}
