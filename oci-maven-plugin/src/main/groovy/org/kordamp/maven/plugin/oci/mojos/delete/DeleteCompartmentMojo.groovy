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

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.requests.DeleteCompartmentRequest
import com.oracle.bmc.identity.requests.GetCompartmentRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'delete-compartment')
class DeleteCompartmentMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    WaitForCompletionAwareTrait {
    @Override
    protected void executeGoal() {
        validateCompartmentId()

        IdentityClient client = createIdentityClient()

        // TODO: check if compartment exists
        // TODO: check is compartment is in a 'deletable' state
        // TODO: check if compartment is empty

        String resolvedCompartmentId = getCompartmentId()
        Compartment compartment = client.getCompartment(GetCompartmentRequest.builder()
            .compartmentId(resolvedCompartmentId)
            .build())
            .compartment

        println("Deleting Compartment ${compartment.name} with id ${resolvedCompartmentId}")

        client.deleteCompartment(DeleteCompartmentRequest.builder()
            .compartmentId(resolvedCompartmentId)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for Compartment to be ${state('Deleted')}")
            client.waiters
                .forCompartment(GetCompartmentRequest.builder()
                    .compartmentId(resolvedCompartmentId)
                    .build(),
                    Compartment.LifecycleState.Deleted)
                .execute()
        }
    }
}
