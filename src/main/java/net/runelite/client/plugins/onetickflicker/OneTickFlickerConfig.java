package net.runelite.client.plugins.onetickflicker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ConfigGroup("OneTickFlicker")
public interface OneTickFlickerConfig extends Config {
    @ConfigItem(
            keyName = "1t toggle hotkey",
            name = "1t toggle hotkey",
            description = "Keybind to toggle quickprayer 1t on/off",
            position = 0
    )
    default Keybind oneTickToggle()
    {
        return new Keybind(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    }
}
