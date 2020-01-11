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

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.Vcn
import com.oracle.bmc.core.requests.GetVcnRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.VcnIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.VcnPrinter.printVcn

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'get-vcn')
class GetVcnMojo extends AbstractOCIMojo implements VcnIdAwareTrait {
    @Override
    protected void executeGoal() {
        validateVcnId()

        VirtualNetworkClient client = createVirtualNetworkClient()

        Vcn vcn = client.getVcn(GetVcnRequest.builder()
            .vcnId(getVcnId())
            .build())
            .vcn

        if (vcn) {
            println(vcn.displayName + ':')
            printVcn(this, vcn, 0)
        } else {
            println("Vcn with id ${getVcnId()} was not found")
        }
    }
}
