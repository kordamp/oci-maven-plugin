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
package org.kordamp.maven.plugin.oci.mojos.create

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.CreateSubnetDetails
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.requests.CreateSubnetRequest
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.ListSubnetsRequest
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.AvailabilityDomainAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDnsLabelAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.SubnetNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VcnIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.SubnetPrinter.printSubnet

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'create-subnet')
class CreateSubnetMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    AvailabilityDomainAwareTrait,
    VcnIdAwareTrait,
    SubnetNameAwareTrait,
    OptionalDnsLabelAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    String createdSubnetId

    @Override
    void executeGoal() {
        validateCompartmentId()
        validateVcnId()
        validateAvailabilityDomain()
        validateSubnetName()
        validateDnsLabel(getVcnId())

        VirtualNetworkClient client = createVirtualNetworkClient()
        IdentityClient identityClient = createIdentityClient()

        AvailabilityDomain _availabilityDomain = validateAvailabilityDomain(identityClient, getCompartmentId())

        Subnet subnet = maybeCreateSubnet(this,
            client,
            getCompartmentId(),
            getVcnId(),
            getDnsLabel(),
            _availabilityDomain.name,
            getSubnetName(),
            '10.0.0.0/24',
            isWaitForCompletion(),
            isVerbose())
        setCreatedSubnetId(subnet.id)
    }

    static Subnet maybeCreateSubnet(OCIMojo owner,
                                    VirtualNetworkClient client,
                                    String compartmentId,
                                    String vcnId,
                                    String dnsLabel,
                                    String availabilityDomain,
                                    String subnetName,
                                    String cidrBlock,
                                    boolean waitForCompletion,
                                    boolean verbose) {
        // 1. Check if it exists
        List<Subnet> subnets = client.listSubnets(ListSubnetsRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(subnetName)
            .build())
            .items

        if (!subnets.empty) {
            Subnet subnet = subnets[0]
            println("Subnet '${subnetName}' already exists. id = ${owner.console.yellow(subnet.id)}")
            if (verbose) printSubnet(owner, subnet, 0)
            return subnets[0]
        }

        Subnet subnet = client.createSubnet(CreateSubnetRequest.builder()
            .createSubnetDetails(CreateSubnetDetails.builder()
                .compartmentId(compartmentId)
                .vcnId(vcnId)
                .availabilityDomain(availabilityDomain)
                .displayName(subnetName)
                .dnsLabel(dnsLabel)
                .cidrBlock(cidrBlock)
                .build())
            .build())
            .subnet

        if (waitForCompletion) {
            println("Waiting for Subnet to be ${owner.state('Available')}")
            client.waiters.forSubnet(GetSubnetRequest.builder()
                .subnetId(subnet.id)
                .build(),
                Subnet.LifecycleState.Available)
                .execute()
        }

        println("Subnet '${subnetName}' has been provisioned. id = ${owner.console.yellow(subnet.id)}")
        if (verbose) printSubnet(owner, subnet, 0)
        subnet
    }
}
