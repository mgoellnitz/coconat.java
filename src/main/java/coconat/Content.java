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
package coconat;

import java.util.Map;


/**
 * Basic interface all items from the repository will fulfill.
 */
public interface Content extends Map<String, Object>, Comparable<Content> {

    /**
     * Return the id if this instance.
     *
     * @return String based formatted content id.
     */
    String getId();

} // Content
