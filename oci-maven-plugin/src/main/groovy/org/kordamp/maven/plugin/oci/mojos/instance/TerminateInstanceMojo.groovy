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
package org.kordamp.maven.plugin.oci.mojos.instance

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.requests.TerminateInstanceRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalInstanceIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalInstanceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.RegexAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.StringUtils.isBlank
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'terminate-instance')
class TerminateInstanceMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    OptionalInstanceIdAwareTrait,
    OptionalInstanceNameAwareTrait,
    RegexAwareTrait,
    WaitForCompletionAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId',
            'instanceId',
            'instanceName'
        ]
    }

    @Override
    protected void executeGoal() {
        validateInstanceId()

        if (isBlank(getInstanceId()) && isBlank(getInstanceName())) {
            throw new IllegalStateException("Missing value for either 'instanceId' or 'instanceName' in $path")
        }

        ComputeClient client = createComputeClient()

        // TODO: check if instance exists
        // TODO: check is instance is in a 'deletable' state

        if (isNotBlank(getInstanceId())) {
            Instance instance = client.getInstance(GetInstanceRequest.builder()
                .instanceId(getInstanceId())
                .build())
                .instance

            if (instance) {
                setInstanceName(instance.displayName)
                terminateInstance(client, instance)
            }
        } else {
            validateCompartmentId()

            if (isRegex()) {
                client.listInstances(ListInstancesRequest.builder()
                    .compartmentId(getCompartmentId())
                    .displayName(getInstanceName())
                    .build())
                    .items.each { instance ->
                    setInstanceId(instance.id)
                    terminateInstance(client, instance)
                }
            } else {
                final String instanceNameRegex = getInstanceName()
                client.listInstances(ListInstancesRequest.builder()
                    .compartmentId(getCompartmentId())
                    .build())
                    .items.each { instance ->
                    if (instance.displayName.matches(instanceNameRegex)) {
                        setInstanceId(instance.id)
                        terminateInstance(client, instance)
                    }
                }
            }
        }
    }

    private void terminateInstance(ComputeClient client, Instance instance) {
        println("Terminating Instance '${instance.displayName}' with id ${instance.id}")
        client.terminateInstance(TerminateInstanceRequest.builder()
            .instanceId(instance.id)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for Instance to be ${state('Terminated')}")
            client.waiters
                .forInstance(GetInstanceRequest.builder()
                    .instanceId(instance.id).build(),
                    Instance.LifecycleState.Terminated)
                .execute()
        }
    }
}
