package net.runelite.client.plugins.autozulrah;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import javax.swing.*;

@ConfigGroup("Zulrah")
public interface ZulrahConfig extends Config {

    @ConfigItem(
            keyName = "exportWornGear",
            name = "Export Gear",
            description = "exports gear as csv for mage/range gear config",
            position = 2
    )
    default JMenuItem exportGear()
    {
        JMenuItem exportButton = new JMenuItem("Export Gear");
        exportButton.addActionListener((e) ->
        {
            System.out.println("awoo!");
        });
        return exportButton;
    }

    @ConfigItem(
            keyName = "mageSwitch",
            name = "Mage Switch",
            description = "what is your mage gear?",
            position = 2
    )
    default String mageSwitch()
    {
        return "";
    }

    @ConfigItem(
            keyName = "rangeSwitch",
            name = "Range Switch",
            description = "what is your range gear?",
            position = 3
    )
    default String rangeSwitch()
    {
        return "";
    }

}
