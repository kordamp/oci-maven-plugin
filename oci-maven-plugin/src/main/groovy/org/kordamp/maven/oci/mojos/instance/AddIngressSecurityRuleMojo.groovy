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
package org.kordamp.maven.oci.mojos.instance

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.IngressSecurityRule
import com.oracle.bmc.core.model.PortRange
import com.oracle.bmc.core.model.SecurityList
import com.oracle.bmc.core.model.TcpOptions
import com.oracle.bmc.core.model.UdpOptions
import com.oracle.bmc.core.model.UpdateSecurityListDetails
import com.oracle.bmc.core.requests.GetSecurityListRequest
import com.oracle.bmc.core.requests.UpdateSecurityListRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.oci.mojos.printers.SecurityListPrinter
import org.kordamp.maven.oci.mojos.traits.SecurityListIdAwareTrait

import static org.kordamp.maven.PropertyUtils.stringProperty

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'add-ingress-security-rule')
class AddIngressSecurityRuleMojo extends AbstractOCIMojo implements SecurityListIdAwareTrait {
    static enum PortType {
        TCP, UDP
    }

    @Parameter(property = 'oci.port.type')
    PortType portType
    @Parameter(property = 'oci.port')
    Integer[] ports

    PortType getPortType() {
        PortType.valueOf(stringProperty('OCI_PORT_TYPE', 'oci.port.type', (this.@portType ?: PortType.TCP).name()).toUpperCase())
    }

    @Override
    protected void executeGoal() {
        validateSecurityListId()

        if (!getPorts()) {
            throw new IllegalStateException("No ports have been defined in $path")
        }
        validatePorts()

        VirtualNetworkClient client = createVirtualNetworkClient()

        SecurityList securityList = addIngressSecurityRules(this,
                client,
                getSecurityListId(),
                getPortType(),
                getPorts())

        SecurityListPrinter.printSecurityList(this, securityList, 0)
    }

    static SecurityList addIngressSecurityRules(OCIMojo owner,
                                                VirtualNetworkClient client,
                                                String securityListId,
                                                PortType portType,
                                                Integer[] ports) {
        SecurityList securityList = client.getSecurityList(GetSecurityListRequest.builder()
                .securityListId(securityListId)
                .build())
                .securityList

        List<IngressSecurityRule> rules = securityList.ingressSecurityRules
        Arrays.sort(ports)
        for (Integer port : ports) {
            IngressSecurityRule.Builder builder = IngressSecurityRule.builder()
                    .source('0.0.0.0/0')
                    .sourceType(IngressSecurityRule.SourceType.CidrBlock)
                    .isStateless(false)
                    .protocol('6')

            switch (portType) {
                case PortType.TCP:
                    builder = builder.tcpOptions(TcpOptions.builder()
                            .sourcePortRange(PortRange.builder()
                            .min(port)
                            .max(port)
                            .build())
                            .build())
                    break
                case PortType.UDP:
                    builder = builder.udpOptions(UdpOptions.builder()
                            .sourcePortRange(PortRange.builder()
                            .min(port)
                            .max(port)
                            .build())
                            .build())
                    break
                default:
                    throw new IllegalStateException("Invalid port type '$portType'")
            }

            rules << builder.build()
        }

        client.updateSecurityList(UpdateSecurityListRequest.builder()
                .securityListId(securityListId)
                .updateSecurityListDetails(UpdateSecurityListDetails.builder()
                .ingressSecurityRules(rules)
                .build())
                .build())
                .securityList
    }

    private void validatePorts() {
        for (String port : ports) {
            try {
                int p = port.toInteger()
                if (p < 1 || p > 65355) {
                    throw new IllegalArgumentException("Port '$port' is out of range.")
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Port '$port' is not a valid integer")
            }
        }
    }
}
