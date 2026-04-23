package org.matsim.contrib.llm.chatcommons;


import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * Container for managing chat manager instances.
 * Stores chat managers by ID and by person ID for quick lookup.
 */
public class ChatManagerContainer {

    private final Map<Id<?>, IChatManager> chatManagerMap = new ConcurrentHashMap<>();
    private final Map<Id<Person>,IChatManager> personChatManagerMap = new ConcurrentHashMap<>();

    /**
     * Adds a chat manager to the container.
     */
    public void add(IChatManager chatManager) {
        chatManagerMap.put(chatManager.getId(), chatManager);
        if(chatManager.getPersonId()!=null)this.personChatManagerMap.put(chatManager.getPersonId(), chatManager);
    }

    /**
     * Gets a chat manager by ID.
     */
    public IChatManager get(Id<?> id) {
        return chatManagerMap.get(id);
    }

    /**
     * Removes a chat manager by ID.
     */
    public void remove(Id<?> id) {
        chatManagerMap.remove(id);
    }

    /**
     * Clears all chat managers from the container.
     */
    public void clear() {
        chatManagerMap.clear();
        personChatManagerMap.clear();
    }

    /**
     * Returns all chat managers as an unmodifiable map.
     */
    public Map<Id<?>, IChatManager> getAll() {
        return Collections.unmodifiableMap(chatManagerMap);
    }

    /**
     * Checks if a chat manager exists by ID.
     */
    public boolean contains(Id<?> id) {
        return chatManagerMap.containsKey(id);
    }

    /**
     * Returns the number of chat managers.
     */
    public int size() {
        return chatManagerMap.size();
    }

    /**
     * Returns all chat managers keyed by person ID.
     */
    public Map<Id<Person>,IChatManager> getAllWithPersonKey(){
    	return this.personChatManagerMap;
    }

    /**
     * Gets a chat manager for a specific person.
     */
    public IChatManager getChatManagerForPerson(Id<Person> pId) {
    	return this.personChatManagerMap.get(pId);
    }

}

