package net.runelite.client.plugins.autothiever;

import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.Collections.BankInventory;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.*;
import com.example.RuneBotApi.Items.Potions;
import com.example.RuneBotApi.LocalPlayer.LocationInformation;
import com.example.RuneBotApi.LocalPlayer.StatInformation;
import com.example.RuneBotApi.Npcs.NpcAction;
import com.example.RuneBotApi.Npcs.NpcInformation;
import com.example.RuneBotApi.Objects.Banks;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.Plugin;

import java.util.Optional;
import java.util.Random;

public final class Knights implements EventLoop {

    private final Client client = RBApi.getClient();

    private final Random random = new Random();

    private final WorldPoint knightTile = new WorldPoint(2655, 3286, 0);

    private final int foodId;

    private boolean lock = true;
    private boolean banking = false;
    private boolean nom = false;
    private int eatingPercentage;
    private int coinPouchAmount;

    Knights(int foodId, Plugin caller)
    {
        this.foodId = foodId;

        RBApi.runOnClientThread(() -> {
            if (!client.getGameState().equals(GameState.LOGGED_IN)) {
                EthanApiPlugin.stopPlugin(caller);
                return;
            }
            if (LocationInformation.getMapSquareId() != MapSquare.ARDOUGNE.getId())
            {
                EthanApiPlugin.sendClientMessage("Move to Ardy south bank before starting plugin.");
                EthanApiPlugin.stopPlugin(caller);
            }

            EthanApiPlugin.sendClientMessage("Ensure there is a splasher in the bank before starting.");
        });

        eatingPercentage = newHpThreshold();
        coinPouchAmount = newCoinPouchAmount();

        // because async
        if (LocationInformation.getMapSquareId() == MapSquare.ARDOUGNE.getId()) Movement.move(knightTile);
    }

    public boolean eventLoop()
    {
        if (LocationInformation.isOnTile(knightTile)) lock = false;
        if (lock) return true;
        if (banking) return wdFromBank();
        if (nom)
        {
            eatFoodge();
            return true;
        }

        // check hp
        if (StatInformation.getHpPercentage() <= eatingPercentage)
        {
            nom = true;
            if(eatFoodge()) return true;

            banking = true;
            return wdFromBank();
        }

        if (!Inventory.search().withId(RBConstants.coinPouchId).quantityGreaterThan(coinPouchAmount).result().isEmpty())
        {
            InventoryInteraction.useItem(RBConstants.coinPouchId, "Open-all");
            coinPouchAmount = newCoinPouchAmount();
        }

        NPC knight = NpcInformation.getNearestNpcWithId(RBConstants.ardyKnightId).orElseThrow();

        double randomGaussian = random.nextGaussian();

        if (randomGaussian < -1.5) return true; // skip tick
        if (randomGaussian > 1.5) NpcAction.queueNPCAction(knight, "Pickpocket"); // click twice

        NpcAction.queueNPCAction(knight, "Pickpocket");

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

    private boolean wdFromBank()
    {
        if (Banks.openNearestBank()) return true;

        Optional<Widget> food = Bank.search().withId(foodId).first();
        Optional<Widget> invFood = BankInventory.search().withId(foodId).first();
        if (food.isEmpty())
        {
            EthanApiPlugin.sendClientMessage("No food in bank!");
            return false;
        }

        // bank vials or jugs if needed
        if (foodId == ItemID.SARADOMIN_BREW4 || foodId == ItemID.JUG_OF_WINE)
        {
            Optional<Widget> junk;

            junk = (foodId == ItemID.SARADOMIN_BREW4) ? BankInventory.search().withId(ItemID.VIAL).first()
                                                      : BankInventory.search().withId(ItemID.EMPTY_JUG).first();

            junk.ifPresent(widget -> WidgetPackets.queueWidgetAction(widget, "Deposit-All"));
        }

        BankInteraction.useItem(foodId, "Withdraw-All");
        // if no coin pouches, we want to bank one after
        if (Inventory.search().withId(RBConstants.coinPouchId).result().isEmpty())
        {
            if (invFood.isPresent())
                WidgetPackets.queueWidgetAction(invFood.get(), "Deposit-1");
            else
                return true; // wait 1t
        }

        banking = false;
        return true;
    }

    private int newCoinPouchAmount()
    {
        return (int)(
            Math.max(8, Math.min(26, Math.abs(random.nextGaussian()) * 26))
        );
    }

    private int newHpThreshold()
    {
        return (int)(
                Math.max(40, Math.min(60, Math.abs(random.nextGaussian()) * 80))
        );
    }
}
