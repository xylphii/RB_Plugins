package net.runelite.client.plugins.autologhop;

import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.WorldHopper;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.*;

import static net.runelite.api.WorldType.*;

@PluginDescriptor(
        name = "<html>[<strong><font color=#87CEFA>RB</font></strong>] Auto Log Hop</html>",
        description = "Auto logs in PvP or hops in wilderness if there are nearby players",
        enabledByDefault = false,
        tags = {"rb", "RB", "pvp", "notifications", "warnings", "hcim", "uim"}
)
@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
public class AutoLogHopPlugin extends Plugin
{

    @Inject
    private Client client;

    @Inject
    private AutoLogHopConfig config;

    @Provides
    AutoLogHopConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoLogHopConfig.class);
    }

    static final String CONFIG_GROUP = "Autolog";
    private boolean inPvp;
    private WorldType currentType;
    private int warns = 0;
    private final int logoutWidgetId = 11927560;
    private final int smallLogoutWidgetId = 4522009;

    private final WorldHopper hopper = new WorldHopper();

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!config.InPvp())
        {
            inPvp = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {

        hopper.setupHop();

        if (config.InPvp())
        {
            EnumSet<WorldType> worldTypeEnumSet = client.getWorldType();

            for (WorldType i : worldTypeEnumSet)
            {
                if (i == DEADMAN || i == PVP || i == HIGH_RISK)
                {
                    inPvp = true;
                    currentType = i;
                    break;
                }
                inPvp = false;
            }
        }

        if (client.getVarbitValue(5963) == 1 || inPvp)
        {
            // using the specorb varbit to check for safezone
            if (client.getVarbitValue(8121) == 0)
            {
                // reset the warn counter if we are in a pvp safezone
                warns = 0;
                return;
            }

            Player localPlayer = client.getLocalPlayer();
            List<Player> players = client.getPlayers();
            int combatlvl = client.getLocalPlayer().getCombatLevel();
            int maxlvl = combatlvl+getLevelDifference(client.getWorldType());
            int minlvl = combatlvl-getLevelDifference(client.getWorldType());

            // reset warn counter if we are the only player on screen
            if (players.size() == 1) warns = 0;

            // iterate over the list of players to search for matches
            for (Player i : players)
            {
                // do not spam logout forever if we end up getting attacked
                if (warns > 4) break;

                // if someone on screen isn't us do stuff
                if (i == localPlayer) continue;

                // check for attackers here
                if (config.onlyAttackers())
                {
                    if (currentType == DEADMAN)
                    {
                        ++warns;
                        logout();
                        continue;
                    }
                    if (i.getCombatLevel() >= minlvl && i.getCombatLevel() <= maxlvl)
                    {
                        ++warns;
                        logout();
                    }
                    continue;
                }

                ++warns;
                logout();
            }
        } else {
            // reset the warns variable if we leave the wilderness
            warns = 0;
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        warns = 0;
    }

    private int getLevelDifference(EnumSet<WorldType> worldTypeEnumSet)
    {
        int difference = 0;

        for (WorldType i : worldTypeEnumSet)
        {
            if (i == PVP || i == HIGH_RISK)
            {
                difference += 15;
                break;
            }
        }

        if (client.getVarbitValue(5963) == 1)
        {
            difference += getWildyLvl();
        }

        return difference;
    }

    // change this eventually to be when within coords 2944 <= x < ? and 3520 <= z < ? because combat ranges are scuffed
    private int getWildyLvl()
    {
        String widgitInfo = client.getWidget(WidgetInfo.PVP_WILDERNESS_LEVEL).getText().replace("Level: ", "");

        return Integer.parseInt(widgitInfo);
    }

    private void logout()
    {
        if (!inPvp) {
            hop();
            return;
        }
        Optional<Widget> widget = Widgets.search().withId(smallLogoutWidgetId).first();
        if (widget.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, smallLogoutWidgetId, -1, -1);
            return;
        }

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, logoutWidgetId, -1, -1);
    }

    private void hop()
    {
        hopper.hopWorlds();
    }


}
