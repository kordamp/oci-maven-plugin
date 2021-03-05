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

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.requests.GetInternetGatewayRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.InternetGatewayIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.InternetGatewayPrinter.printInternetGateway

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'get-internet-gateway')
class GetInternetGatewayMojo extends AbstractOCIMojo implements InternetGatewayIdAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'internetGatewayId'
        ]
    }

    @Override
    protected void executeGoal() {
        validateInternetGatewayId()

        VirtualNetworkClient client = createVirtualNetworkClient()

        InternetGateway internetGateway = client.getInternetGateway(GetInternetGatewayRequest.builder()
            .igId(getInternetGatewayId())
            .build())
            .internetGateway

        if (internetGateway) {
            println(internetGateway.displayName + ':')
            printInternetGateway(this, internetGateway, 0)
        } else {
            println("InternetGateway with id ${getInternetGatewayId()} was not found")
        }
    }
}
