/**
 *
 * Copyright 2011-2015 Martin Goellnitz
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
public class CoconatContentRepository implements Repository {

    private static final Logger LOG = LoggerFactory.getLogger(CoconatContentRepository.class);

    /**
     * describe which type are derived from which others - via documenttype definitions
     */
    private Map<String, String> parents = new HashMap<>();

    private Connection dbConnection;

    private final String dbUrl;

    private final String dbDriver;

    private final String dbUser;

    private final String dbPassword;

    private Map<String, Object> additionalProperties = new HashMap<>();


    public CoconatContentRepository(String dbUrl, String dbDriver, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        try {
            Class.forName(this.dbDriver).newInstance();
        } catch (RuntimeException|ClassNotFoundException|InstantiationException|IllegalAccessException ex) {
            LOG.error("() error loading driver {} ({}) ", dbDriver, this, ex);
        } // try/catch
        try {
            dbConnection = DriverManager.getConnection(this.dbUrl, this.dbUser, this.dbPassword);
        } catch (RuntimeException|SQLException ex) {
            LOG.error("() error getting connection to {} as {}", this.dbUrl, this.dbUser, ex);
        } // try/catch
    } // CoconatContentRepository()


    public Map<String, String> getParents() {
        return parents;
    }


    public void setParents(Map<String, String> parents) {
        this.parents = parents;
    }


    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }


    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }


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


    @Override
    public Content getChild(String path) {
        return getContent(getChildId(path));
    } // getChild()


    public List<Content> listContentsOfExactType(String typeName, String optionalQuery, String orderProperty, Boolean ascending) {
        List<Content> result = new ArrayList<>();
        for (String id : listIds(typeName, optionalQuery, orderProperty, ascending)) {
            result.add(getContent(id));
        } // for
        return result;
    } // listBeansOfExactClass()


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
        String query = "SELECT * FROM "+type+" WHERE id_ = "+id+" ORDER BY version_ DESC";
        try (Statement baseStatement = dbConnection.createStatement(); ResultSet baseSet = baseStatement.executeQuery(query)) {
            if (baseSet.next()) {
                int contentId = baseSet.getInt("id_");
                int version = baseSet.getInt("version_");
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
                    LOG.error("getProperties() "+query, se);
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
                            LOG.error("getProperties() "+query, se);
                        } // try/catch
                    } // while
                } catch (SQLException se) {
                    LOG.error("getProperties() "+query, se);
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
                            LOG.error("getProperties() "+query, se);
                        } // try/catch
                        LOG.debug("getProperties() {} text={}", propertyName, text.toString());

                        query = "SELECT * FROM SgmlData WHERE id = "+target;
                        StringBuilder data = new StringBuilder(256);
                        try (Statement sd = dbConnection.createStatement(); ResultSet dataSet = sd.executeQuery(query)) {
                            while (dataSet.next()) {
                                String xmlData = dataSet.getString("data");
                                data.append(xmlData);
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("getProperties() "+propertyName+" "+dataSet.getInt("id")+" "+dataSet.getInt("segmentno")+" "
                                            +xmlData);
                                } // if
                            } // if
                        } catch (SQLException se) {
                            LOG.error("getProperties() "+query, se);
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
                    LOG.error("getProperties() "+query, se);
                } // try/catch
            } // if
        } catch (SQLException se) {
            LOG.error("getProperties() "+query, se);
        } // try/catch
        return properties;
    } // getProperties()


    public String getType(String id) {
        String type = null;
        String query = "SELECT * FROM Resources WHERE id_ = '"+id+"'";
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
            if (resultSet.next()) {
                type = resultSet.getString("documenttype_");
                if (LOG.isDebugEnabled()) {
                    int contentId = resultSet.getInt("id_");
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


    public Set<String> listIds(String typeName, String optionalQuery, String orderProperty, Boolean ascending) {
        Set<String> ids = new HashSet<>();
        String query = "SELECT id_ FROM Resources WHERE documenttype_ = '"+typeName+"' ";
        if (optionalQuery!=null) {
            query += optionalQuery;
        } // if
        if (orderProperty!=null) {
            String asc = "ASC";
            if (ascending!=null) {
                asc = ascending ? "ASC" : "DESC";
            } // if
            String order = orderProperty+" "+asc;
            query += " ORDER BY "+order;
        } // if
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
            while (resultSet.next()) {
                if (LOG.isInfoEnabled()) {
                    int contentId = resultSet.getInt("id_");
                    ids.add(""+contentId);
                    LOG.debug("getBean() {}", contentId);
                } // if
            } // while
        } catch (SQLException se) {
            LOG.error("listIds() "+query, se);
        } // try/catch
        return ids;
    } // listIds()


    public String getChildId(String name, String parentId) {
        String id = null;
        String query = "SELECT * FROM Resources WHERE folderid_ = "+parentId+" AND name_ = '"+name+"'";
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
            if (resultSet.next()) {
                id = ""+resultSet.getInt("id_");
                LOG.debug("getChildId() {}/{}: {}", parentId, name, id);
            } // if
        } catch (SQLException se) {
            LOG.error("getChildId() "+query, se);
        } // try/catch
        return id;
    } // getChildId()


    public Set<String> getChildrenIds(String parentId, String type, String pattern) {
        LOG.info("getChildrenIds() parentId={} type={} pattern={}", parentId, type, pattern);
        Pattern p = null;
        if (pattern!=null) {
            p = Pattern.compile(pattern);
        } // if
        Set<String> result = new HashSet<>();
        String query = "SELECT * FROM Resources WHERE folderid_ = "+parentId;
        if (type!=null) {
            query += " AND documenttype_ = '"+type+"'";
        } // if
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
            while (resultSet.next()) {
                String id = ""+resultSet.getInt("id_");
                String name = resultSet.getString("name_");
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


    public Set<String> getChildrenIds(String parentId, String pattern) {
        return getChildrenIds(parentId, null, pattern);
    } // getChildrenIds()


    public Set<String> getChildrenIds(String parentId) {
        return getChildrenIds(parentId, null);
    } // getChildrenIds()


    public Set<String> getChildrenWithTypeIds(String parentId, String type) {
        return getChildrenIds(parentId, type, null);
    } // getChildrenWithTypeIds()


    public Set<String> getReferrerIds(String targetId, String type, String property) {
        LOG.info("getReferrerIds() targetId={} type={} property={}", targetId, type, property);
        Set<String> result = new HashSet<>();
        String query = "SELECT * FROM LinkLists WHERE targetdocument = "+targetId;
        if (property!=null) {
            query += " AND propertyname = '"+property+"'";
        } // if
        try (Statement s = dbConnection.createStatement(); ResultSet resultSet = s.executeQuery(query)) {
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
                LOG.info("getReferrerIds() version={} :{}", version, (version!=null ? version.getClass().getName() : "null"));
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


    public Set<Content> getChildrenWithType(String parentId, String type) {
        Set<Content> result = new HashSet<>();
        for (String id : getChildrenWithTypeIds(parentId, type)) {
            result.add(getContent(id));
        } // for
        LOG.debug("getChildrenWithType() size={}", result.size());
        return result;
    } // getChildrenWithType()


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
