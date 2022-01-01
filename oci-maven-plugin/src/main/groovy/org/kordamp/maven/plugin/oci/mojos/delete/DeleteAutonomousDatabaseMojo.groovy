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
package org.kordamp.maven.plugin.oci.mojos.delete

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.model.AutonomousDatabaseSummary
import com.oracle.bmc.database.requests.DeleteAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDatabaseIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDatabaseNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.StringUtils.isBlank
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'delete-autonomous-dabase')
class DeleteAutonomousDatabaseMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    OptionalDatabaseIdAwareTrait,
    OptionalDatabaseNameAwareTrait,
    WaitForCompletionAwareTrait {
    @Override
    void executeGoal() {
        validateDatabaseId()

        if (isBlank(getDatabaseId()) && isBlank(getDatabaseName())) {
            throw new IllegalStateException("Missing value for either 'databaseId' or 'databaseName' in $path")
        }

        DatabaseClient client = createDatabaseClient()

        // TODO: check if database exists
        // TODO: check is database is in a 'deletable' state

        if (isNotBlank(getDatabaseId())) {
            AutonomousDatabase database = client.getAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                .autonomousDatabaseId(getDatabaseId())
                .build())
                .autonomousDatabase

            if (database) {
                setDatabaseName(database.displayName)
                deleteDatabase(client, database)
            }
        } else {
            validateCompartmentId()

            client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
                .compartmentId(getCompartmentId())
                .displayName(getDatabaseName())
                .build())
                .items.each { database ->
                setDatabaseId(database.id)
                deleteDatabase(client, database)
            }
        }
    }

    private void deleteDatabase(DatabaseClient client, AutonomousDatabase database) {
        println("Deleting Database '${database.displayName}' with id ${database.id}")
        client.deleteAutonomousDatabase(DeleteAutonomousDatabaseRequest.builder()
            .autonomousDatabaseId(database.id)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for Database to be ${state('Terminated')}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(database.id).build(),
                    AutonomousDatabase.LifecycleState.Terminated)
                .execute()
        }
    }

    private void deleteDatabase(DatabaseClient client, AutonomousDatabaseSummary database) {
        println("Deleting Database '${database.displayName}' with id ${database.id}")
        client.deleteAutonomousDatabase(DeleteAutonomousDatabaseRequest.builder()
            .autonomousDatabaseId(database.id)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for Database to be ${state('Terminated')}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder().autonomousDatabaseId(database.id).build(),
                    AutonomousDatabase.LifecycleState.Terminated)
                .execute()
        }
    }
}
