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
package org.kordamp.maven.plugin.oci.mojos.list

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.requests.ListCompartmentsRequest
import com.oracle.bmc.identity.responses.ListCompartmentsResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.printers.CompartmentPrinter
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'list-compartments')
class ListCompartmentsMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait, VerboseAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId'
        ]
    }

    @Override
    protected void executeGoal() {
        validateCompartmentId()

        IdentityClient client = createIdentityClient()
        ListCompartmentsResponse response = client.listCompartments(ListCompartmentsRequest.builder()
            .compartmentId(getCompartmentId())
            .build())

        println('Total Compartments: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Compartment compartment : response.items) {
            println(compartment.name + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                CompartmentPrinter.printCompartment(this, compartment, 0)
            }
        }
    }
}
