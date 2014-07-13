package madscience.items.spawnegg;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MadGMORegistry
{
    private static final Map<Short, MadSpawnEggInfo> eggs = new LinkedHashMap<Short, MadSpawnEggInfo>();

    static MadSpawnEggInfo getEggInfo(short id)
    {
        return eggs.get(id);
    }

    static Collection<MadSpawnEggInfo> getEggInfoList()
    {
        return Collections.unmodifiableCollection(eggs.values());
    }

    private static boolean isValidSpawnEggID(short id)
    {
        return !eggs.containsKey(id);
    }

    public static void registerSpawnEgg(MadSpawnEggInfo info) throws IllegalArgumentException
    {
        if (info == null)
        {
            throw new IllegalArgumentException("MadSpawnEggInfo cannot be null");
        }

        if (!isValidSpawnEggID(info.eggID))
        {
            throw new IllegalArgumentException("Duplicate GMO spawn egg with id " + info.eggID + "(" + info.mobID + ")");
        }

        eggs.put(info.eggID, info);
    }

}