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
import coconat.Repository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple non caching implementation of the content repository.
 */
@SuppressWarnings("PMD.GodClass")
public class CoconatContentRepository implements Repository {

    private static final Logger LOG = LoggerFactory.getLogger(CoconatContentRepository.class);

    private static final String VIRTUAL_PROPERTY_VERSION = "version_";

    private static final String VIRTUAL_PROPERTY_ID = "id_";

    private static final String VIRTUAL_PROPERTY_FOLDER_ID = "folderid_";

    private static final String VIRTUAL_PROPERTY_NAME = "name_";

    private static final String VIRTUAL_PROPERTY_TYPE = "documenttype_";

    private static final String SELECT_FROM_RESOURCES_CLAUSE = "SELECT * FROM Resources WHERE ";

    /**
     * describe which type are derived from which others - via documenttype definitions
     */
    private Map<String, String> parents = new HashMap<>();

    private Connection dbConnection;

    private Map<String, Object> additionalProperties = new HashMap<>();


    /**
     * Create a content repository instance from a DB connection descripbed by connection parameters.
     *
     * @param dbUrl JDBC URL of the database to be used for this repository instance
     * @param dbDriver class name of the JDBC driver to be used for the connection
     * @param dbUser user name to be used for the connection
     * @param dbPassword password to be used for the connection
     */
    public CoconatContentRepository(String dbUrl, String dbDriver, String dbUser, String dbPassword) {
        try {
            Class.forName(dbDriver).newInstance();
        } catch (RuntimeException|ClassNotFoundException|InstantiationException|IllegalAccessException ex) {
            LOG.error("() error loading driver {} ({}) ", dbDriver, this, ex);
        } // try/catch
        try {
            dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (RuntimeException|SQLException ex) {
            LOG.error("() error getting connection to {} as {}", dbUrl, dbUser, ex);
        } // try/catch
    } // CoconatContentRepository()


    /**
     * Get document type parent relation.
     *
     * @return map mapping document types names to the name of the corresponding parent document type
     */
    public Map<String, String> getParents() {
        return parents;
    }


    /**
     * Set document type parent.
     *
     * @param parents map mapping document types names to the name of the corresponding parent document type
     */
    public void setParents(Map<String, String> parents) {
        this.parents = parents;
    }


    /**
     * Get static additional properties.
     * Each content object generated through this repository implementation will receive these named values as properties.
     *
     * @return map mapping property names to their respective values
     */
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }


    /**
     * Set static additional properties.
     * Each content object generated through this repository implementation will receive these named values as properties.
     *
     * @param additionalProperties map mapping property names to their respective values
     */
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }


    /**
     * Get content object with a given ID.
     *
     * @param id ID of the object to look for
     * @return content object for the ID or null
     */
    @Override
    public Content getContent(String id) {
        Content result = null;

        String type = getType(id);
        if (type!=null) {
            Map<String, Object> properties = getProperties(type, id);
            properties.putAll(additionalProperties);
            result = createContent(id, type, properties);
        } // if

        return result;
    } // getContent()


    /**
     * Get content object described by its path in the repository.
     *
     * @param path path of the object to look for
     * @return content object described by the path or the root folder
     */
    @Override
    public Content getChild(String path) {
        return getContent(getChildId(path));
    } // getChild()


    /**
     * List content objects matching certain criteria.
     *
     * @param typeName exact document type name of the document to look for - no subtypes
     * @param optionalQuery SQL based where clause part to be used
     * @param orderProperty name of the property to sort result list
     * @param ascending true if sorting should be ascending - false otherwise
     * @return sorted list of matching content objects
     */
    public List<Content> listContentsOfExactType(String typeName, String optionalQuery, String orderProperty, Boolean ascending) {
        List<Content> result = new ArrayList<>();
        for (String id : listIds(typeName, optionalQuery, orderProperty, ascending)) {
            result.add(getContent(id));
        } // for
        return result;
    } // listBeansOfExactClass()


    /**
     * List content objects matching certain criteria.
     *
     * @param typeName document type name of the document to look for
     * @param optionalQuery SQL based where clause part to be used
     * @param orderProperty name of the property to sort result list
     * @param ascending true if sorting should be ascending - false otherwise
     * @return sorted list of matching content objects
     */
    public List<Content> listContents(String typeName, String optionalQuery, String orderProperty, Boolean ascending) {
        List<Content> result = new ArrayList<>();
        for (String t = typeName; t!=null; t = parents.get(t)) {
            result.addAll(listContentsOfExactType(t, optionalQuery, orderProperty, ascending));
        } // for
        return result;
    } // listBeans()


    /**
     * supporting methods for implementing CM style access to content *
     */
    /**
     * Get the properties for an object with a given type and id.
     *
     * The instance "id" must be of type "type" otherwise the method will fail!
     *
     * @param type document type of the content item to retrieve the properties map for
     * @param id content id of the content item to retrieve the properties map for
     * @return map mapping the property names to their respective values
     */
    protected Map<String, Object> getProperties(String type, String id) {
        Map<String, Object> properties = new HashMap<>();
        if ((type==null)||(type.length()==0)) {
            // it's most likely a folder
            return properties;
        } // if
        String query = "SELECT * FROM "+type+" WHERE "+VIRTUAL_PROPERTY_ID+" = "+id+" ORDER BY "+VIRTUAL_PROPERTY_VERSION+" DESC";
        String sqlError = "getProperties() query=";
        try (Statement baseStatement = dbConnection.createStatement(); ResultSet baseSet = baseStatement.executeQuery(query)) {
            if (baseSet.next()) {
                int contentId = baseSet.getInt(VIRTUAL_PROPERTY_ID);
                int version = baseSet.getInt(VIRTUAL_PROPERTY_VERSION);
                LOG.debug("getProperties() {}/{} :{}", contentId, version, type);

                ResultSetMetaData metaData = baseSet.getMetaData();
                for (int i = 1; i<=metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = baseSet.getObject(i);
                    LOG.debug("getProperties() property {} = {}", columnName, value);
                    properties.put(columnName, value);
                } // for

                // select links
                query = "SELECT * FROM LinkLists WHERE sourcedocument = "+id+" AND sourceversion = "+version
                        +" ORDER BY propertyname ASC, linkindex ASC";
                Map<String, List<String>> linkLists = new HashMap<>();
                try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
                    while (resultSet.next()) {
                        String propertyName = resultSet.getString("propertyname");
                        String targetId = resultSet.getString("targetdocument");
                        int linkIndex = resultSet.getInt("linkindex");
                        LOG.debug("getProperties() {}[{}] {}", propertyName, linkIndex, targetId);
                        List<String> ids = linkLists.get(propertyName);
                        if (ids==null) {
                            ids = new ArrayList<>();
                            linkLists.put(propertyName, ids);
                        } // if
                        ids.add(linkIndex, targetId);
                    } // while
                } catch (SQLException se) {
                    LOG.error(sqlError+query, se);
                } // try/catch
                for (Entry<String, List<String>> entry : linkLists.entrySet()) {
                    properties.put(entry.getKey(), new LazyContentList(this, entry.getValue()));
                } // for

                // select blobs
                query = "SELECT * FROM Blobs WHERE documentid = "+id+" AND documentversion = "+version+" ORDER BY propertyname ASC";
                try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
                    while (resultSet.next()) {
                        String propertyName = resultSet.getString("propertyname");
                        int blobId = resultSet.getInt("target");

                        query = "SELECT * FROM BlobData WHERE id = "+blobId;
                        try (Statement st = dbConnection.createStatement(); ResultSet blobSet = st.executeQuery(query)) {
                            if (blobSet.next()) {
                                String mimeType = blobSet.getString("mimetype");
                                byte[] data = blobSet.getBytes("data");
                                long len = blobSet.getLong("len");
                                properties.put(propertyName, createBlob(id, propertyName, mimeType, len, data));
                                LOG.debug("getProperties() {} blob bytes {} ({})", propertyName, data.length, len);
                            } // if
                        } catch (SQLException se) {
                            LOG.error(sqlError+query, se);
                        } // try/catch
                    } // while
                } catch (SQLException se) {
                    LOG.error(sqlError+query, se);
                } // try/catch

                // select xml
                query = "SELECT * FROM Texts WHERE documentid = "+id+" AND documentversion = "+version+" ORDER BY propertyname ASC";
                try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
                    while (resultSet.next()) {
                        String propertyName = resultSet.getString("propertyname");
                        int target = resultSet.getInt("target");
                        // int segment = resultSet.getInt("segment");

                        StringBuilder text = new StringBuilder(256);
                        query = "SELECT * FROM SgmlText WHERE id = "+target;
                        try (Statement st = dbConnection.createStatement(); ResultSet textSet = st.executeQuery(query)) {
                            while (textSet.next()) {
                                String xmlText = textSet.getString("text");
                                text.append(xmlText);
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("getBean() "+propertyName+" "+textSet.getInt("id")+" "+textSet.getInt("segmentno")+" "+xmlText);
                                } // if
                            } // if
                        } catch (SQLException se) {
                            LOG.error(sqlError+query, se);
                        } // try/catch
                        LOG.debug("getProperties() {} text={}", propertyName, text.toString());

                        query = "SELECT * FROM SgmlData WHERE id = "+target;
                        StringBuilder data = new StringBuilder(256);
                        try (Statement sd = dbConnection.createStatement(); ResultSet dataSet = sd.executeQuery(query)) {
                            while (dataSet.next()) {
                                String xmlData = dataSet.getString("data");
                                data.append(xmlData);
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug(sqlError+propertyName+" "+dataSet.getInt("id")+" "+dataSet.getInt("segmentno")+" "
                                            +xmlData);
                                } // if
                            } // if
                        } catch (SQLException se) {
                            LOG.error(sqlError+query, se);
                        } // try/catch
                        LOG.debug("getProperties() {} data={}", propertyName, data.toString());

                        try {
                            properties.put(propertyName, CoconatTextConverter.convert(text, data));
                        } catch (Exception e) {
                            LOG.error("getProperties() ignoring richtext", e);
                            properties.put(propertyName, text.toString());
                        } // try/catch
                    } // while
                } catch (SQLException se) {
                    LOG.error(sqlError+query, se);
                } // try/catch
            } // if
        } catch (SQLException se) {
            LOG.error(sqlError+query, se);
        } // try/catch
        return properties;
    } // getProperties()


    /**
     * Get document type for a given content object.
     *
     * @param id ID of the object to get the document type for
     * @return document type name or null
     */
    public String getType(String id) {
        String type = null;
        String query = SELECT_FROM_RESOURCES_CLAUSE+VIRTUAL_PROPERTY_ID+" = '"+id+"'";
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
            if (resultSet.next()) {
                type = resultSet.getString(VIRTUAL_PROPERTY_TYPE);
                if (LOG.isDebugEnabled()) {
                    int contentId = resultSet.getInt(VIRTUAL_PROPERTY_ID);
                    LOG.debug("getType() "+contentId+": "+type);
                } // if
                if (type==null) {
                    type = ""; // Folder indication
                } // if
            } // if
        } catch (SQLException se) {
            LOG.error("getType()", se);
        } // try/catch
        return type;
    } // getType()


    /**
     * Get ID for a content object described by its path in the repository.
     *
     * @param path path of the object to look for
     * @return ID of the object described by the path or at least "1" for the root folder
     */
    public String getChildId(String path) {
        try {
            String[] arcs = path.split("/");
            String currentFolder = "1"; // root
            for (String folder : arcs) {
                LOG.info("getChildId() lookup up {} in id {}", folder, currentFolder);
                if (folder.length()>0) {
                    currentFolder = getChildId(folder, currentFolder);
                } // if
            } // for
            return currentFolder;
        } catch (RuntimeException se) {
            LOG.error("getChildId()", se);
        } // try/catch
        return null;
    } // getChildId()


    /**
     * Get the ID of the parent folder for a given content object.
     *
     * @param childId ID of the child to find the parent for
     * @return ID of the parent - or null
     */
    public String getParentId(String childId) {
        String id = null;
        String query = SELECT_FROM_RESOURCES_CLAUSE+VIRTUAL_PROPERTY_ID+" = "+childId;
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
            if (resultSet.next()) {
                id = ""+resultSet.getInt(VIRTUAL_PROPERTY_FOLDER_ID);
                LOG.debug("getParentId() {}: {}", childId, id);
            } // if
        } catch (SQLException se) {
            LOG.error("getParentId() "+query, se);
        } // try/catch
        return id;
    } // getParentId()


    /**
     * List IDs of documents fulfilling certain criteria.
     *
     * @param typeName exact document type name of the document to look for - no subtypes
     * @param optionalQuery SQL based where clause part to be used
     * @param orderProperty name of the property to sort result list
     * @param ascending true if sorting should be ascending - false otherwise
     * @return sorted list of IDs of matching content objects
     */
    public List<String> listIds(String typeName, String optionalQuery, String orderProperty, Boolean ascending) {
        List<String> ids = new ArrayList<>();
        @SuppressWarnings("PMD.ConsecutiveLiteralAppends") // Enhance readability using more than one line
        StringBuilder query = new StringBuilder(128).append("SELECT ").append(VIRTUAL_PROPERTY_ID);
        query.append(" FROM Resources WHERE ").append(VIRTUAL_PROPERTY_TYPE).append(" = '").append(typeName).append("' ");
        if (optionalQuery!=null) {
            query.append(optionalQuery);
        } // if
        if (orderProperty!=null) {
            String asc = (ascending==null) ? "ASC" : (ascending ? "ASC" : "DESC");
            String order = orderProperty+" "+asc;
            query.append(" ORDER BY ");
            query.append(order);
        } // if
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query.toString())) {
            while (resultSet.next()) {
                int contentId = resultSet.getInt(VIRTUAL_PROPERTY_ID);
                ids.add(""+contentId);
                LOG.debug("getBean() {}", contentId);
            } // while
        } catch (SQLException se) {
            LOG.error("listIds() "+query, se);
        } // try/catch
        return ids;
    } // listIds()


    /**
     * Get ID of a child in a given folder with a certain name.
     *
     * @param name name of the child to find
     * @param parentId id of the folder so search in
     * @return id of the object or null
     */
    public String getChildId(String name, String parentId) {
        String id = null;
        String q = SELECT_FROM_RESOURCES_CLAUSE+VIRTUAL_PROPERTY_FOLDER_ID+" = "+parentId+" AND "+VIRTUAL_PROPERTY_NAME+" = '"+name+"'";
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(q)) {
            if (resultSet.next()) {
                id = ""+resultSet.getInt(VIRTUAL_PROPERTY_ID);
                LOG.debug("getChildId() {}/{}: {}", parentId, name, id);
            } // if
        } catch (SQLException se) {
            LOG.error("getChildId() "+q, se);
        } // try/catch
        return id;
    } // getChildId()


    /**
     * Get ids of the objects in a folder with a certain type where the name matches a given pattern.
     *
     * @param parentId id of the folder
     * @type document type name the children must fulfill
     * @param pattern pattern used for name matching
     * @return set of IDs of the objects in the folder matching the pattern
     */
    public Set<String> getChildrenIds(String parentId, String type, String pattern) {
        LOG.info("getChildrenIds() parentId={} type={} pattern={}", parentId, type, pattern);
        Pattern p = null;
        if (pattern!=null) {
            p = Pattern.compile(pattern);
        } // if
        Set<String> result = new HashSet<>();
        StringBuilder query = new StringBuilder(SELECT_FROM_RESOURCES_CLAUSE);
        query.append(VIRTUAL_PROPERTY_FOLDER_ID).append(" = ").append(parentId);
        if (type!=null) {
            query.append(" AND ").append(VIRTUAL_PROPERTY_TYPE).append(" = '").append(type).append('\'');
        } // if
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query.toString())) {
            while (resultSet.next()) {
                String id = ""+resultSet.getInt(VIRTUAL_PROPERTY_ID);
                String name = resultSet.getString(VIRTUAL_PROPERTY_NAME);
                LOG.info("getChildrenIds() {}/{}: {}", parentId, name, id);
                if (p==null) {
                    result.add(id);
                } else {
                    if (p.matcher(name).matches()) {
                        LOG.info("getChildrenIds() match!");
                        result.add(id);
                    } // if
                } // if
            } // if
        } catch (SQLException se) {
            LOG.error("getChildrenIds() "+query, se);
        } // try/catch
        return result;
    } // getChildrenIds()


    /**
     * Get ids of the objects in a folder where the name matches a given pattern.
     *
     * @param parentId id of the folder
     * @param pattern pattern used for name matching
     * @return set of IDs of the objects in the folder matching the pattern
     */
    public Set<String> getChildrenIds(String parentId, String pattern) {
        return getChildrenIds(parentId, null, pattern);
    } // getChildrenIds()


    /**
     * Get ids of the object in a folder.
     *
     * @param parentId id of the folder
     * @return set of IDs of the objects in the folder
     */
    public Set<String> getChildrenIds(String parentId) {
        return getChildrenIds(parentId, null);
    } // getChildrenIds()


    /**
     * Get ids of object from a given folder with a certain type.
     *
     * @param parentId id of the folder to search in
     * @param type document type name the children must fulfill
     * @return set of IDs of objects
     */
    public Set<String> getChildrenWithTypeIds(String parentId, String type) {
        return getChildrenIds(parentId, type, null);
    } // getChildrenWithTypeIds()


    /**
     * Return a set of IDs of objects refering a given object.
     *
     * @param targetId id of the object to find referrers for
     * @param type document type the referrers must fulfill
     * @param property name of the property the referrings object use to point to the target
     * @return set of content ids
     */
    public Set<String> getReferrerIds(String targetId, String type, String property) {
        LOG.info("getReferrerIds() targetId={} type={} property={}", targetId, type, property);
        Set<String> result = new HashSet<>();
        StringBuilder query = new StringBuilder("SELECT * FROM LinkLists WHERE targetdocument = ");
        query.append(targetId);
        if (property!=null) {
            query.append(" AND propertyname = '").append(property).append('\'');
        } // if
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query.toString())) {
            while (resultSet.next()) {
                String sourceId = ""+resultSet.getInt("sourcedocument");
                String sourceVersion = ""+resultSet.getInt("sourceversion");
                LOG.info("getReferrerIds() {}/{}/#{} -> {}", sourceId, sourceVersion, property, targetId);
                // Check for latest version referencing the object
                final String sourceType = getType(sourceId);
                final Map<String, Object> properties = getProperties(sourceType, sourceId);
                Object version = properties.get("version_");
                if (version==null) {
                    version = properties.get("VERSION_");
                } // if
                LOG.info("getReferrerIds() version={} :{}", version, (version==null ? "null" : version.getClass().getName()));
                if ((!result.contains(sourceId))&&sourceVersion.equals(version.toString())) {
                    result.add(sourceId);
                } // if
            } // if
        } catch (SQLException se) {
            LOG.error("getReferrerIds() "+query, se);
        } // try/catch
        return result;
    } // getReferrerIds()


    /**
     * Create a transient blob object from content instance.
     *
     * @param id id of the content
     * @param propertyName name of the property to find the blob in
     * @param mimeType mimetype to choose
     * @param len length in bytes
     * @param data blob data
     * @return return abstrace representation of the blob
     */
    protected Object createBlob(String id, String propertyName, String mimeType, long len, byte[] data) {
        return new CoconatBlob(id, propertyName, mimeType, len, data);
    } // createBlob()


    /**
     * Create a content instance for the given id and type with the given properties.
     *
     * @param id content id in long string form
     * @param type textual type identifier
     * @param properties property map with all properties
     * @return content instance - may not be null
     */
    protected Content createContent(String id, String type, Map<String, Object> properties) {
        return new CoconatContent(id, type, properties);
    } // createContent()


    /**
     * Get children of a folder with a certain type.
     *
     * @param parentId if of the folder
     * @param type type of the children to find
     * @return set of content objects
     */
    public Set<Content> getChildrenWithType(String parentId, String type) {
        Set<Content> result = new HashSet<>();
        for (String id : getChildrenWithTypeIds(parentId, type)) {
            result.add(getContent(id));
        } // for
        LOG.debug("getChildrenWithType() size={}", result.size());
        return result;
    } // getChildrenWithType()


    /**
     * Get children of a folder matching a name pattern.
     *
     * @param startFolderId id of the folder to search in
     * @param pattern pattern for matching
     * @return set of content objects
     */
    public Set<Content> getChildren(String startFolderId, String pattern) {
        LOG.info("getChildren() {}", startFolderId);
        Set<String> resultIds = getChildrenIds(startFolderId, pattern);
        Set<Content> results = new HashSet<>();
        for (String id : resultIds) {
            results.add(getContent(id));
        } // for
        return results;
    } // getChildren()

} // CoconatContentRepository
