package chatcommons;


import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class ChatManagerContainer {

    private final Map<Id<?>, IChatManager> chatManagerMap = new ConcurrentHashMap<>();
    private final Map<Id<Person>,IChatManager> personChatManagerMap = new ConcurrentHashMap<>();

    public void add(IChatManager chatManager) {
        chatManagerMap.put(chatManager.getId(), chatManager);
        if(chatManager.getPersonId()!=null)this.personChatManagerMap.put(chatManager.getPersonId(), chatManager);
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
    
    public Map<Id<Person>,IChatManager> getAllWithPersonKey(){
    	return this.personChatManagerMap;
    }
    
    public IChatManager getChatManagerForPerson(Id<Person> pId) {
    	return this.personChatManagerMap.get(pId);
    }
    
}

