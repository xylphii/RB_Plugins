package net.runelite.client.plugins.autozulrah;

import com.example.RuneBotApi.RBApi;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;

public abstract class StateController {

    @Inject
    protected ZulrahConfig config;

    @Provides
    ZulrahConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ZulrahConfig.class);
    }

    protected final Client client = RBApi.getClient();

    protected int hpThreshold = 60;
    protected int timeout = 0;

    abstract StateChange eventLoop();

    StateController()
    {
        this.config = getConfig(RBApi.getConfigManager());
    }
}
