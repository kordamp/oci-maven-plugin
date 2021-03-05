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
package org.kordamp.maven.plugin.oci.mojos.traits

import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import com.oracle.bmc.identity.responses.ListAvailabilityDomainsResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.plugin.oci.mojos.interfaces.ExecutionIdAware
import org.kordamp.maven.plugin.oci.mojos.interfaces.PathAware

import static org.kordamp.maven.PropertyUtils.stringProperty

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
trait OptionalAvailabilityDomainAwareTrait implements PathAware, ExecutionIdAware {
    @Parameter(property = 'oci.availability.domain', name = 'availabilityDomain')
    String availabilityDomain

    String getAvailabilityDomain() {
        stringProperty(this, 'OCI_AVAILABILITY_DOMAIN', 'oci.availability.domain', this.@availabilityDomain)
    }

    AvailabilityDomain validateAvailabilityDomain(IdentityClient identityClient, String compartmentId) {
        ListAvailabilityDomainsResponse response = identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
            .compartmentId(compartmentId)
            .build())
        response.items.find { AvailabilityDomain ad -> ad.name == getAvailabilityDomain() }
    }
}
