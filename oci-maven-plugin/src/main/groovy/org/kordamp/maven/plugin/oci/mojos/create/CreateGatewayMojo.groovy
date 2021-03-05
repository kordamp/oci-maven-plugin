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
package org.kordamp.maven.plugin.oci.mojos.create

import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.apigateway.model.CreateGatewayDetails
import com.oracle.bmc.apigateway.model.Gateway
import com.oracle.bmc.apigateway.model.Gateway.EndpointType
import com.oracle.bmc.apigateway.model.GatewaySummary
import com.oracle.bmc.apigateway.requests.CreateGatewayRequest
import com.oracle.bmc.apigateway.requests.GetGatewayRequest
import com.oracle.bmc.apigateway.requests.ListGatewaysRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.GatewayNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.SubnetIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.plugin.oci.mojos.printers.GatewayPrinter.printGateway

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'create-gateway')
class CreateGatewayMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    SubnetIdAwareTrait,
    GatewayNameAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    String createdGatewayId

    @Parameter(property = 'oci.gateway.endpoint.type')
    EndpointType action

    EndpointType getEndpointType() {
        EndpointType.valueOf(stringProperty(this, 'OCI_GATEWAY_ENDPOINT_TYPE', 'oci.gateway.endpoint.type', (this.@action ?: EndpointType.Public).name()).toUpperCase())
    }

    @Override
    void executeGoal() {
        validateCompartmentId()
        validateSubnetId()
        validateGatewayName()

        GatewayClient client = createGatewayClient()

        GatewaySummary gateway = maybeCreateGateway(this,
            client,
            getCompartmentId(),
            getSubnetId(),
            getGatewayName(),
            getEndpointType(),
            isWaitForCompletion(),
            isVerbose())
        createdGatewayId = gateway.id
    }

    static GatewaySummary maybeCreateGateway(OCIMojo owner,
                                             GatewayClient client,
                                             String compartmentId,
                                             String subnetId,
                                             String gatewayName,
                                             EndpointType endpointType,
                                             boolean waitForCompletion,
                                             boolean verbose) {
        // 1. Check if it exists
        List<GatewaySummary> gateways = client.listGateways(ListGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .build())
            .gatewayCollection
            .items

        GatewaySummary gatewaySummary = gateways.find { GatewaySummary ig -> ig.displayName == gatewayName }

        if (gatewaySummary) {
            println("Gateway '${gatewayName}' already exists. id = ${gatewaySummary.id}")
            if (verbose) printGateway(owner, gatewaySummary, 0)
            return gatewaySummary
        }

        if (!gateways.empty) {
            gatewaySummary = gateways[0]
            println("Gateway '${gatewaySummary.displayName}' exists. id = ${gatewaySummary.id}")
            if (verbose) printGateway(owner, gatewaySummary, 0)
            return gatewaySummary
        }

        Gateway gateway = client.createGateway(CreateGatewayRequest.builder()
            .createGatewayDetails(CreateGatewayDetails.builder()
                .compartmentId(compartmentId)
                .subnetId(subnetId)
                .displayName(gatewayName)
                .endpointType(endpointType)
                .build())
            .build())
            .gateway

        if (waitForCompletion) {
            println("Waiting for Gateway to be ${owner.state('Available')}")
            client.waiters.forGateway(GetGatewayRequest.builder()
                .gatewayId(gateway.id)
                .build(),
                Gateway.LifecycleState.Active)
                .execute()
        }

        gateways = client.listGateways(ListGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .displayName(gatewayName)
            .build())
            .gatewayCollection
            .items

        println("Gateway '${gatewayName}' has been provisioned. id = ${owner.console.yellow(gateway.id)}")
        if (verbose) printGateway(owner, gateways[0], 0)
        gateways[0]
    }
}
