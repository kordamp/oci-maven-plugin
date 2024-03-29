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
package org.kordamp.maven.plugin.oci.mojos.delete

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.WaitForCompletionAwareTrait

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'delete-bucket')
class DeleteBucketMojo extends AbstractOCIMojo implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    WaitForCompletionAwareTrait {

    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'namespaceName',
            'bucketName'
        ]
    }

    @Override
    protected void executeGoal() {
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()

        println("Deleting Bucket ${getBucketName()}")

        client.deleteBucket(DeleteBucketRequest.builder()
            .namespaceName(getNamespaceName())
            .bucketName(getBucketName())
            .build())
    }
}
