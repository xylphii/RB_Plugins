package net.runelite.client.plugins.autoeat;

import com.example.RuneBotApi.Items.Food;
import com.example.RuneBotApi.Items.Potions;
import com.example.RuneBotApi.LocalPlayer.StatInformation;
import com.example.RuneBotApi.LocalPlayer.StatType;
import com.google.inject.Provides;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@PluginDescriptor(
        name = "<html>[<strong><font color=#87CEFA>RB</font></strong>] Auto Eater</html>",
        description = "Eats for you when hp drops below a threshold",
        enabledByDefault = false,
        tags = {"rb", "RB", "auto eater"}
)
public class AutoEatPlugin extends Plugin {

    @Inject
    private Notifier notifier;

    @Inject
    private AutoEatConfig config;

    @Provides
    AutoEatConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoEatConfig.class);
    }

    private int notifierCount = 0;

    private int brewCount = 0;

    @Subscribe
    public void onGameTick(GameTick awoo)
    {
        if (brewCount == 3) // if we've had 3 brew sips, we must restore
        {
            if (!Potions.drinkRestore())
            {
                if (notifierCount <= 5) // if we can't restore, warn the user. don't spam them
                {
                    notifier.notify("Out of restores!");
                    ++notifierCount;
                }
                return;
            }
            brewCount = 0;
            notifierCount = 0;
            return;
        }

        if (StatInformation.getLevel(Skill.HITPOINTS, StatType.BOOSTED) <= config.eatingThreshold())
        {
            int foodConsumed = Food.eatBestFood();
            if (foodConsumed > 0) // if we drink a brew, increment this counter
            {
                ++brewCount;
            }
            else if (foodConsumed < 0) // if we don't have food, warn the user
            {
                if (notifierCount <= 5) // no spammy
                {
                    notifier.notify("Out of food!");
                    ++notifierCount;
                }
                return;
            }
            notifierCount = 0; // we were able to eat something
        }
    }
}
