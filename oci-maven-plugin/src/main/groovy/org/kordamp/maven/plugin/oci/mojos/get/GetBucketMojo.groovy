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
import com.oracle.bmc.objectstorage.model.Bucket
import com.oracle.bmc.objectstorage.requests.GetBucketRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.BucketPrinter.printBucket

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'get-bucket')
class GetBucketMojo extends AbstractOCIMojo implements NamespaceNameAwareTrait,
    BucketNameAwareTrait {
    @Override
    protected void executeGoal() {
        validateNamespaceName()
        validateBucketName()

        List<GetBucketRequest.Fields> fields = new ArrayList<>(2)
        fields.add(GetBucketRequest.Fields.ApproximateCount)
        fields.add(GetBucketRequest.Fields.ApproximateSize)

        ObjectStorageClient client = createObjectStorageClient()
        Bucket bucket = client.getBucket(GetBucketRequest.builder()
            .namespaceName(getNamespaceName())
            .bucketName(getBucketName())
            .fields(fields)
            .build())
            .bucket

        println(bucket.name + ':')
        printBucket(this, bucket, 0)
    }
}
