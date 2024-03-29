/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Andres Almiray.
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

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.ObjectNameAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.GetObjectResponsePrinter.printObject

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'get-object')
class GetObjectMojo extends AbstractOCIMojo implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    ObjectNameAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'namespaceName',
            'bucketName',
            'objectName'
        ]
    }

    @Override
    protected void executeGoal() {
        validateNamespaceName()
        validateBucketName()
        validateObjectName()

        ObjectStorageClient client = createObjectStorageClient()
        GetObjectResponse object = client.getObject(GetObjectRequest.builder()
            .namespaceName(getNamespaceName())
            .bucketName(getBucketName())
            .objectName(getObjectName())
            .build())

        println(getObjectName() + ':')
        printObject(this, object, 0)
    }
}
