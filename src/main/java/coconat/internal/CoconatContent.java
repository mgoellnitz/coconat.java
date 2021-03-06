/**
 *
 * Copyright 2011-2019 Martin Goellnitz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package coconat.internal;

import coconat.Content;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * Internal implementation of items from the CoconatContentRepository.
 */
public class CoconatContent implements Content, Map<String, Object> {

    private final String id;

    private final String documentType;

    private final Map<String, Object> properties;


    /**
     * Create a content object instance from a given ID, document type name a named properties.
     *
     * @param id ID to be assumed by the content object
     * @param type document type name of the content object
     * @param properties named values for the properties of the content object
     */
    public CoconatContent(String id, String type, Map<String, Object> properties) {
        this.id = id;
        this.documentType = type;
        this.properties = properties;
    } // ComeContent()


    @Override
    public String getId() {
        return id;
    }


    public String getDocumentType() {
        return documentType;
    }


    /*
     * Map Interface
     */
    
    @Override
    public void clear() {
        properties.clear();
    }


    @Override
    public Set<Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }


    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }


    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }


    @Override
    public Object put(String key, Object value) {
        return properties.put(key, value);
    }


    @Override
    public Object get(Object key) {
        return properties.get(key);
    }


    @Override
    public int size() {
        return properties.size();
    }


    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }


    @Override
    public Set<String> keySet() {
        return properties.keySet();
    }


    @Override
    public Object remove(Object key) {
        return properties.remove(key);
    }


    @Override
    public Collection<Object> values() {
        return properties.values();
    }


    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        properties.putAll(m);
    }


    @Override
    public int compareTo(Content c) {
        return getId().compareTo(c.getId());
    } // compareTo()


    @Override
    public String toString() {
        return getId()+" :"+getDocumentType();
    } // toString()

} // CoconatContent
