/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021 Andres Almiray.
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
package org.kordamp.maven.plugin.oci.mojos.autonomous

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import com.oracle.bmc.database.requests.RestartAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.StartAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.StopAutonomousDatabaseRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDatabaseIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDatabaseNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'autonomous-database-action')
class AutonomousDatabaseActionMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    OptionalDatabaseIdAwareTrait,
    OptionalDatabaseNameAwareTrait,
    WaitForCompletionAwareTrait {
    private static enum DatabaseAction {
        START(AutonomousDatabase.LifecycleState.Available),
        STOP(AutonomousDatabase.LifecycleState.Stopped),
        RESTART(AutonomousDatabase.LifecycleState.Available)

        private AutonomousDatabase.LifecycleState state

        DatabaseAction(AutonomousDatabase.LifecycleState state) {
            this.state = state
        }

        AutonomousDatabase.LifecycleState state() {
            this.state
        }
    }

    @Parameter(property = 'oci.database.action')
    DatabaseAction action

    DatabaseAction getAction() {
        DatabaseAction.valueOf(stringProperty(this, 'OCI_DATABASE_ACTION', 'oci.database.action', (this.@action ?: DatabaseAction.STOP).name()).toUpperCase())
    }

    @Override
    void executeGoal() {
        validateDatabaseId()

        if (isBlank(getDatabaseId()) && isBlank(getDatabaseName())) {
            throw new IllegalStateException("Missing value for either 'databaseId' or 'databaseName' in $path")
        }
        if (!getAction()) {
            throw new IllegalStateException("Missing value for 'action' in $path")
        }

        DatabaseClient client = createDatabaseClient()

        if (isNotBlank(getDatabaseId())) {
            databaseAction(client, getDatabaseId(), getAction())
        } else {
            validateCompartmentId()

            client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
                .compartmentId(getCompartmentId())
                .displayName(getDatabaseName())
                .build())
                .items.each { database ->
                setDatabaseId(database.id)
                databaseAction(client, database.id, getAction())
            }
        }
    }

    private void databaseAction(DatabaseClient client, String databaseId, DatabaseAction action) {
        println("Sending ${getAction().name()} to Database with id ${console.yellow(databaseId)}")

        switch (action) {
            case DatabaseAction.START:
                client.startAutonomousDatabase(StartAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId)
                    .build())
                break
            case DatabaseAction.STOP:
                client.stopAutonomousDatabase(StopAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId)
                    .build())
                break
            case DatabaseAction.RESTART:
                client.restartAutonomousDatabase(RestartAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId)
                    .build())
                break
        }

        if (isWaitForCompletion()) {
            println("Waiting for Database to be ${state(action.state().name())}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(databaseId).build(),
                    action.state())
                .execute()
        }
    }
}

