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

import coconat.internal.CoconatContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Quite simple test program.
 */
public final class ReadContent {

    private static final Logger LOG = LoggerFactory.getLogger(ReadContent.class);


    private ReadContent() {
    }


    /**
     * Test to fetch content from a localhost MySQL database with the ID of 58.
     *
     * @param args arguments are ignored
     */
    public static void main(String[] args) {
        LOG.info("main()");
        String dbDriver = "com.mysql.jdbc.Driver";
        String dbUrl = "jdbc:mysql://localhost:3306/cm_management";
        String dbUser = "cm_management";
        String dbPassword = "cm_management";
        Repository repository = new CoconatContentRepository(dbUrl, dbDriver, dbUser, dbPassword);
        Content content = repository.getContent("58");
        LOG.info("main() content {}", content);
        LOG.info("main() properties {}", content.keySet());
        LOG.info("main() property placement {}", content.get("placement"));
    } // main()

} // ReadContent
