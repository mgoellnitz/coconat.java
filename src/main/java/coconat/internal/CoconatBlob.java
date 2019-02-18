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

import coconat.Blob;


/**
 * Simple implementation of the blob interface.
 */
public class CoconatBlob implements Blob {

    private final String contentId;

    private final String propertyName;

    private final String mimeType;

    private final long len;

    private final byte[] bytes;


    /**
     * Create a blob instance from a set of describing parameters.
     *
     * @param contentId ID of the content object holding the blob
     * @param propertyName name of the property holding the blob
     * @param mimeType mime type of the contents of the blob
     * @param len size of the blob in bytes
     * @param data bytes of the blob as an array
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public CoconatBlob(String contentId, String propertyName, String mimeType, long len, byte[] data) {
        this.contentId = contentId;
        this.propertyName = propertyName;
        this.mimeType = mimeType;
        this.len = len;
        this.bytes = data;
    } // CoconatBlob()


    @Override
    public String getContentId() {
        return contentId;
    }


    @Override
    public String getPropertyName() {
        return propertyName;
    }


    @Override
    public String getMimeType() {
        return mimeType;
    }


    @Override
    public long getLen() {
        return len;
    }


    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public byte[] getBytes() {
        return bytes;
    }

} // CoconatBlob
