package net.runelite.client.plugins.autopot;

import com.example.RuneBotApi.Items.PotionType;
import com.example.RuneBotApi.Items.Potions;
import com.example.RuneBotApi.LocalPlayer.StatInformation;
import com.example.RuneBotApi.LocalPlayer.StatType;
import com.google.inject.Provides;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@PluginDescriptor(
        name = "<html>[<strong><font color=#87CEFA>RB</font></strong>] Auto Potter</html>",
        description = "Pots for you when a stat drops below a threshold",
        enabledByDefault = false,
        tags = {"rb", "RB", "auto potter", "potions"}
)
public class AutoPotPlugin extends Plugin {

    @Inject
    private AutoPotConfig config;

    @Provides
    AutoPotConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoPotConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick awoo)
    {
        // skip all logic if we aren't at lvl/lvl stats
        if (config.brewUsage()
                && StatInformation.getLevel(Skill.MAGIC, StatType.BOOSTED)
                != StatInformation.getLevel(Skill.MAGIC, StatType.BASE))
                    return;


        if (config.attRepotBool()
                && StatInformation.getLevel(Skill.ATTACK, StatType.BOOSTED) <= config.attackThreshold()
                && repotAttack())
                    return;

        if (config.strRepotBool()
                && StatInformation.getLevel(Skill.STRENGTH, StatType.BOOSTED) <= config.strengthThreshold()
                && repotStrength())
                    return;

        if (config.rangeRepotBool()
                && StatInformation.getLevel(Skill.RANGED, StatType.BOOSTED) <= config.rangeThreshold()
                && repotRanged())
                    return;

        if (config.prayerRepotBool()
                && StatInformation.getLevel(Skill.PRAYER, StatType.BOOSTED) <= config.prayerThreshold())
            repotPrayer();
    }

    private boolean repotAttack()
    {
        if (repotScp()) return true;
        return Potions.drinkPotion(PotionType.SUPER_ATTACK);
    }

    private boolean repotStrength()
    {
        if (repotScp()) return true;
        return Potions.drinkPotion(PotionType.SUPER_STRENGTH);
    }

    private boolean repotRanged()
    {
        if (Potions.drinkPotion(PotionType.RANGING)) return true;
        if (Potions.drinkPotion(PotionType.BASTION)) return true;
        if (Potions.drinkPotion(PotionType.DIVINE_RANGING)) return true;
        return Potions.drinkPotion(PotionType.DIVINE_BASTION);
    }

    private boolean repotScp()
    {
        if (Potions.drinkPotion(PotionType.SUPER_COMBAT)) return true;
        return Potions.drinkPotion(PotionType.DIVINE_SUPER_COMBAT);
    }

    private boolean repotPrayer()
    {
        if (Potions.drinkPotion(PotionType.PRAYER)) return true;
        return Potions.drinkPotion(PotionType.SUPER_RESTORE);
    }
}
