package net.runelite.client.plugins.autozulrah;

public class Range extends ZulrahController {
    @Override
    StateChange eventLoop()
    {
        super.eventLoop();
        switch (eventControl)
        {
            case EXEC: return StateChange.RANGE;
            case RESET:
            case YIELD:
        }
        System.out.println("EastRange.eventLoop");
        attackZulrah();
        return StateChange.RANGE;
    }
}
