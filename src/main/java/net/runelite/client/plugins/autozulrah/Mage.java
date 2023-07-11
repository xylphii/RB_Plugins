package net.runelite.client.plugins.autozulrah;

public class Mage extends ZulrahController {
    @Override
    StateChange eventLoop()
    {
        super.eventLoop();
        switch (eventControl)
        {
            case EXEC: return StateChange.MAGE;
            case RESET:
            case YIELD:
        }
        System.out.println("Mage.eventLoop");
        attackZulrah();
        return StateChange.MAGE;
    }
}
