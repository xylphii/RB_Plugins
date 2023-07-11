package net.runelite.client.plugins.autoeat;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoEater")
public interface AutoEatConfig extends Config {
    @ConfigItem(
            keyName = "hpThreshold",
            name = "Eating threshold",
            description = "What hp to eat at",
            position = 1
    )
    default int eatingThreshold()
    {
        return 60;
    }
}
