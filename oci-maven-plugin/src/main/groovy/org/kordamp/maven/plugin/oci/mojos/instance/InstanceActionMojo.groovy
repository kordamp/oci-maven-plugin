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
package org.kordamp.maven.plugin.oci.mojos.instance

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.InstanceActionRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalInstanceIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalInstanceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'instance-action')
class InstanceActionMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    OptionalInstanceIdAwareTrait,
    OptionalInstanceNameAwareTrait,
    WaitForCompletionAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId',
            'instanceId',
            'instanceName'
        ]
    }

    private static enum InstanceAction {
        START(Instance.LifecycleState.Running),
        STOP(Instance.LifecycleState.Stopped),
        SOFTRESET(Instance.LifecycleState.Running),
        SOFTSTOP(Instance.LifecycleState.Stopped),
        RESET(Instance.LifecycleState.Running)

        private Instance.LifecycleState state

        InstanceAction(Instance.LifecycleState state) {
            this.state = state
        }

        Instance.LifecycleState state() {
            this.state
        }
    }

    @Parameter(property = 'oci.instance.action')
    InstanceAction action

    InstanceAction getAction() {
        InstanceAction.valueOf(stringProperty(this, 'OCI_INSTANCE_ACTION', 'oci.instance.action', (this.@action ?: InstanceAction.STOP).name()).toUpperCase())
    }

    @Override
    protected void executeGoal() {
        validateInstanceId()

        if (isBlank(getInstanceId()) && isBlank(getInstanceName())) {
            throw new IllegalStateException("Missing value for either 'instanceId' or 'instanceName' in $path")
        }
        if (!getAction()) {
            throw new IllegalStateException("Missing value for 'action' in $path")
        }

        ComputeClient client = createComputeClient()

        if (isNotBlank(getInstanceId())) {
            Instance instance = instanceAction(client, getInstanceId(), getAction())
            if (instance) setInstanceName(instance.displayName)
        } else {
            validateCompartmentId()

            client.listInstances(ListInstancesRequest.builder()
                .compartmentId(compartmentId)
                .displayName(getInstanceName())
                .build())
                .items.each { instance ->
                setInstanceId(instance.id)
                instanceAction(client, instance.id, getAction())
            }
        }
    }

    private Instance instanceAction(ComputeClient client, String instanceId, InstanceAction action) {
        println("Sending ${getAction().name()} to Instance with id ${console.yellow(instanceId)}")
        Instance instance = client.instanceAction(InstanceActionRequest.builder()
            .instanceId(instanceId)
            .action(action.name())
            .build())
            .instance

        if (isWaitForCompletion()) {
            println("Waiting for Instance to be ${state(action.state().name())}")
            client.waiters
                .forInstance(GetInstanceRequest.builder()
                    .instanceId(instance.id).build(),
                    action.state())
                .execute()
        }

        instance
    }
}

