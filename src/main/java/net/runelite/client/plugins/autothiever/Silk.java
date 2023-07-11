package net.runelite.client.plugins.autothiever;

import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.ObjectPackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.*;
import com.example.RuneBotApi.Items.Potions;
import com.example.RuneBotApi.LocalPlayer.LocationInformation;
import com.example.RuneBotApi.LocalPlayer.StatInformation;
import com.example.RuneBotApi.Objects.Banks;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.Plugin;

import java.util.Optional;
import java.util.Random;

public final class Silk implements EventLoop {

    private final Client client = RBApi.getClient();

    private final Random random = new Random();

    private final WorldPoint silkTile = new WorldPoint(2662, 3316, 0);

    private final WorldPoint runTile = new WorldPoint(2679, 3313, 0);

    private final int foodId;

    private boolean lock = true;
    private boolean banking = false;
    private boolean nom = false;
    private boolean dropping = false;
    private int eatingPercentage;
    private int inventoryIndex = 0;

    Silk(int foodId, Plugin caller)
    {
        this.foodId = foodId;

        RBApi.runOnClientThread(() -> {
            if (!client.getGameState().equals(GameState.LOGGED_IN)) {
                EthanApiPlugin.stopPlugin(caller);
                return;
            }
            if (LocationInformation.getMapSquareId() != MapSquare.ARDOUGNE.getId())
            {
                EthanApiPlugin.sendClientMessage("Move to Ardy before starting plugin.");
                EthanApiPlugin.stopPlugin(caller);
            }
        });

        eatingPercentage = newHpThreshold();
        Movement.move(silkTile);


    }

    public boolean eventLoop()
    {
        if (LocationInformation.isOnTile(silkTile)) lock = false;
        if (LocationInformation.isOnTile(runTile)) Movement.move(silkTile);
        if (lock) return true;
        if (dropping) return dropItems();
        if (banking) return bank();
        if (nom)
        {
            eatFoodge();
            return true;
        }

        // if the police are after us
        if (!NPCs.search().interactingWithLocal().empty()) {
            Movement.move(runTile);
            lock = true;
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

        Optional<TileObject> silkStall = TileObjects.search().withId(ObjectID.SILK_STALL_11729).atLocation(2662, 3314, 0).first();

        silkStall.ifPresent(obj ->  {
            double randomGaussian = random.nextGaussian();

            if (randomGaussian < -1.5) return; // skip tick
            if (randomGaussian > 1.5) ObjectPackets.queueObjectAction(obj, false, "Steal-from");

            ObjectPackets.queueObjectAction(obj, false, "Steal-from");
        });


        return true;
    }

    private boolean dropItems()
    {
        dropping = true;
        ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);
        if (container == null) return true;

        for (int j = 0; inventoryIndex <= 28 && j < RBRandom.randRange(3, 6); ++inventoryIndex)
        {

            Item item = container.getItem(inventoryIndex);
            if (item == null) continue;

            if (ItemID.SILK == item.getId())
            {
                InventoryInteraction.useItem(item.getId(), "Drop");
                ++j;
            }

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
        WidgetPackets.queueWidgetActionPacket(1, 786474, -1, -1);

        // wd 8
        BankInteraction.withdrawX(food.get(), 10);

        banking = false;
        Movement.move(silkTile);
        lock = true;
        return true;
    }
    private boolean eatFoodge()
    {
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
