package net.runelite.client.plugins.autozulrah;

public class MeleeSouth extends ZulrahController {
    @Override
    StateChange eventLoop()
    {
        super.eventLoop();
        switch (eventControl)
        {
            case EXEC: return StateChange.MELEE_SOUTH;
            case RESET:
            case YIELD:
        }
        System.out.println("SouthRange.eventLoop");
        attackZulrah();
        return StateChange.MELEE_SOUTH;
    }
}
