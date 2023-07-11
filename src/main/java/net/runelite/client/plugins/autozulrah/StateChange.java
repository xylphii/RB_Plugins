package net.runelite.client.plugins.autozulrah;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StateChange {
    BANKING(Banking.class),
    RANGE(Range.class),
    MAGE(Mage.class),
    MELEE_SOUTH(MeleeSouth.class),
    MELEE_WEST(MeleeWest.class),
    START_KILL(StartKill.class),
    DEAD(Dead.class),
    TIMEOUT(Class.class);

    private final Class clazz;
}
