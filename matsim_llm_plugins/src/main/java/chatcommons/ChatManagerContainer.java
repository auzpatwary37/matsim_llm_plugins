package chatcommons;


import org.matsim.api.core.v01.Id;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManagerContainer {

    private final Map<Id<?>, IChatManager> chatManagerMap = new ConcurrentHashMap<>();

    public void add(IChatManager chatManager) {
        chatManagerMap.put(chatManager.getId(), chatManager);
    }

    public IChatManager get(Id<?> id) {
        return chatManagerMap.get(id);
    }

    public void remove(Id<?> id) {
        chatManagerMap.remove(id);
    }

    public void clear() {
        chatManagerMap.clear();
    }

    public Map<Id<?>, IChatManager> getAll() {
        return Collections.unmodifiableMap(chatManagerMap);
    }

    public boolean contains(Id<?> id) {
        return chatManagerMap.containsKey(id);
    }

    public int size() {
        return chatManagerMap.size();
    }
}

