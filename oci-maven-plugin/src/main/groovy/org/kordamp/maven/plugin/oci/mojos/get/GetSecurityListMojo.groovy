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
package org.kordamp.maven.plugin.oci.mojos.get

import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.SecurityList
import com.oracle.bmc.core.requests.GetSecurityListRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.SecurityListIdAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.SecurityListPrinter.printSecurityList

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'get-security-list')
class GetSecurityListMojo extends AbstractOCIMojo implements SecurityListIdAwareTrait {
    @Override
    protected void executeGoal() {
        validateSecurityListId()

        VirtualNetworkClient client = createVirtualNetworkClient()

        SecurityList securityList = client.getSecurityList(GetSecurityListRequest.builder()
            .securityListId(getSecurityListId())
            .build())
            .securityList

        if (securityList) {
            println(securityList.displayName + ':')
            printSecurityList(this, securityList, 0)
        } else {
            println("SecurityList with id ${getSecurityListId()} was not found")
        }
    }
}
