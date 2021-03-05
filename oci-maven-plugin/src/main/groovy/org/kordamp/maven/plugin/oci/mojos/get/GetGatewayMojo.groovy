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

import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.apigateway.model.GatewaySummary
import com.oracle.bmc.apigateway.requests.ListGatewaysRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.GatewayIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.GatewayPrinter.printGateway

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'get-gateway')
class GetGatewayMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    GatewayIdAwareTrait {
    @Override
    void executeGoal() {
        validateCompartmentId()
        validateGatewayId()

        GatewayClient client = createGatewayClient()

        List<GatewaySummary> gateways = client.listGateways(ListGatewaysRequest.builder()
            .compartmentId(getCompartmentId())
            .build())
            .gatewayCollection
            .items

        GatewaySummary gatewaySummary = gateways.find { GatewaySummary ig -> ig.id == getGatewayId() }

        if (gatewaySummary) {
            println(gatewaySummary.displayName + ':')
            printGateway(this, gatewaySummary, 0)
        } else {
            println("Gateway with id ${getGatewayId()} was not found")
        }
    }
}
