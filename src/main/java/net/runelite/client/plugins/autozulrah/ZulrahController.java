package net.runelite.client.plugins.autozulrah;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.RuneBotApi.Items.Food;
import com.example.RuneBotApi.LocalPlayer.StatInformation;
import com.example.RuneBotApi.Npcs.NpcAction;
import com.example.RuneBotApi.RBApi;
import com.example.RuneBotApi.RBRandom;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.client.game.ItemManager;

import java.util.HashSet;
import java.util.Optional;


public abstract class ZulrahController extends StateController {


    private final ItemManager itemManager = RBApi.getItemManager();
    private State state = State.NONE;
    private GearType gearType = GearType.MAGE;
    private int gearIndex = 0;
    private final int[] zulrahIds = {2042, 2043, 2044};
    private NPC zulrah = null;


    // States for sub-controllers
    protected EventControl eventControl = EventControl.YIELD;



    @Override
    StateChange eventLoop() {

//        this.zulrahOptional = getZulrahNpc();
        checkHp();
        getZulrahNpc().ifPresent(zul -> this.zulrah = zul);
        switch (state)
        {
            case SWAPPING_GEAR: swapGear(gearType);
        }

        /*
        this is specifically to yield to timeout events for child classes
        since we want to always execute ZulrahController logic regardless
        of the state of thie children
         */
        if (0 < timeout--) return StateChange.TIMEOUT;
        return null;
    }

    private boolean eatFoodAndResume()
    {
        boolean didEat = (Food.eatBestFood() == 0);
        if (!EthanApiPlugin.isMoving()) attackZulrah(); // we don't want to try to attack Zulrah yet if we are running to a new location
        return didEat;
    }

    protected boolean checkHp()
    {
        if (StatInformation.getHp() < hpThreshold)
        {
            if (!eatFoodAndResume())
            {
                panicTele();
                state = State.NONE;
                eventControl = EventControl.RESET;
                return true;
            }
        }
        return false;
    }

    private void panicTele()
    {
        // teleport logic
    }

    private Optional<NPC> getZulrahNpc()
    {
        for (int id : zulrahIds)
        {
            if (NPCs.search().withId(id).first().isPresent()) return NPCs.search().withId(id).first();
        }
        return Optional.empty();
    }

    protected Optional<NPC> getZulrah()
    {
        return Optional.of(zulrah);
    }

    protected void attackZulrah()
    {
        this.getZulrahNpc().ifPresent(zul -> NpcAction.queueNPCAction(zul, "Attack"));
    }

    protected void swapGear(GearType type)
    {
        state = State.SWAPPING_GEAR;
        eventControl = EventControl.EXEC;

        this.gearType = type;

        HashSet<String> gear = (type == GearType.MAGE) ? RBApi.configCSVToHashSet(config.mageSwitch())
                                                       : RBApi.configCSVToHashSet(config.rangeSwitch());


        switchGear(gear, gear.size());
    }

    private void switchGear(HashSet<String> gear, int size)
    {
        ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);
        if (container == null) return;

        for (int i = 0, inventoryIndex = 0; i < RBRandom.randRange(3, 6) && inventoryIndex <= 28; ++inventoryIndex)
        {

            Item item = container.getItem(inventoryIndex);
            if (item == null) continue;

            String itemName = itemManager.getItemComposition(item.getId()).getName();

            if (RBApi.configMatcher(gear, itemName))
            {
                if (!Inventory.search().withId(item.getId()).withAction("Wear").result().isEmpty())
                    InventoryInteraction.useItem(item.getId(), "Wear");
                else
                    InventoryInteraction.useItem(item.getId(), "Wield");

                ++i;
                ++this.gearIndex;
            }
        }

        if (gearIndex == size)
        {
            this.gearIndex = 0;
            this.state = State.NONE;
            this.eventControl = EventControl.YIELD;
        }
    }

    private enum State
    {
        SWAPPING_GEAR,
        NONE
    }

    protected enum GearType
    {
        MAGE,
        RANGED
    }

    protected enum EventControl
    {
        YIELD,
        EXEC,
        RESET
    }
}
