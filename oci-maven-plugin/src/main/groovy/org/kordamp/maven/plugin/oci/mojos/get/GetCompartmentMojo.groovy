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
package org.kordamp.maven.plugin.oci.mojos.get

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.requests.GetCompartmentRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.CompartmentPrinter.printCompartment

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'get-compartment')
class GetCompartmentMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait {
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
        Compartment compartment = client.getCompartment(GetCompartmentRequest.builder()
            .compartmentId(compartmentId)
            .build())
            .compartment

        println(compartment.name + ':')
        printCompartment(this, compartment, 0)
    }
}
