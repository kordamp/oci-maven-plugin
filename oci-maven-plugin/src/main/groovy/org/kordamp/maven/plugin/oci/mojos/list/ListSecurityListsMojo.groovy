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
package org.kordamp.maven.plugin.oci.mojos.list

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.SecurityList
import com.oracle.bmc.core.requests.ListSecurityListsRequest
import com.oracle.bmc.core.responses.ListSecurityListsResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.printers.SecurityListPrinter
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VcnIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'list-security-lists')
class ListSecurityListsMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    VcnIdAwareTrait,
    VerboseAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId',
            'vcnId'
        ]
    }

    @Override
    protected void executeGoal() {
        validateCompartmentId()
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()
        ListSecurityListsResponse response = client.listSecurityLists(ListSecurityListsRequest.builder()
            .compartmentId(getCompartmentId())
            .vcnId(getVcnId())
            .build())

        println('Total SecurityLists: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (SecurityList securityList : response.items) {
            println(securityList.displayName + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                SecurityListPrinter.printSecurityList(this, securityList, 0)
            }
        }
    }
}
