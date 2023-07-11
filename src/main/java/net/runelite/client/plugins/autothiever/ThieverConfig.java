package net.runelite.client.plugins.autothiever;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Thiever")
public interface ThieverConfig extends Config {

    @ConfigItem(
            keyName = "thievingActivity",
            name = "Thieving Activity",
            description = "Whatchya doin?",
            position = 1
    )
    default ThievingActivity thievingActivity() { return ThievingActivity.KNIGHTS; }

    @ConfigItem(
            keyName = "foodType",
            name = "Food Type",
            description = "What type of food?",
            position = 2
    )
    default FoodgeType foodgeType() { return FoodgeType.SHARK; }

    @ConfigItem(
            keyName = "seedNames",
            name = "Seed Names",
            description = "Keep all the listed seeds. Separate by commas. SeedName, seedname, *eedna* will all work for each entry",
            position = 3
    )
    default String seedNames()
    {
        return "";
    }

    enum ThievingActivity
    {
        KNIGHTS,
        BLACKJACK,
        FARMERS,
        CAKES,
        SILK,
        FRUIT
    }

    @AllArgsConstructor
    @Getter
    enum FoodgeType
    {
        SHARK(ItemID.SHARK),
        ANGLERFISH(ItemID.ANGLERFISH),
        KARAMBWAN(ItemID.COOKED_KARAMBWAN),
        MANTAS(ItemID.MANTA_RAY),
        WINE(ItemID.JUG_OF_WINE),
        BREWS(ItemID.SARADOMIN_BREW4);

        private final int id;
    }

}
