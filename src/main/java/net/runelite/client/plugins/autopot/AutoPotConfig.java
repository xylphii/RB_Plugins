package net.runelite.client.plugins.autopot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoPotter")
public interface AutoPotConfig extends Config {

    @ConfigItem(
            keyName = "brewUsage",
            name = "Healing with brews?",
            description = "We will only repot if your stats are max/max",
            position = 1
    )
    default boolean brewUsage()
    {
        return false;
    }

    @ConfigItem(
            keyName = "attackEnabled",
            name = "Attack pots enabled",
            description = "Do we want to check/repot for attack levels?",
            position = 2
    )
    default boolean attRepotBool()
    {
        return false;
    }
    @ConfigItem(
            keyName = "attackThreshold",
            name = "Attack stat threshold",
            description = "What level to repot at",
            position = 3
    )
    default int attackThreshold()
    {
        return 116;
    }

    @ConfigItem(
            keyName = "strengthEnabled",
            name = "Strength pots enabled",
            description = "Do we want to check/repot for strength levels?",
            position = 4
    )
    default boolean strRepotBool()
    {
        return false;
    }
    @ConfigItem(
            keyName = "strThreshold",
            name = "Strength stat threshold",
            description = "What level to repot at",
            position = 5
    )
    default int strengthThreshold()
    {
        return 116;
    }

    @ConfigItem(
            keyName = "rangedEnabled",
            name = "Range pots enabled",
            description = "Do we want to check/repot for ranged levels?",
            position = 6
    )
    default boolean rangeRepotBool()
    {
        return false;
    }
    @ConfigItem(
            keyName = "rangedThreshold",
            name = "Ranged stat threshold",
            description = "What level to repot at",
            position = 7
    )
    default int rangeThreshold()
    {
        return 110;
    }

    @ConfigItem(
            keyName = "prayerEnabled",
            name = "Prayer/rest pots enabled",
            description = "Do we want to check/repot for prayer points?",
            position = 8
    )
    default boolean prayerRepotBool()
    {
        return false;
    }
    @ConfigItem(
            keyName = "prayerThreshold",
            name = "Prayer stat threshold",
            description = "What level to repot at",
            position = 9
    )
    default int prayerThreshold()
    {
        return 40;
    }
}
