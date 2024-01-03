package org.example.functions.util;

import com.azure.core.util.Configuration;

import java.util.Optional;

public class EnvHelper {
    public static String getAccountName() {
        return getProp("PRIMARY_STORAGE_ACCOUNT_NAME");
    }

    public static String getAccountKey() {
        return getProp("PRIMARY_STORAGE_ACCOUNT_KEY");
    }

    static String getProp(String prop) {
        Optional<String> config = Optional.ofNullable(Configuration.getGlobalConfiguration().get(prop));
        return config.orElse(System.getenv(prop));
    }

}