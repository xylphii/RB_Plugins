package net.runelite.client.plugins.autozulrah;

public class Banking extends StateController {

    private State state;

    Banking()
    {
        state = State.TELEPORT;
    }

    @Override
    StateChange eventLoop()
    {


        switch (state)
        {
                   case TELEPORT:
            break; case OPEN_BANK:
            break; case DEPOSIT_ITEMS:
            break; case WITHDRAW_ITEMS:
            break; case RETURN:
        }

        return StateChange.BANKING;
    }

    private enum State
    {
        TELEPORT,
        OPEN_BANK,
        DEPOSIT_ITEMS,
        WITHDRAW_ITEMS,
        RETURN
    }
}
