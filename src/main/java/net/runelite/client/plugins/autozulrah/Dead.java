package net.runelite.client.plugins.autozulrah;

public class Dead extends ZulrahController {
    @Override
    StateChange eventLoop()
    {
        super.eventLoop();
        switch (eventControl)
        {
            case EXEC: return StateChange.DEAD;
            case RESET:
            case YIELD:
        }
        return StateChange.MELEE_SOUTH;
    }
}
