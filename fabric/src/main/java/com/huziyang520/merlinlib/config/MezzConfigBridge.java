// MezzConfig 联动：装了就把 client.toml 的开关登记成 MezzConfig 的配置页，没装就静默跳过。

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MezzConfigBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);
    private static final String CONFIGS = "net.mezzdev.config.api.Configs";
    private static final String REGISTRATION = "net.mezzdev.config.api.IConfigRegistration";
    private static final String SCHEMA = "net.mezzdev.config.api.schema.builder.IConfigSchemaBuilder";
    private static final String CATEGORY = "net.mezzdev.config.api.schema.builder.IConfigCategoryBuilder";
    private static final String VALUE_BUILDER = "net.mezzdev.config.api.value.builder.IConfigValueBuilder";
    private static final String VALUE = "net.mezzdev.config.api.value.IConfigValue";
    private static final String LISTENER = "net.mezzdev.config.api.value.change.IConfigValueChangeListener";
    private static final Map<String, Object> VALUES = new LinkedHashMap<>();
    private static boolean installed;

    private MezzConfigBridge() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        try {
            Class<?> configs = Class.forName(CONFIGS);
            Class<?> registrationType = Class.forName(REGISTRATION);
            Class<?> schemaType = Class.forName(SCHEMA);
            Class<?> categoryType = Class.forName(CATEGORY);
            Object registration = configs.getMethod("forMod", String.class).invoke(null, Constants.MOD_ID);
            Object schema = registrationType.getMethod("createClientSchemaBuilder", String.class, String.class)
                    .invoke(registration, "client.ini", ConfigOption.KEY_PREFIX.substring(0, ConfigOption.KEY_PREFIX.length() - 1));
            Method addCategory = schemaType.getMethod("addCategory", String.class);
            Method addBoolean = categoryType.getMethod("addBoolean", String.class, boolean.class);
            Method addInteger = categoryType.getMethod("addInteger", String.class, int.class, int.class, int.class);
            Method addDouble = categoryType.getMethod("addDouble", String.class, double.class, double.class, double.class);
            Method addString = categoryType.getMethod("addString", String.class, String.class);
            Method buildValue = Class.forName(VALUE_BUILDER).getMethod("build");
            Map<String, String> current = ClientOptions.values();
            for (String section : ClientOptions.sections()) {
                Object category = addCategory.invoke(schema, section);
                for (ConfigOption option : ClientOptions.in(section)) {
                    String name = option.key().substring(section.length() + 1);
                    String text = ConfigOption.valueOf(option, current).trim();
                    Object builder = switch (option.kind()) {
                        case BOOLEAN -> addBoolean.invoke(category, name, Boolean.parseBoolean(text));
                        case INTEGER -> addInteger.invoke(category, name, Integer.parseInt(text), option.intMin(), option.intMax());
                        case DECIMAL -> addDouble.invoke(category, name, Double.parseDouble(text), option.min(), option.max());
                        case COLOUR, TEXT -> addString.invoke(category, name, text);
                    };
                    Object value = buildValue.invoke(builder);
                    VALUES.put(option.key(), value);
                    listen(option, value);
                }
            }
            schemaType.getMethod("build").invoke(schema);
            installed = true;
            LOGGER.info("[MerlinLib] 已接入 MezzConfig，配置项：{}", VALUES.keySet());
        } catch (Exception | LinkageError failure) {
            VALUES.clear();
            installed = false;
            LOGGER.warn("[MerlinLib] MezzConfig 接入失败，沿用自带配置文件", failure);
        }
    }

    private static void listen(ConfigOption option, Object value) throws ReflectiveOperationException {
        Class<?> listenerType = Class.forName(LISTENER);
        InvocationHandler handler = (proxy, method, args) -> {
            if ("onConfigValueChanged".equals(method.getName()) && args != null && args.length == 1) {
                Object next = args[0].getClass().getMethod("newValue").invoke(args[0]);
                if (next != null) {
                    ClientConfig.write(option.key(), String.valueOf(next));
                }
            }
            return null;
        };
        Object listener = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[] {listenerType}, handler);
        Class.forName(VALUE).getMethod("addListener", listenerType).invoke(value, listener);
    }
}
