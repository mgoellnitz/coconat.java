/**
 *
 * Copyright 2015 Martin Goellnitz
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
import coconat.Repository;
import java.util.AbstractList;
import java.util.List;

public class LazyContentList extends AbstractList<Content> implements List<Content> {

    private final Repository repository;

    private final List<String> idList;


    public LazyContentList(Repository repository, List<String> idList) {
        this.repository = repository;
        this.idList = idList;
    } // LazyContentList()


    @Override
    public Content get(int index) {
        return repository.getContent(idList.get(index));
    } // get()


    @Override
    public int size() {
        return idList.size();
    } // size()

} // LazyContentList
