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
package org.kordamp.maven.plugin.oci.mojos.object

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.HeadObjectRequest
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.ObjectNameAwareTrait

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'head-object')
class HeadObjectMojo extends AbstractOCIMojo implements NamespaceNameAwareTrait,
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

        headObject(this,
            createObjectStorageClient(),
            getNamespaceName(),
            getBucketName(),
            getObjectName())
    }

    static HeadObjectResponse headObject(OCIMojo owner,
                                         ObjectStorageClient storageClient,
                                         String namespaceName,
                                         String bucketName,
                                         String objectName) {
        HeadObjectResponse response = storageClient.headObject(HeadObjectRequest.builder()
            .namespaceName(namespaceName)
            .bucketName(bucketName)
            .objectName(objectName)
            .build())

        println(objectName + ':')
        owner.printKeyValue('ETag', response.ETag, 1)
        owner.printKeyValue('Modified', !response.notModified, 1)
        owner.printKeyValue('Last Modified', response.lastModified, 1)
        owner.printKeyValue('Content Length', response.contentLength, 1)
        owner.printKeyValue('Content Type', response.contentType, 1)
        owner.printKeyValue('Content MD5', response.contentMd5, 1)
        owner.printKeyValue('Content Encoding', response.contentEncoding, 1)
        owner.printKeyValue('Content Language', response.contentLanguage, 1)
        owner.printKeyValue('Archival State', response.archivalState, 1)
        owner.printKeyValue('Time of Archival', response.timeOfArchival, 1)
    }
}
