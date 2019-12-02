/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.maven.plugin.oci.mojos.create

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.CreateInstanceConsoleConnectionDetails
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.CreateInstanceConsoleConnectionRequest
import com.oracle.bmc.core.requests.GetInstanceConsoleConnectionRequest
import com.oracle.bmc.core.requests.ListInstanceConsoleConnectionsRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.InstanceIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.PublicKeyFileAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.InstanceConsoleConnectionPrinter.printInstanceConsoleConnection

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'create-instance-console-connection')
class CreateInstanceConsoleConnectionMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    InstanceIdAwareTrait,
    PublicKeyFileAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    String createdConnectionId

    @Override
    void executeGoal() {
        validateCompartmentId()
        validateInstanceId()
        validatePublicKeyFile()

        ComputeClient client = createComputeClient()

        InstanceConsoleConnection connection = maybeCreateInstanceConsoleConnection(this,
            client,
            getCompartmentId(),
            getInstanceId(),
            getPublicKeyFile(),
            isWaitForCompletion(),
            isVerbose())
        setCreatedConnectionId(connection.id)
    }

    static InstanceConsoleConnection maybeCreateInstanceConsoleConnection(OCIMojo owner,
                                                                          ComputeClient client,
                                                                          String compartmentId,
                                                                          String instanceId,
                                                                          File publicKeyFile,
                                                                          boolean waitForCompletion,
                                                                          boolean verbose) {
        // 1. Check if it exists
        List<InstanceConsoleConnection> connections = client.listInstanceConsoleConnections(ListInstanceConsoleConnectionsRequest.builder()
            .compartmentId(compartmentId)
            .instanceId(instanceId)
            .build())
            .items

        if (!connections.empty) {
            InstanceConsoleConnection connection = connections[0]
            println("InstanceConsoleConnection for instance '${instanceId}' already exists. id = ${owner.console.yellow(connection.id)}")
            if (verbose) printInstanceConsoleConnection(owner, connection, 0)
            return connection
        }

        // 2. Create
        println('Provisioning InstanceConsoleConnection. This may take a while.')
        CreateInstanceConsoleConnectionDetails details = CreateInstanceConsoleConnectionDetails.builder()
            .publicKey(publicKeyFile.text)
            .instanceId(instanceId)
            .build()

        InstanceConsoleConnection connection = client.createInstanceConsoleConnection(CreateInstanceConsoleConnectionRequest.builder()
            .createInstanceConsoleConnectionDetails(details)
            .build())
            .instanceConsoleConnection

        if (waitForCompletion) {
            println("Waiting for InstanceConsoleConnection to be ${owner.state('Active')}")
            client.waiters.forInstanceConsoleConnection(GetInstanceConsoleConnectionRequest.builder()
                .instanceConsoleConnectionId(connection.id)
                .build(),
                InstanceConsoleConnection.LifecycleState.Active)
                .execute()
        }

        println("InstanceConsoleConnection has been provisioned. id = ${owner.console.yellow(connection.id)}")
        if (verbose) printInstanceConsoleConnection(owner, connection, 0)
        connection
    }
}
