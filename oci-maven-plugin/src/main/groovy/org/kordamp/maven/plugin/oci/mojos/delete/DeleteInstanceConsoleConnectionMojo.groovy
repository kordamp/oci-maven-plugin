/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Andres Almiray.
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
package org.kordamp.maven.plugin.oci.mojos.delete

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.DeleteInstanceConsoleConnectionRequest
import com.oracle.bmc.core.requests.GetInstanceConsoleConnectionRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.InstanceConsoleConnectionIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'delete-instance-console-connection')
class DeleteInstanceConsoleConnectionMojo extends AbstractOCIMojo implements InstanceConsoleConnectionIdAwareTrait,
    WaitForCompletionAwareTrait {

    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'instanceConsoleConnectionId'
        ]
    }

    @Override
    protected void executeGoal() {
        validateInstanceConsoleConnectionId()

        ComputeClient client = createComputeClient()

        // TODO: check if connection exists
        // TODO: check is connection is in a 'deletable' state

        client.deleteInstanceConsoleConnection(DeleteInstanceConsoleConnectionRequest.builder()
            .instanceConsoleConnectionId(instanceConsoleConnectionId)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for InstanceConsoleConnection to be ${state('Deleted')}")
            client.waiters
                .forInstanceConsoleConnection(GetInstanceConsoleConnectionRequest.builder()
                    .instanceConsoleConnectionId(instanceConsoleConnectionId).build(),
                    InstanceConsoleConnection.LifecycleState.Deleted)
                .execute()
        }
    }
}
