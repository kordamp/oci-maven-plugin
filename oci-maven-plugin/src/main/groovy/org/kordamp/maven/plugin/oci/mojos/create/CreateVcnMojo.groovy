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
import com.oracle.bmc.core.model.CreateVcnDetails
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.CreateVcnRequest
import com.oracle.bmc.core.requests.GetVcnRequest
import com.oracle.bmc.core.requests.ListVcnsRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.DnsLabelAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VcnNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.VcnPrinter.printVcn

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'create-vcn')
class CreateVcnMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    VcnNameAwareTrait,
    DnsLabelAwareTrait,
    WaitForCompletionAwareTrait,
    VerboseAwareTrait {
    String createdVcnId

    @Override
    void executeGoal() {
        validateCompartmentId()
        validateDnsLabel(getCompartmentId())
        validateVcnName()

        VirtualNetworkClient client = createVirtualNetworkClient()

        Vcn vcn = maybeCreateVcn(this,
            client,
            getCompartmentId(),
            getVcnName(),
            getDnsLabel(),
            '10.0.0.0/16',
            isWaitForCompletion(),
            isVerbose())
        setCreatedVcnId(vcn.id)
    }

    static Vcn maybeCreateVcn(OCIMojo owner,
                              VirtualNetworkClient client,
                              String compartmentId,
                              String vcnName,
                              String dnsLabel,
                              String cidrBlock,
                              boolean waitForCompletion,
                              boolean verbose) {
        // 1. Check if it exists
        List<Vcn> vcns = client.listVcns(ListVcnsRequest.builder()
            .compartmentId(compartmentId)
            .displayName(vcnName)
            .build())
            .items

        if (!vcns.empty) {
            Vcn vcn = vcns[0]
            println("Vcn '${vcnName}' already exists. id = ${owner.console.yellow(vcn.id)}")
            if (verbose) printVcn(owner, vcn, 0)
            return vcn
        }

        // 2. Create
        println('Provisioning Vcn. This may take a while.')
        Vcn vcn = client.createVcn(CreateVcnRequest.builder()
            .createVcnDetails(CreateVcnDetails.builder()
                .cidrBlock(cidrBlock)
                .compartmentId(compartmentId)
                .displayName(vcnName)
                .dnsLabel(dnsLabel)
                .build())
            .build())
            .vcn

        if (waitForCompletion) {
            println("Waiting for Vcn to be ${owner.state('Available')}")
            client.waiters
                .forVcn(GetVcnRequest.builder()
                    .vcnId(vcn.id)
                    .build(),
                    Vcn.LifecycleState.Available)
                .execute()
        }

        println("Vcn '${vcnName}' has been provisioned. id = ${owner.console.yellow(vcn.id)}")
        if (verbose) printVcn(owner, vcn, 0)
        vcn
    }
}
