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
package coconat.internal.test;

import coconat.Blob;
import coconat.Content;
import coconat.Repository;
import coconat.internal.CoconatContentRepository;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Do some very basic test accessing a prepared read only database from CoreMedia 5.0 with hsqldb.
 */
@Test
public class CoconatRepositoryTest {

    @Test
    public void testRepository() {
        String dbDriver = "org.hsqldb.jdbcDriver";
        String dbUrl = "jdbc:hsqldb:src/test/resources/unittest;readonly=true";
        String dbUser = "sa";
        String dbPassword = "";
        Repository repository = new CoconatContentRepository(dbUrl, dbDriver, dbUser, dbPassword);
        Content home = repository.getChild("CoConAT/Home");
        Assert.assertNotNull(home, "root topic 'Home' not found");
        Assert.assertFalse(home.isEmpty(), "root topic must not have an empty property set");
        Assert.assertEquals(home.entrySet().size(), 16, "Unexpected number of properties for root topic");
        Assert.assertEquals(home.get("title"), "CoConAT", "Unexpected title found");
        Assert.assertEquals(home.get("teaser"), "<div xmlns=\"http://www.coremedia.com/2003/richtext-1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"><p>CoreMedia Content Access Tool. A far too simple library to access basic CoreMedia CMS content objects directly from the database using different languages for integration purposes.</p></div>", "Unexpected teaser found");
        Assert.assertTrue(home.containsKey("keywords"), "root topic should contain property keywords");
        Assert.assertTrue(home.containsValue("CoConAT"), "root topic should property value CoConAT in some property");
        Object l = home.get("logo");
        Assert.assertNotNull(l, "no logo found in root topic");
        List<Content> logos = (List<Content>) l;
        Assert.assertEquals(logos.size(), 1, "Expected to find exactly one logo");
        Content logo = logos.get(0);
        Assert.assertEquals(logo.keySet().size(), logo.values().size(), "Size of keys must match size of values for logo");
        Assert.assertEquals(logo.size(), 17, "Unexpected number of properties for logo");
        Assert.assertEquals(logo.get("width"), "200", "Unexpected width in logo");
        Assert.assertEquals(logo.get("height"), "94", "Unexpected height in logo");
        Assert.assertEquals(logo.getId(), "10", "Unexpected id for logo");
        Assert.assertEquals(""+logo, "10 :ImageData", "Unexpected string representation for logo");
        Object b = logo.get("data");
        Assert.assertNotNull(b, "no blob found in logo object");
        Blob blob = (Blob) b;
        Assert.assertEquals(blob.getLen(), 10657, "Unexpected number of bytes in blob");
        Assert.assertEquals(blob.getMimeType(), "image/png", "Unexpected mime type in blob");
        Assert.assertEquals(blob.getContentId(), "10", "Unexpected content id reference in blob");
        Assert.assertEquals(blob.getPropertyName(), "data", "Unexpected property name reference in blob");
        Object s = home.get("subTopics");
        Assert.assertNotNull(s, "no subtopics found in root topic");
    } // testRepository()


    /**
     * Test of the not public api elements.
     * These elements have been used in other projects before this one and were just not ripped out of the code and
     * this should be tested.
     */
    @Test
    public void testImplementation() {
        String dbDriver = "org.hsqldb.jdbcDriver";
        String dbUrl = "jdbc:hsqldb:src/test/resources/unittest;readonly=true";
        String dbUser = "sa";
        String dbPassword = "";
        CoconatContentRepository repository = new CoconatContentRepository(dbUrl, dbDriver, dbUser, dbPassword);
        Content homeFolder = repository.getChild("CoConAT");
        Assert.assertEquals(homeFolder.getId(), "9", "Unexpected id for home folder");
        Set<Content> topicSet = repository.getChildrenWithType(homeFolder.getId(), "Topic");
        Assert.assertEquals(topicSet.size(), 2, "Unexpected number of topics");
        for (Content topic : topicSet) {
            Set<String> referrerIds = repository.getReferrerIds(topic.getId(), "RootTopic", "subTopics");
            Assert.assertEquals(referrerIds.size(), 1, "Unexpected number of referrers to topic ");
        } // for
        List<Content> topics = repository.listContents("Topic", "AND lastname_ = 'coconat.php'", null, true);
        Assert.assertEquals(topics.size(), 1, "Unexpected number of topics with a certain name");
        Set<Content> children = repository.getChildren("9", "coco.*");
        Assert.assertEquals(children.size(), 2, "Unexpected number of topics");
    } // testImplementation()

} // CoconatRepositoryTest
