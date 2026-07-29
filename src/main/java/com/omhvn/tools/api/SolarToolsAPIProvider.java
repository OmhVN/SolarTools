package com.omhvn.tools.api;

public final class SolarToolsAPIProvider {
    private static SolarToolsAPI instance;

    private SolarToolsAPIProvider() {}

    public static SolarToolsAPI getAPI() {
        if (instance == null) {
            throw new IllegalStateException("SolarToolsAPI is not initialized yet!");
        }
        return instance;
    }

    public static void register(SolarToolsAPI api) {
        instance = api;
    }

    public static void unregister() {
        instance = null;
    }
}
