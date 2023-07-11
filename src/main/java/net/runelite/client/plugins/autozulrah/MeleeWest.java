package net.runelite.client.plugins.autozulrah;

public class MeleeWest extends ZulrahController {
    @Override
    StateChange eventLoop()
    {
        super.eventLoop();
        switch (eventControl)
        {
            case EXEC: return StateChange.MELEE_WEST;
            case RESET:
            case YIELD:
        }

        System.out.println("Melee.eventLoop");
        attackZulrah();
        return StateChange.MELEE_WEST;
    }

    private enum state
    {

    }
}
