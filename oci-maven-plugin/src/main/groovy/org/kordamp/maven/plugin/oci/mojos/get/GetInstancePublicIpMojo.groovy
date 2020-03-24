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

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.GetPublicIpByPrivateIpIdDetails
import com.oracle.bmc.core.model.PrivateIp
import com.oracle.bmc.core.model.VnicAttachment
import com.oracle.bmc.core.requests.GetPublicIpByPrivateIpIdRequest
import com.oracle.bmc.core.requests.GetVnicRequest
import com.oracle.bmc.core.requests.ListPrivateIpsRequest
import com.oracle.bmc.core.requests.ListVnicAttachmentsRequest
import com.oracle.bmc.core.responses.GetPublicIpByPrivateIpIdResponse
import com.oracle.bmc.core.responses.GetVnicResponse
import com.oracle.bmc.model.BmcException
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.InstanceIdAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'get-instance-public-ip')
class GetInstancePublicIpMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    InstanceIdAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId',
            'instanceId'
        ]
    }

    @Override
    protected void executeGoal() {
        validateCompartmentId()
        validateInstanceId()

        ComputeClient computeClient = createComputeClient()
        VirtualNetworkClient vcnClient = createVirtualNetworkClient()

        Set<String> publicIps = getInstancePublicIp(this,
            computeClient,
            vcnClient,
            getCompartmentId(),
            getInstanceId())

        for (String publicIp : publicIps) {
            println(publicIp)
        }
    }

    static Set<String> getInstancePublicIp(OCIMojo owner,
                                           ComputeClient computeClient,
                                           VirtualNetworkClient vcnClient,
                                           String compartmentId,
                                           String instanceId) {
        Iterable<VnicAttachment> vnicAttachmentsIterable = computeClient.paginators
            .listVnicAttachmentsRecordIterator(ListVnicAttachmentsRequest.builder()
                .compartmentId(compartmentId)
                .instanceId(instanceId)
                .build())

        List<String> vnicIds = new ArrayList<>()
        for (VnicAttachment va : vnicAttachmentsIterable) {
            vnicIds << va.vnicId
        }

        final Set<String> publicIps = new HashSet<>()
        for (String vnicId : vnicIds) {
            GetVnicResponse getVnicResponse = vcnClient.getVnic(GetVnicRequest.builder()
                .vnicId(vnicId)
                .build())

            if (getVnicResponse.vnic.publicIp != null) {
                publicIps << getVnicResponse.vnic.publicIp
            }

            Iterable<PrivateIp> privateIpsIterable = vcnClient.paginators
                .listPrivateIpsRecordIterator(ListPrivateIpsRequest.builder()
                    .vnicId(vnicId)
                    .build())

            for (PrivateIp privateIp : privateIpsIterable) {
                try {
                    GetPublicIpByPrivateIpIdResponse getPublicIpResponse =
                        vcnClient.getPublicIpByPrivateIpId(
                            GetPublicIpByPrivateIpIdRequest.builder()
                                .getPublicIpByPrivateIpIdDetails(
                                    GetPublicIpByPrivateIpIdDetails.builder()
                                        .privateIpId(privateIp.id)
                                        .build())
                                .build())
                    publicIps << getPublicIpResponse.publicIp.ipAddress
                } catch (BmcException e) {
                    // A 404 is expected if the private IP address does not have a public IP
                    if (e.statusCode != 404) {
                        println(
                            String.format(
                                "Exception when retrieving public IP for private IP %s (%s)",
                                privateIp.id,
                                privateIp.ipAddress))
                    } else {
                        println(
                            String.format(
                                "No public IP for private IP %s (%s)",
                                privateIp.id,
                                privateIp.ipAddress))
                    }
                }
            }
        }

        publicIps
    }
}
