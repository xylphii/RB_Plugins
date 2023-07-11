package net.runelite.client.plugins.onetickflicker;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;

@PluginDescriptor(
        name = "<html>[<strong><font color=#87CEFA>RB</font></strong>] 1t Flicker</html>",
        description = "1t flicks your quickprayers",
        enabledByDefault = false,
        tags = {"rb", "RB", "1t flicker", "prayer", "prayer flicker"}
)
public class OneTickFlickerPlugin extends Plugin {

    @Inject
    OneTickFlickerConfig config;

    @Inject
    private KeyManager keymanager;

    private final HotkeyListener toggleKeyListener = new HotkeyListener(() -> config.oneTickToggle())
    {
        @Override
        public void hotkeyPressed() {
            isActive = !isActive;
        }
    };

    @Provides
    OneTickFlickerConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneTickFlickerConfig.class);
    }

    private boolean isActive = false;

    @Override
    protected void startUp()
    {
        keymanager.registerKeyListener(toggleKeyListener);
    }

    @Subscribe
    public void onGameTick(GameTick ily) {
        if (isActive) {
            if (EthanApiPlugin.isQuickPrayerEnabled()) {
                // deactivate and activate
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(1, 10485779, -1, -1);
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(1, 10485779, -1, -1);
                return;
            }
            // activate
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 10485779, -1, -1);
        }
    }


}
