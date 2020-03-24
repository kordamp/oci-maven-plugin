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
package org.kordamp.maven.plugin.oci.mojos.get

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.RouteTable
import com.oracle.bmc.core.requests.GetRouteTableRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.RouteTableIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.RouteTablePrinter.printRouteTable

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'get-route-table')
class GetRouteTableMojo extends AbstractOCIMojo implements RouteTableIdAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'routeTableId'
        ]
    }

    @Override
    protected void executeGoal() {
        validateRouteTableId()

        VirtualNetworkClient client = createVirtualNetworkClient()

        RouteTable routeTable = client.getRouteTable(GetRouteTableRequest.builder()
            .rtId(getRouteTableId())
            .build())
            .routeTable

        if (routeTable) {
            println(routeTable.displayName + ':')
            printRouteTable(this, routeTable, 0)
        } else {
            println("RouteTable with id ${getRouteTableId()} was not found")
        }
    }
}
