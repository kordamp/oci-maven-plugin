/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.maven.plugin.oci.mojos.get

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.DatabaseIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.AutonomousDatabasePrinter.printAutonomousDatabase

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'get-autonomous-dabase')
class GetAutonomousDatabaseMojo extends AbstractOCIMojo implements DatabaseIdAwareTrait {
    @Override
    void executeGoal() {
        validateDatabaseId()

        DatabaseClient client = createDatabaseClient()

        AutonomousDatabase database = client.getAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
            .autonomousDatabaseId(getDatabaseId())
            .build())
            .autonomousDatabase

        if (database) {
            println(database.displayName + ':')
            printAutonomousDatabase(this, database, 0)
        } else {
            println("Database with id ${getDatabaseId()} was not found")
        }
    }
}
