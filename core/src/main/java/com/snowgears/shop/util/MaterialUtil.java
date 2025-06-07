package com.snowgears.shop.util;

import org.bukkit.Material;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class MaterialUtil {
    public static TextComponent getTranslatableComponent(Material material) {
        if (MCVersion.atLeast("1.16")) {
            return new TextComponent(new TranslatableComponent(material.getTranslationKey()));
        } else {
            return new TextComponent(material.name());
        }
    }
}
