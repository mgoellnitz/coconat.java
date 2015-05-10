/*
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
package coconat;


/**
 * The interface to access the underlying repository.
 */
public interface Repository {

    /**
     * retrieves item with the given id from the repository.
     *
     * @param id
     * @return respective content item or null if not available
     */
    Content getContent(String id);

    /**
     * retrieves item with the given path from the repository.
     *
     * @param path path relative to the root of the repository
     * @return respective content item or null if not available
     */
    Content getChild(String path);

} // Repository
