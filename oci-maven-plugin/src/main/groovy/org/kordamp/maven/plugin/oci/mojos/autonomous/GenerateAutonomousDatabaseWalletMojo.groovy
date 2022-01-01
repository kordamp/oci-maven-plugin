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
package org.kordamp.maven.plugin.oci.mojos.autonomous

import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.database.model.GenerateAutonomousDatabaseWalletDetails
import com.oracle.bmc.database.requests.GenerateAutonomousDatabaseWalletRequest
import com.oracle.bmc.database.requests.ListAutonomousDatabasesRequest
import com.oracle.bmc.database.responses.GenerateAutonomousDatabaseWalletResponse
import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDatabaseIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDatabaseNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WalletPasswordAwareTrait

import static org.kordamp.maven.StringUtils.isBlank
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'generate-autonomous-database-wallet')
class GenerateAutonomousDatabaseWalletMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    OptionalDatabaseIdAwareTrait,
    OptionalDatabaseNameAwareTrait,
    WalletPasswordAwareTrait,
    WaitForCompletionAwareTrait {
    private File output

    File getOutput() {
        if (!this.@output) {
            this.@output = new File('target/oci/autonomous/wallet.zip')
        }
        this.@output
    }

    @Override
    void executeGoal() {
        validateWalletPassword()
        validateDatabaseId()

        if (isBlank(getDatabaseId()) && isBlank(getDatabaseName())) {
            throw new IllegalStateException("Missing value for either 'databaseId' or 'databaseName' in $path")
        }

        DatabaseClient client = createDatabaseClient()

        if (isNotBlank(getDatabaseId())) {
            generateAutonomousDatabaseWallet(client,
                getDatabaseId(),
                getWalletPassword(),
                getOutput())
        } else {
            validateCompartmentId()

            client.listAutonomousDatabases(ListAutonomousDatabasesRequest.builder()
                .compartmentId(getCompartmentId())
                .displayName(getDatabaseName())
                .build())
                .items.each { database ->
                setDatabaseId(database.id)
                generateAutonomousDatabaseWallet(client,
                    database.id,
                    getWalletPassword(),
                    getOutput())
            }
        }
    }

    static void generateAutonomousDatabaseWallet(DatabaseClient client,
                                                 String databaseId,
                                                 String password,
                                                 File output) {
        GenerateAutonomousDatabaseWalletResponse response = client.generateAutonomousDatabaseWallet(GenerateAutonomousDatabaseWalletRequest.builder()
            .autonomousDatabaseId(databaseId)
            .generateAutonomousDatabaseWalletDetails(GenerateAutonomousDatabaseWalletDetails.builder()
                .password(password)
                .build())
            .build())

        FileUtils.copyInputStreamToFile(response.inputStream, output)
        println("Wallet for database with id ${databaseId} has been saved to ${output.absolutePath}")
    }
}

