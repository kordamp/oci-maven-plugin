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
package org.kordamp.maven.plugin.oci.mojos.list

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.responses.ListInstancesResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.printers.InstancePrinter
import org.kordamp.maven.plugin.oci.mojos.traits.AvailabilityDomainAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'list-instances')
class ListInstancesMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait, AvailabilityDomainAwareTrait, VerboseAwareTrait {
    @Override
    protected void executeGoal() {
        validateCompartmentId()

        ComputeClient client = createComputeClient()
        ListInstancesResponse response = client.listInstances(ListInstancesRequest.builder()
            .compartmentId(getCompartmentId())
            .availabilityDomain(getAvailabilityDomain())
            .build())

        println('Total Instances: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Instance instance : response.items) {
            println(instance.displayName + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                InstancePrinter.printInstance(this, instance, 0)
            }
        }
    }
}
