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

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.InstanceConsoleConnection
import com.oracle.bmc.core.requests.GetInstanceConsoleConnectionRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.InstanceConsoleConnectionIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.InstanceConsoleConnectionPrinter.printInstanceConsoleConnection

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'get-instance-console-connection')
class GetInstanceConsoleConnectionMojo extends AbstractOCIMojo implements InstanceConsoleConnectionIdAwareTrait {
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

        InstanceConsoleConnection connection = client.getInstanceConsoleConnection(GetInstanceConsoleConnectionRequest.builder()
            .instanceConsoleConnectionId(getInstanceConsoleConnectionId())
            .build())
            .instanceConsoleConnection

        if (connection) {
            printInstanceConsoleConnection(this, connection, 0)
        } else {
            println("InstanceConsoleConnection with id ${getInstanceConsoleConnectionId()} was not found")
        }
    }
}
