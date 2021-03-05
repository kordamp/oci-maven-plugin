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
package org.kordamp.maven.plugin.oci.mojos.delete

import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.apigateway.model.Gateway
import com.oracle.bmc.apigateway.model.GatewaySummary
import com.oracle.bmc.apigateway.requests.DeleteGatewayRequest
import com.oracle.bmc.apigateway.requests.GetGatewayRequest
import com.oracle.bmc.apigateway.requests.ListGatewaysRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalGatewayIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalGatewayNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.StringUtils.isBlank
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
@Mojo(name = 'delete-gateway')
class DeleteGatewayMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    OptionalGatewayIdAwareTrait,
    OptionalGatewayNameAwareTrait,
    WaitForCompletionAwareTrait {
    @Override
    void executeGoal() {
        validateGatewayId()

        if (isBlank(getGatewayId()) && isBlank(getGatewayName())) {
            throw new IllegalStateException("Missing value for either 'gatewayId' or 'gatewayName' in $path")
        }

        GatewayClient client = createGatewayClient()

        // TODO: check if gateway exists
        // TODO: check is gateway is in a 'deletable' state

        if (isNotBlank(getGatewayId())) {
            Gateway gateway = client.getGateway(GetGatewayRequest.builder()
                .gatewayId(getGatewayId())
                .build())
                .gateway

            if (gateway) {
                setGatewayName(gateway.displayName)
                deleteGateway(client, gateway)
            }
        } else {
            validateCompartmentId()

            client.listGateways(ListGatewaysRequest.builder()
                .compartmentId(getCompartmentId())
                .displayName(getGatewayName())
                .build())
                .gatewayCollection
                .items.each { gateway ->
                setGatewayId(gateway.id)
                deleteGateway(client, gateway)
            }
        }
    }

    private void deleteGateway(GatewayClient client, Gateway gateway) {
        println("Deleting Gateway '${gateway.displayName}' with id ${gateway.id}")

        client.deleteGateway(DeleteGatewayRequest.builder()
            .gatewayId(gateway.id)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for Gateway to be ${state('Terminated')}")
            client.waiters
                .forGateway(GetGatewayRequest.builder()
                    .gatewayId(gateway.id).build(),
                    Gateway.LifecycleState.Deleted)
                .execute()
        }
    }

    private void deleteGateway(GatewayClient client, GatewaySummary gateway) {
        println("Deleting Gateway '${gateway.displayName}' with id ${gateway.id}")

        client.deleteGateway(DeleteGatewayRequest.builder()
            .gatewayId(gateway.id)
            .build())

        if (isWaitForCompletion()) {
            println("Waiting for Gateway to be ${state('Terminated')}")
            client.waiters
                .forGateway(GetGatewayRequest.builder()
                    .gatewayId(gateway.id).build(),
                    Gateway.LifecycleState.Deleted)
                .execute()
        }
    }
}
