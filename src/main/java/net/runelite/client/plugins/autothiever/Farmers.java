package net.runelite.client.plugins.autothiever;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.*;
import com.example.RuneBotApi.Items.Potions;
import com.example.RuneBotApi.LocalPlayer.LocationInformation;
import com.example.RuneBotApi.LocalPlayer.StatInformation;
import com.example.RuneBotApi.Npcs.NpcAction;
import com.example.RuneBotApi.Npcs.NpcInformation;
import com.example.RuneBotApi.Objects.Banks;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;

public final class Farmers implements EventLoop {
    private final Client client = RBApi.getClient();
    private final ItemManager itemManager = RBApi.getItemManager();
    private final Plugin caller;
    private final Random random = new Random();
    private final String seedNames;
    private final int foodId;

    private boolean banking = false;
    private boolean nom = false;
    private boolean dropping = false;
    private int eatingPercentage;
    private int inventoryIndex = 0;

    Farmers(int foodId, String seedNames, Plugin caller)
    {
        this.caller = caller;
        this.foodId = foodId;
        this.seedNames = seedNames;

        RBApi.runOnClientThread(() -> {
            if (!client.getGameState().equals(GameState.LOGGED_IN)) {
                EthanApiPlugin.stopPlugin(caller);
                return;
            }
            if (LocationInformation.getMapSquareId() != MapSquare.DRAYNOR.getId())
            {
                EthanApiPlugin.sendClientMessage("Move to Draynor bank before starting plugin.");
                EthanApiPlugin.stopPlugin(caller);
            }
        });

        eatingPercentage = newHpThreshold();
        banking = true; // deposit all our shit before we start to avoid potentially weird packets being sent w/ destroyable items
    }

    public boolean eventLoop()
    {
        if (dropping) return dropItems();
        if (banking) return bank();
        if (nom)
        {
            eatFoodge();
            return true;
        }

        if (Inventory.full()) {
            dropItems();
            return true;
        }

        // check hp
        if (StatInformation.getHpPercentage() <= eatingPercentage)
        {
            if(eatFoodge()) return true;

            return bank();
        }

        Optional<NPC> farmer = NpcInformation.getNearestNpcWithId(NpcID.MASTER_FARMER);
        if (!NpcInformation.getNearestNpcWithId(NpcID.MASTER_FARMER).isPresent())
        {
            EthanApiPlugin.sendClientMessage("No Master Farmers Found");
            EthanApiPlugin.stopPlugin(caller);
            return false;
        }

        double randomGaussian = random.nextGaussian();

        if (randomGaussian < -1.5) return true; // skip tick
        if (randomGaussian > 1.5) NpcAction.queueNPCAction(farmer.orElseThrow(), "Pickpocket"); // click twice

        NpcAction.queueNPCAction(farmer.orElseThrow(), "Pickpocket");

        return true;
    }

    private boolean dropItems()
    {
        dropping = true;
        ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);
        if (container == null) return true;

        HashSet<String> seeds = RBApi.configCSVToHashSet(seedNames);

        for (int j = 0; inventoryIndex <= 28 && j < RBRandom.randRange(3, 6); ++inventoryIndex)
        {

            Item item = container.getItem(inventoryIndex);
            if (item == null) continue;

            String itemName = itemManager.getItemComposition(item.getId()).getName();

            int itemStackPrice = itemManager.getItemPrice(item.getId()) * item.getQuantity();

            // if item at slot n isn't in the list,         isn't food,             and isn't too valuable
            if (!(RBApi.configMatcher(seeds, itemName) || item.getId() == foodId || itemStackPrice > 10000)) {
                InventoryInteraction.useItem(item.getId(), "Drop");
                ++j;
            }

        }

        if (inventoryIndex >= 28 && Inventory.getEmptySlots() < 4)
        {
            inventoryIndex = 0;
            dropping = false;
            return bank();
        }

        if (inventoryIndex >= 28) {
            inventoryIndex = 0;
            dropping = false;
        }

        return true;
    }

    private boolean bank()
    {
        banking = true;

        if (Banks.openNearestBank()) return true;

        Optional<Widget> food = Bank.search().withId(foodId).first();
        if (food.isEmpty())
        {
            EthanApiPlugin.sendClientMessage("No food in bank!");
            return false;
        }

        // bank all
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, 786474, -1, -1);

        // wd 8 - this doesn't produce a visual bug
        BankInteraction.useItem(food.get(), "Withdraw-5");
        BankInteraction.useItem(food.get(), "Withdraw-1");
        BankInteraction.useItem(food.get(), "Withdraw-1");
        BankInteraction.useItem(food.get(), "Withdraw-1");

        banking = false;
        return true;
    }

    private boolean eatFoodge()
    {
        nom = true;
        if (!Inventory.search().withId(foodId).result().isEmpty() || foodId == ItemID.SARADOMIN_BREW4)
        {
            if (StatInformation.getHpPercentage() >= 95)
            {
                eatingPercentage = newHpThreshold();
                nom = false;
                return true;
            }

            if (foodId == ItemID.SARADOMIN_BREW4)
            {
                if (Potions.drinkBrew()) return true;
            }
            else
            {
                InventoryInteraction.useItem(foodId, "Eat");
                return true;
            }
        }

        nom = false;
        return false;
    }

    private int newHpThreshold()
    {
        return (int)(
                Math.max(40, Math.min(60, Math.abs(random.nextGaussian()) * 80))
        );
    }
}
