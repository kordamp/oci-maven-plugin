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
package org.kordamp.maven.plugin.oci.mojos.instance

import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.model.InternetGateway
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.model.Subnet
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.GetSubnetRequest
import com.oracle.bmc.core.requests.GetVcnRequest
import com.oracle.bmc.core.requests.ListSubnetsRequest
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.internal.hash.HashUtil
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.ImageAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.InstanceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalAvailabilityDomainAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDnsLabelAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalSubnetIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalUserDataFileAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.PublicKeyFileAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.ShapeAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

import static org.kordamp.maven.StringUtils.isNotBlank
import static org.kordamp.maven.plugin.oci.mojos.create.CreateInstanceMojo.maybeCreateInstance
import static org.kordamp.maven.plugin.oci.mojos.create.CreateInternetGatewayMojo.maybeCreateInternetGateway
import static org.kordamp.maven.plugin.oci.mojos.create.CreateSubnetMojo.maybeCreateSubnet
import static org.kordamp.maven.plugin.oci.mojos.create.CreateVcnMojo.maybeCreateVcn
import static org.kordamp.maven.plugin.oci.mojos.get.GetInstancePublicIpMojo.getInstancePublicIp

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'setup-instance')
class SetupInstanceMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    InstanceNameAwareTrait,
    ImageAwareTrait,
    ShapeAwareTrait,
    PublicKeyFileAwareTrait,
    OptionalUserDataFileAwareTrait,
    OptionalDnsLabelAwareTrait,
    OptionalAvailabilityDomainAwareTrait,
    OptionalSubnetIdAwareTrait,
    VerboseAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId',
            'instanceName',
            'image',
            'shape',
            'publicKeyFile',
            'userDataFile',
            'dnsLabel'
        ]
    }

    private String createdInstanceId
    private File output

    String getCreatedInstanceId() {
        this.@createdInstanceId
    }

    File getOutput() {
        if (!this.@output) {
            this.@output = new File("target/oci/instance/${getInstanceName()}.properties")
        }
        this.@output
    }

    @Override
    protected void executeGoal() {
        validateCompartmentId()
        validateImage()
        validateShape()
        validatePublicKeyFile()

        getOutput().parentFile.mkdirs()

        Properties props = new Properties()
        props.put('compartment.id', getCompartmentId())

        ComputeClient computeClient = createComputeClient()
        IdentityClient identityClient = createIdentityClient()
        VirtualNetworkClient vcnClient = createVirtualNetworkClient()
        BlockstorageClient blockstorageClient = createBlockstorageClient()

        Image _image = validateImage(computeClient, getCompartmentId())
        Shape _shape = validateShape(computeClient, getCompartmentId())

        File publicKeyFile = getPublicKeyFile()
        File userDataFile = getUserDataFile()
        String internetGatewayDisplayName = getInstanceName() + '-internet-gateway'
        String kmsKeyId = ''

        AvailabilityDomain availabilityDomain = null
        Subnet subnet = null
        Vcn vcn = null

        if (isNotBlank(getAvailabilityDomain())) {
            for (AvailabilityDomain ad : identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
                .compartmentId(getCompartmentId())
                .build()).items) {
                if (ad.id == getAvailabilityDomain()) {
                    availabilityDomain = ad
                    break
                }
            }
        } else if (isNotBlank(getSubnetId())) {
            try {
                subnet = vcnClient.getSubnet(GetSubnetRequest.builder()
                    .subnetId(getSubnetId())
                    .build())
                    .subnet

                vcn = vcnClient.getVcn(GetVcnRequest.builder()
                    .vcnId(subnet.vcnId)
                    .build())
                    .vcn
            } catch (Exception ignored) {
                // ignored
            }
        }

        if (!subnet) {
            String networkCidrBlock = '10.0.0.0/16'
            String vcnDisplayName = getInstanceName() + '-vcn'
            String dnsLabel = normalizeDnsLabel(isNotBlank(getDnsLabel()) ? getDnsLabel() : getInstanceName())
            vcn = maybeCreateVcn(this,
                vcnClient,
                getCompartmentId(),
                vcnDisplayName,
                dnsLabel,
                networkCidrBlock,
                true,
                isVerbose())
        }

        props.put('vcn.id', vcn.id)
        props.put('vcn.name', vcn.displayName)
        props.put('vcn.security-list.id', vcn.defaultSecurityListId)
        props.put('vcn.route-table.id', vcn.defaultRouteTableId)

        InternetGateway internetGateway = maybeCreateInternetGateway(this,
            vcnClient,
            getCompartmentId(),
            vcn.id,
            internetGatewayDisplayName,
            true,
            isVerbose())
        props.put('internet-gateway.id', internetGateway.id)

        if (!subnet) {
            if (availabilityDomain) {
                for (Subnet s : vcnClient.listSubnets(ListSubnetsRequest.builder()
                    .compartmentId(getCompartmentId())
                    .vcnId(vcn.id)
                    .build()).items) {
                    if (s.availabilityDomain == availabilityDomain.name) {
                        subnet = s
                        props.put('vcn.subnets', '1')
                        props.put('subnet.0.id'.toString(), s.id)
                        props.put('subnet.0.name'.toString(), s.displayName)
                        break
                    }
                }
            } else {
                int subnetIndex = 0
                // create a Subnet per AvailabilityDomain
                List<AvailabilityDomain> availabilityDomains = identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
                    .compartmentId(getCompartmentId())
                    .build()).items
                props.put('vcn.subnets', availabilityDomains.size().toString())
                for (AvailabilityDomain domain : availabilityDomains) {
                    String subnetDnsLabel = 'sub' + HashUtil.sha1(vcn.id.bytes).asHexString()[0..8] + (subnetIndex.toString().padLeft(3, '0'))

                    Subnet s = maybeCreateSubnet(this,
                        vcnClient,
                        getCompartmentId(),
                        vcn.id,
                        subnetDnsLabel,
                        domain.name,
                        'Subnet ' + domain.name,
                        "10.0.${subnetIndex}.0/24".toString(),
                        true,
                        isVerbose())
                    props.put("subnet.${subnetIndex}.id".toString(), s.id)
                    props.put("subnet.${subnetIndex}.name".toString(), s.displayName)

                    // save the first one
                    if (subnet == null) subnet = s
                    if (availabilityDomain == null) availabilityDomain = domain
                    subnetIndex++
                }
            }
        }

        Instance instance = maybeCreateInstance(this,
            computeClient,
            vcnClient,
            blockstorageClient,
            identityClient,
            getCompartmentId(),
            getInstanceName(),
            _image,
            _shape,
            availabilityDomain,
            subnet,
            publicKeyFile,
            userDataFile,
            kmsKeyId,
            true)
        createdInstanceId = instance.id
        props.put('instance.id', instance.id)
        props.put('instance.name', instance.displayName)

        Set<String> publicIps = getInstancePublicIp(this,
            computeClient,
            vcnClient,
            getCompartmentId(),
            instance.id)

        props.put('instance.public-ips', publicIps.size().toString())
        int publicIpIndex = 0
        for (String publicIp : publicIps) {
            props.put("instance.public-ip.${publicIpIndex++}".toString(), publicIp)
        }

        props.store(new FileWriter(getOutput()), '')
        println("Result stored at ${console.yellow(getOutput().absolutePath)}")
    }

    private String normalizeDnsLabel(String dnsLabel) {
        String label = dnsLabel?.replace('.', '')?.replace('-', '')
        if (label?.length() > 15) label = HashUtil.sha1(dnsLabel.bytes).asHexString()[0..14]
        label
    }
}
