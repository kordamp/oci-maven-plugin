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
package org.kordamp.maven.plugin.oci.mojos.create

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.AutonomousDatabase
import com.oracle.bmc.database.model.AutonomousDatabaseSummary
import com.oracle.bmc.database.model.CreateAutonomousDatabaseDetails
import com.oracle.bmc.database.requests.CreateAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.GetAutonomousDatabaseRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.AdminPasswordAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.DatabaseNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalCpuCoreCountAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDataStorageSizeAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.AutonomousDatabasePrinter.printAutonomousDatabase

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'create-autonomous-database')
class CreateAutonomousDatabaseMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    DatabaseNameAwareTrait,
    AdminPasswordAwareTrait,
    OptionalCpuCoreCountAwareTrait,
    OptionalDataStorageSizeAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    String createdAutonomousDatabaseId
    private static final Random RANDOM = new Random()

    @Override
    void executeGoal() {
        validateCompartmentId()
        validateDatabaseName()
        validateAdminPassword()

        DatabaseClient client = createDatabaseClient()

        AutonomousDatabase database = maybeCreateAutonomousDatabase(this,
            client,
            getCompartmentId(),
            getDatabaseName(),
            getAdminPassword(),
            getCpuCoreCount() ?: 1,
            getDataStorageSize() ?: 1,
            isWaitForCompletion(),
            isVerbose())
        setCreatedAutonomousDatabaseId(database.id)
    }

    static AutonomousDatabase maybeCreateAutonomousDatabase(OCIMojo owner,
                                                            DatabaseClient client,
                                                            String compartmentId,
                                                            String databaseName,
                                                            String adminPassword,
                                                            int cpuCoreCount,
                                                            int dataStorageSize,
                                                            boolean waitForCompletion,
                                                            boolean verbose) {
        // 1. Check if it exists
        List<AutonomousDatabaseSummary> databases = client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
            .compartmentId(compartmentId)
            .displayName(databaseName)
            .build())
            .items

        if (!databases.empty) {
            AutonomousDatabase database = client.getAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                .autonomousDatabaseId(databases[0].id)
                .build())
                .autonomousDatabase
            println("Autonomous Database '${databaseName}' already exists. id = ${owner.console.yellow(database.id)}")
            if (verbose) printAutonomousDatabase(owner, database, 0)
            return database
        }

        // 2. Create
        println('Provisioning Autonomous Database. This may take a while.')
        AutonomousDatabase database = client.createAutonomousDatabase(CreateAutonomousDatabaseRequest.builder()
            .createAutonomousDatabaseDetails(CreateAutonomousDatabaseDetails.builder()
                .compartmentId(compartmentId)
                .displayName(databaseName)
                .adminPassword(adminPassword)
                .cpuCoreCount(cpuCoreCount)
                .dataStorageSizeInTBs(dataStorageSize)
                .dbName(databaseName + RANDOM.nextInt(500))
                .dbWorkload(CreateAutonomousDatabaseDetails.DbWorkload.Oltp)
                .licenseModel(CreateAutonomousDatabaseDetails.LicenseModel.LicenseIncluded)
                .build())
            .build())
            .autonomousDatabase

        if (waitForCompletion) {
            println("Waiting for Autonomous Database to be ${owner.state('Available')}")
            client.waiters
                .forAutonomousDatabase(GetAutonomousDatabaseRequest.builder()
                    .autonomousDatabaseId(database.id)
                    .build(),
                    AutonomousDatabase.LifecycleState.Available)
                .execute()
        }

        println("Autonomous Database '${databaseName}' has been provisioned. id = ${owner.console.yellow(database.id)}")
        if (verbose) printAutonomousDatabase(owner, database, 0)
        database
    }
}
