package net.runelite.client.plugins.autothiever;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.NPCQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.ObjectPackets;
import com.example.Packets.WidgetPackets;
import com.example.RuneBotApi.*;
import com.example.RuneBotApi.Items.Potions;
import com.example.RuneBotApi.LocalPlayer.LocationInformation;
import com.example.RuneBotApi.LocalPlayer.StatInformation;
import com.example.RuneBotApi.LocalPlayer.StatType;
import com.example.RuneBotApi.Npcs.NpcAction;
import com.example.RuneBotApi.Npcs.NpcInformation;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;

import java.util.*;

import static java.lang.Math.max;

public final class Blackjack implements EventLoop {

    Client client = RBApi.getClient();
    private final Random random = new Random();
    private final WorldArea hut = new WorldArea(3340, 2953, 5, 4, 0);
    private final Plugin caller;
    private final int foodId;
    private final WorldHopper worldHopper = new WorldHopper();


    // program state
    private BjState state = BjState.KO;
    private SetupState setupState = SetupState.ALL_GOOD;
    private LureState lureState = LureState.LURE_MENAPHITE;
    private LureMenaphiteOut nyooGoAway = LureMenaphiteOut.LURE_MENAPHITE;
    private EnterHut enterHut = EnterHut.PATH_TO_HUT;
    private RunAway runAway = RunAway.OPEN_CURTAIN;
    private int ragCounter = 0;
    private int lureTimeout = 0;
    private int fallbackTimeout = 0;
    private int timeout = 0;
    private int ppCount = 0;
    private int eatingPercentage;


    Blackjack(int foodId, Plugin caller) {
        this.foodId = foodId;
        this.caller = caller;

        RBApi.runOnClientThread(() -> {
            if (client.getGameState().equals(GameState.LOGIN_SCREEN)) {
                System.out.println("Blackjack.Blackjack");
                EthanApiPlugin.stopPlugin(caller);
                return;
            }
            if (LocationInformation.getMapSquareId() != MapSquare.POLLNIVNEACH.getId()) {
                EthanApiPlugin.sendClientMessage("Move to Pollnivneach before starting plugin.");
                EthanApiPlugin.stopPlugin(caller);
            }
        });

        Optional<NPC> menaphite = NpcInformation.getNearestNpcWithId(NpcID.MENAPHITE_THUG_3550);
        if (menaphite.isEmpty()) {
            // I hate concurrency
            if (!client.getGameState().equals(GameState.LOGGED_IN)) return;
            RBApi.runOnClientThread(() -> {
                EthanApiPlugin.sendClientMessage("No menaphite thugs found! Try heading south");
                EthanApiPlugin.stopPlugin(caller);
            });
        }

        eatingPercentage = newHpThreshold();
    }

    public boolean eventLoop() {
        timeout = max(0, --timeout);
        if (LocationInformation.getMapSquareId() != MapSquare.POLLNIVNEACH.getId()) {
            EthanApiPlugin.sendClientMessage("Please head inside the hut south of the camels in Pollnivneach");
            EthanApiPlugin.stopPlugin(caller);
        }

        NPCQuery menaphiteQuery = NPCs.search().withId(NpcID.MENAPHITE_THUG_3550).withinWorldArea(hut);

        // lure menaphites in or out or run away if in combat
        if (setupState == SetupState.ALL_GOOD) {
            if (!menaphiteQuery.first().isPresent()) setupState = SetupState.LURE_IN;
            if (menaphiteQuery.result().size() > 1) setupState = SetupState.LURE_OUT;
            Optional<NPC> interacting = menaphiteQuery.interactingWithLocal().first();
            if (interacting.isPresent()) {
                NPC interactingMenaphite = interacting.get();
                if (interactingMenaphite.getAnimation() == 395 &&
                        !(interactingMenaphite.getOverheadText().contains("kill") || interactingMenaphite.getOverheadText().contains("doing")))
                    setupState = SetupState.RUN_AWAY;
            }
        }

        if (timeout == 0) {
            switch (setupState) {
                case LURE_IN:
                    lureIn();
                    return true;
                case LURE_OUT:
                    lureOut();
                    return true;
                case RUN_AWAY:
                    runAway();
                    return true;
            }
        }

        // only continue if things are working properly
        if (setupState != SetupState.ALL_GOOD) return true;


        // hop if there's someone else inside
        if (!Players.search().notLocalPlayer().withinWorldArea(hut).isEmpty()) {
            state = BjState.HOP_WORLDS;
        }

        // local player go inside hut if we're outside but in pollnivneach
        if (!LocationInformation.isInTileRange(3340, 3344, 2953, 2956) || enterHut != EnterHut.PATH_TO_HUT) {
            enterHut();
            return true;
        }


        NPC menaphite = NPCs.search().withId(NpcID.MENAPHITE_THUG_3550).withinWorldArea(hut).first().orElseThrow();

        // close curtain if open
        if (state == BjState.KO) {
            Optional<TileObject> openCurtain = TileObjects.search().withId(1534).atLocation(3345, 2955, 0).first();
            if (openCurtain.isPresent()) {
                ObjectPackets.queueObjectAction(openCurtain.get(), false, "Close");
                return true;
            }
        }

        // some arbitrary number of hp just in case. should be good
        // this should prevent any sort of deaths from getting rly unlucky unless maybe someone fucks with the curtain and you're in combat somehow
        if (StatInformation.getLevel(Skill.HITPOINTS, StatType.BOOSTED) < 25) state = BjState.EATING;

        if (timeout == 0) {
            switch (state) {
                case KO:
                    NpcAction.queueNPCAction(menaphite, "Knock-Out");
                    timeout = 2;
                    state = BjState.PP;
            break; case PP:
                    pickpocketHandler(menaphite);
            break; case EATING:
                    eatFoodge();
            break; case HOP_WORLDS:
                    hopWorlds();
            break; case LOGOUT:
                    logoutAndExit();
            }
        }

        if (state == BjState.KO) {
            // check hp
            if (StatInformation.getHpPercentage() <= eatingPercentage) {
                if (eatFoodge()) return true;

                timeout = 10;
                EthanApiPlugin.sendClientMessage("No adequate food in Inventory. If this seems wrong, check the \"Food Type\" field in the config");
                state = BjState.LOGOUT;
                logoutAndExit();
            }
        }

        return true;
    }

    private void runAway()
    {
        Optional<TileObject> closedCurtain = TileObjects.search().withId(1533).atLocation(3345, 2955, 0).first();
        switch (runAway)
        {
            case OPEN_CURTAIN:
                if (closedCurtain.isPresent()) {
                    ObjectPackets.queueObjectAction(closedCurtain.get(), false, "Open");
                } else {
                    Movement.move(3343, 2955, false);
                }
                if (LocationInformation.getTile().equals(new WorldPoint(3343, 2955, 0))) runAway = RunAway.RUN_AWAY;
            break; case RUN_AWAY:
                Movement.move(3367, 2964, false);
                runAway = RunAway.RUNNING;
            break; case RUNNING:
                if (!EthanApiPlugin.isMoving() && closedCurtain.isPresent()) {
                    runAway = RunAway.OPEN_CURTAIN;
                    return;
                }
                if (!EthanApiPlugin.isMoving()) runAway = RunAway.OPEN_CURTAIN;
                setupState = SetupState.ALL_GOOD;
        }

    }

    private void enterHut()
    {
        switch (enterHut)
        {
                   case PATH_TO_HUT:
                Movement.move(3345, 2955, false);
                enterHut = EnterHut.RUNNING;
            break; case RUNNING:
                if (!EthanApiPlugin.isMoving()) enterHut = EnterHut.OPEN_CURTAIN;
            break; case OPEN_CURTAIN:
                Optional<TileObject> closedCurtain = TileObjects.search().withId(1533).atLocation(3345, 2955, 0).first();
                closedCurtain.ifPresent(tileObject -> ObjectPackets.queueObjectAction(tileObject, false, "Open"));
                enterHut = EnterHut.GO_INSIDE;
            break; case GO_INSIDE:
                Movement.move(3344, 2955, false);
                enterHut = EnterHut.CLOSE_CURTAIN;
            break; case CLOSE_CURTAIN:
                if (LocationInformation.isInTileRange(3340, 2953, 5, 4)) {
                    Optional<TileObject> openCurtain = TileObjects.search().withId(1534).atLocation(3345, 2955, 0).first();
                    openCurtain.ifPresent(curtain -> ObjectPackets.queueObjectAction(curtain, false, "Close"));
                }
                enterHut = EnterHut.PATH_TO_HUT;
        }
    }

    private void lureOut()
    {
        if (50 <= fallbackTimeout++) {
            Optional<TileObject> closedCurtain = TileObjects.search().withId(1533).atLocation(3345, 2955, 0).first();
            if (closedCurtain.isPresent()) hopWorlds();
            nyooGoAway = LureMenaphiteOut.LURE_MENAPHITE;
        }

        switch (nyooGoAway)
        {
            case LURE_MENAPHITE:
                NPC menaphite = NPCs.search().withId(NpcID.MENAPHITE_THUG_3550).withinWorldArea(hut).first().orElseThrow();
                NpcAction.queueNPCAction(menaphite, "Lure");
                timeout = 2;
                nyooGoAway = LureMenaphiteOut.CHAT_DIALOGUE_ONE;
            break; case CHAT_DIALOGUE_ONE:
                if (Widgets.search().withTextContains("Psst").result().isEmpty()) return;
                RBApi.sendKeystroke(KeyStroke.SPACE);
                nyooGoAway = LureMenaphiteOut.CHAT_DIALOGUE_TWO;
            break; case CHAT_DIALOGUE_TWO:
                if (Widgets.search().withTextContains("What is it?").result().isEmpty()) {
                    nyooGoAway = LureMenaphiteOut.LURE_MENAPHITE;
                    return;
                }
                RBApi.sendKeystroke(KeyStroke.SPACE);
                nyooGoAway = LureMenaphiteOut.CHAT_DIALOGUE_THREE;
            break; case CHAT_DIALOGUE_THREE:
                RBApi.sendKeystroke(KeyStroke.SPACE);
                nyooGoAway = LureMenaphiteOut.LEAVE_TENT;
            break; case LEAVE_TENT:
                Optional<TileObject> closedCurtain = TileObjects.search().withId(1533).atLocation(3345, 2955, 0).first();
                if (closedCurtain.isPresent()) {
                    ObjectPackets.queueObjectAction(closedCurtain.get(), false, "Open");
                } else {
                    Movement.move(3343, 2955, false);
                }
                closedCurtain.ifPresent(curtain -> ObjectPackets.queueObjectAction(curtain, false, "Open"));
                timeout = 2;
                nyooGoAway = LureMenaphiteOut.WAIT_FOR_ORTHOGONAL_TILE1;
            break; case WAIT_FOR_ORTHOGONAL_TILE1:
                if (10 <= lureTimeout++) {
                    nyooGoAway = LureMenaphiteOut.LURE_MENAPHITE;
                    lureTimeout = 0;
                }
                // if they're on an orthogonal tile or on top of you idk fuck this lol
                if (!(NPCs.search().interacting().meleeable().result().isEmpty()) || !(NPCs.search().withId(NpcID.MENAPHITE_THUG_3550).atLocation(LocationInformation.getTile()).result().isEmpty())) nyooGoAway = LureMenaphiteOut.TOUCH_GRASS;
//                if (!NPCs.search().interacting().meleeable().result().isEmpty()) nyooGoAway = NyooGoAway.TOUCH_GRASS;
            break; case TOUCH_GRASS:
                Movement.move(3346, 2955, false);
                nyooGoAway = LureMenaphiteOut.WAIT_FOR_ORTHOGONAL_TILE2;
            break; case WAIT_FOR_ORTHOGONAL_TILE2:
            if (10 <= lureTimeout++) {
                nyooGoAway = LureMenaphiteOut.LURE_MENAPHITE;
                lureTimeout = 0;
            }
            if (!NPCs.search().interacting().meleeable().result().isEmpty()) nyooGoAway = LureMenaphiteOut.RETURN_TO_TENT;
            break; case RETURN_TO_TENT:
                closedCurtain = TileObjects.search().withId(1533).atLocation(3345, 2955, 0).first();
                if (closedCurtain.isPresent()) {
                    ObjectPackets.queueObjectAction(closedCurtain.get(), false, "Open");
                    return;
                }
                Movement.move(3344, 2955, false);
                nyooGoAway = LureMenaphiteOut.CLOSE_CURTAIN;
            break; case CLOSE_CURTAIN:
                Optional<TileObject> openCurtain = TileObjects.search().withId(1534).atLocation(3345, 2955, 0).first();
                openCurtain.ifPresent(curtain -> ObjectPackets.queueObjectAction(curtain, false, "Close"));
                nyooGoAway = LureMenaphiteOut.LURE_MENAPHITE;
                setupState = SetupState.ALL_GOOD;
        }
    }

    private void lureIn()
    {
        Optional<TileObject> closedCurtain = TileObjects.search().withId(1533).atLocation(3345, 2955, 0).first();
        if (closedCurtain.isPresent()) {
            if (4 < ragCounter++) hopWorlds();
            lureState = LureState.LURE_MENAPHITE;
            ObjectPackets.queueObjectAction(closedCurtain.get(), false, "Open");
            timeout = 3;
            return;
        }

        NPC menaphite = NpcInformation.getNearestWalkableNpcWithId(NpcID.MENAPHITE_THUG_3550).orElseThrow();
        switch (lureState)
        {
                   case LURE_MENAPHITE:
                NpcAction.queueNPCAction(menaphite, "Lure");
                lureState = LureState.RUNNING;
            break; case RUNNING:
                if (Widgets.search().withTextContains("Psst").result().isEmpty()) return;
                RBApi.sendKeystroke(KeyStroke.SPACE);
                lureState = LureState.CHAT_DIALOGUE1;
            break; case CHAT_DIALOGUE1:
                if (Widgets.search().withTextContains("What is it?").result().isEmpty()) lureState = LureState.LURE_MENAPHITE;
                RBApi.sendKeystroke(KeyStroke.SPACE);
                lureState = LureState.CHAT_DIALOGUE2;
            break; case CHAT_DIALOGUE2:
                RBApi.sendKeystroke(KeyStroke.SPACE);
                lureState = LureState.LURE_TILE0;
            break; case LURE_TILE0:
                Movement.move(3347, 2955, false);
                lureState = LureState.WAIT_FOR_ORTHOGONAL_TILE;
            break; case WAIT_FOR_ORTHOGONAL_TILE:
                if (20 <= lureTimeout++) {
                    lureState = LureState.LURE_MENAPHITE;
                    lureTimeout = 0;
                }
                if (!NPCs.search().interacting().meleeable().result().isEmpty()) lureState = LureState.LURE_TILE1;
            break; case LURE_TILE1:
                Movement.move(3344, 2955, false);
                timeout = 2;
                lureState = LureState.LURE_TILE2;
            break; case LURE_TILE2:
                Movement.move(3344, 2954, false);
                lureState = LureState.CLOSE_CURTAIN;
            break; case CLOSE_CURTAIN:
                Optional<TileObject> openCurtain = TileObjects.search().withId(1534).atLocation(3345, 2955, 0).first();
                openCurtain.ifPresent(curtain -> ObjectPackets.queueObjectAction(curtain, false, "Close"));
                lureState = LureState.LURE_MENAPHITE;
                setupState = SetupState.ALL_GOOD;
                ragCounter = 0;
        }

    }

    private void logoutAndExit()
    {
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, 11927560, -1, -1);
    }

    private void hopWorlds()
    {
        worldHopper.setupHop();
        worldHopper.hopWorlds();
    }

    private boolean eatFoodge()
    {
        state = BjState.EATING;
        if (!Inventory.search().withId(foodId).result().isEmpty() || foodId == ItemID.SARADOMIN_BREW4)
        {
            if (StatInformation.getHpPercentage() >= 95)
            {
                eatingPercentage = newHpThreshold();
                state = BjState.KO;
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

        state = BjState.LOGOUT;
        return false;
    }

    private void pickpocketHandler(NPC menaphite)
    {
        if (menaphite.getOverheadText() != null || menaphite.getOverheadText().contains("zzz")) {
            if (menaphite.getOverheadText().contains("doing")) {
                // set timeout
                timeout = 9;
                state = BjState.KO;
                return;
            }

            if (!menaphite.getOverheadText().contains("zzz")) {
                NpcAction.queueNPCAction(menaphite, "Pickpocket");
                NpcAction.queueNPCAction(menaphite, "Knock-Out");
                state = BjState.PP;
                timeout = 0;
                return;
                //pickpocket and knockout
            }
        }

        if (ppCount == 1) {
            NpcAction.queueNPCAction(menaphite, "Pickpocket");
            ppCount = 0;
            timeout = 2;
            state = BjState.KO;
            return;
        }

        NpcAction.queueNPCAction(menaphite, "Pickpocket");
        ppCount = 1;
        timeout = 2;
    }

    private int newHpThreshold()
    {
        return (int)(
                max(50, Math.min(70, Math.abs(random.nextGaussian()) * 80))
        );
    }

    enum RunAway
    {
        OPEN_CURTAIN,
        RUN_AWAY,
        RUNNING
    }

    enum EnterHut
    {
        PATH_TO_HUT,
        RUNNING,
        OPEN_CURTAIN,
        GO_INSIDE,
        CLOSE_CURTAIN
    }

    enum LureMenaphiteOut
    {
        LURE_MENAPHITE,
        CHAT_DIALOGUE_ONE,
        CHAT_DIALOGUE_TWO,
        CHAT_DIALOGUE_THREE,
        LEAVE_TENT,
        WAIT_FOR_ORTHOGONAL_TILE1,
        TOUCH_GRASS,
        WAIT_FOR_ORTHOGONAL_TILE2,
        RETURN_TO_TENT,
        CLOSE_CURTAIN
    }

    enum LureState
    {
        LURE_MENAPHITE,
        RUNNING,
        CHAT_DIALOGUE1,
        CHAT_DIALOGUE2,
        LURE_TILE0,
        WAIT_FOR_ORTHOGONAL_TILE,
        LURE_TILE1,
        LURE_TILE2,
        CLOSE_CURTAIN,
        RETURN
    }

    enum SetupState
    {
        LURE_OUT,
        LURE_IN,
        RUN_AWAY,
        ALL_GOOD,
        HOPPING
    }

    enum BjState
    {
        KO,
        PP,
        EATING,
        HOP_WORLDS,
        LOGOUT
    }
}
