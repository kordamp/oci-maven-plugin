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

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.model.CreateCompartmentDetails
import com.oracle.bmc.identity.requests.CreateCompartmentRequest
import com.oracle.bmc.identity.requests.GetCompartmentRequest
import com.oracle.bmc.identity.requests.ListCompartmentsRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentDescriptionAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.CompartmentPrinter.printCompartment

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'create-compartment')
class CreateCompartmentMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    CompartmentNameAwareTrait,
    CompartmentDescriptionAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    String createdCompartmentId

    @Override
    void executeGoal() {
        validateCompartmentId()
        validateCompartmentName()
        validateCompartmentDescription()

        IdentityClient client = createIdentityClient()

        Compartment compartment = maybeCreateCompartment(this,
            client,
            getCompartmentId(),
            getCompartmentName(),
            getCompartmentDescription(),
            isWaitForCompletion(),
            isVerbose())
        setCreatedCompartmentId(compartment.id)
    }

    static Compartment maybeCreateCompartment(OCIMojo owner,
                                              IdentityClient client,
                                              String parentCompartmentId,
                                              String compartmentName,
                                              String compartmentDescription,
                                              boolean waitForCompletion,
                                              boolean verbose) {
        // 1. Check if it exists
        List<Compartment> compartments = client.listCompartments(ListCompartmentsRequest.builder()
            .compartmentId(parentCompartmentId)
            .build()).items
        Compartment compartment = compartments.find { Compartment c -> c.name == compartmentName }

        if (compartment) {
            println("Compartment '${compartmentName}' already exists. id = ${owner.console.yellow(compartment.id)}")
            if (verbose) printCompartment(owner, compartment, 0)
            return compartment
        }
        // 2. Create
        println('Provisioning Compartment. This may take a while.')
        compartment = client.createCompartment(CreateCompartmentRequest.builder()
            .createCompartmentDetails(CreateCompartmentDetails.builder()
                .compartmentId(parentCompartmentId)
                .name(compartmentName)
                .description(compartmentDescription)
                .build())
            .build())
            .compartment

        if (waitForCompletion) {
            println("Waiting for Compartment to be ${owner.state('Active')}")
            client.waiters
                .forCompartment(GetCompartmentRequest.builder()
                    .compartmentId(compartment.id)
                    .build(),
                    Compartment.LifecycleState.Active)
                .execute()
        }

        println("Compartment '${compartmentName}' has been provisioned. id = ${owner.console.yellow(compartment.id)}")
        if (verbose) printCompartment(owner, compartment, 0)
        compartment
    }
}
