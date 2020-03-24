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


import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalCompartmentIdAwareTrait

import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'get-namespace')
class GetNamespaceMojo extends AbstractOCIMojo implements OptionalCompartmentIdAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId'
        ]
    }

    @Override
    protected void executeGoal() {
        validateCompartmentId()

        GetNamespaceRequest.Builder builder = GetNamespaceRequest.builder()
        String compartmentId = getCompartmentId()
        if (isNotBlank(compartmentId)) {
            builder = builder.compartmentId(compartmentId)
        }

        ObjectStorageClient client = createObjectStorageClient()
        String namespace = client.getNamespace(builder.build())
            .value

        println(namespace)
    }
}
