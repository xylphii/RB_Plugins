package net.runelite.client.plugins.autothiever;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.RuneBotApi.RBApi;
import com.google.inject.Provides;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@PluginDescriptor(
        name = "<html>[<strong><font color=#87CEFA>RB</font></strong>] Auto Thiever</html>",
        description = "Thieving bot for stalls, pickpocketing, and blackjacking",
        enabledByDefault = false,
        tags = {"rb", "RB", "thiever", "thieving"}
)
public class ThieverPlugin extends Plugin {

    @Inject
    private ThieverConfig config;

    @Provides
    ThieverConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ThieverConfig.class);
    }

    private ThieverConfig.ThievingActivity activity;

    private EventLoop currentActivity;

    private String seedNames;

    @Override
    protected void startUp()
    {
        seedNames = config.seedNames(); // seems like onConfigChanged is invoked whenever we hop, so check for equiv before exiting
        activity = config.thievingActivity();
        int foodId = config.foodgeType().getId();

        switch (activity)
        {
                   case BLACKJACK:    this.currentActivity = new Blackjack(foodId, this);
            break; case CAKES:        this.currentActivity = new Cakes(foodId, this);
            break; case FARMERS:      this.currentActivity = new Farmers(foodId, config.seedNames(), this);
            break; case KNIGHTS:      this.currentActivity = new Knights(foodId, this);
            break; case SILK:         this.currentActivity = new Silk(foodId, this);
        }
    }


    @Subscribe
    public void onGameTick(GameTick meow)
    {
        if (!currentActivity.eventLoop()) {
            System.out.println("ThieverPlugin.onGameTick");
            EthanApiPlugin.stopPlugin(this);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged UwU)
    {
        if (UwU.getGameState() == GameState.LOGIN_SCREEN) {
            System.out.println("ThieverPlugin.onGameStateChanged loginscreen");
            EthanApiPlugin.stopPlugin(this);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!config.seedNames().equals(seedNames)) RBApi.runOnClientThread(() -> EthanApiPlugin.stopPlugin(this));
    }

}
