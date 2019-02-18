/*
 *
 * Copyright 2015-2019 Martin Goellnitz
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
 * Simple but fully descriptive blob interface.
 * Instances describe a blob residing in a given property of a given content item.
 */
public interface Blob {

    /**
     * @return ID of the content object holding the blob
     */
    String getContentId();


    /**
     * @return name of the property holding the blob
     */
    String getPropertyName();


    /**
     * @return mime type of the contents of the blob
     */
    String getMimeType();


    /**
     * @return size of the blob in bytes
     */
    long getLen();


    /**
     * @return bytes of the blob as an array
     */
    byte[] getBytes();

} // Blob
