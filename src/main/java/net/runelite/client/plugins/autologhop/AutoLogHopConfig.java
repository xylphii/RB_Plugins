package net.runelite.client.plugins.autologhop;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Autolog")
public interface AutoLogHopConfig extends Config {
    @ConfigItem(
            keyName = "inPvp",
            name = "In PvP Worlds",
            description = "Logout on players when in pvp worlds",
            position = 1
    )
    default boolean InPvp() { return false; }

    @ConfigItem(
            keyName = "onlyAttackers",
            name = "Only If Attackable",
            description = "Only logout if the player is able to attack you",
            position = 2
    )
    default boolean onlyAttackers() { return false; }


}
