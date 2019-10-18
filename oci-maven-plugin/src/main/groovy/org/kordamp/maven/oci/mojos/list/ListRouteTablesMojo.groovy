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
package org.kordamp.maven.oci.mojos.list

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.RouteTable
import com.oracle.bmc.core.requests.ListRouteTablesRequest
import com.oracle.bmc.core.responses.ListRouteTablesResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.oci.mojos.printers.RouteTablePrinter
import org.kordamp.maven.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.oci.mojos.traits.VcnIdAwareTrait
import org.kordamp.maven.oci.mojos.traits.VerboseAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'list-route-tables')
class ListRouteTablesMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
        VcnIdAwareTrait,
        VerboseAwareTrait {
    @Override
    protected void executeGoal() {
        validateCompartmentId()
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()
        ListRouteTablesResponse response = client.listRouteTables(ListRouteTablesRequest.builder()
                .compartmentId(getCompartmentId())
                .vcnId(getVcnId())
                .build())

        println('Total RouteTables: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (RouteTable routeTable : response.items) {
            println(routeTable.displayName + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                RouteTablePrinter.printRouteTable(this, routeTable, 0)
            }
        }
    }
}
