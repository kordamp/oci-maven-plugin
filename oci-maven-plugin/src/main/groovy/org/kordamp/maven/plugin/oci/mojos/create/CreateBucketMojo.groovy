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
package org.kordamp.maven.plugin.oci.mojos.create

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.Bucket
import com.oracle.bmc.objectstorage.model.CreateBucketDetails
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.requests.GetBucketRequest
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.printers.BucketPrinter.printBucket

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'create-bucket')
class CreateBucketMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    VerboseAwareTrait {

    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId',
            'namespaceName',
            'bucketName'
        ]
    }

    @Override
    void executeGoal() {
        validateCompartmentId()
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()

        maybeCreateBucket(this,
            client,
            getCompartmentId(),
            getNamespaceName(),
            getBucketName(),
            isVerbose())
    }

    static Bucket maybeCreateBucket(OCIMojo owner,
                                    ObjectStorage client,
                                    String compartmentId,
                                    String namespaceName,
                                    String bucketName,
                                    boolean verbose) {
        // 1. Check if it exists
        try {
            List<GetBucketRequest.Fields> fields = new ArrayList<>(2)
            fields.add(GetBucketRequest.Fields.ApproximateCount)
            fields.add(GetBucketRequest.Fields.ApproximateSize)

            Bucket bucket = client.getBucket(GetBucketRequest.builder()
                .namespaceName(namespaceName)
                .bucketName(bucketName)
                .fields(fields)
                .build())
                .bucket
            println("Bucket '${bucketName}' already exists.")
            if (verbose) printBucket(owner, bucket, 0)
            return bucket
        } catch (BmcException e) {
            // exception most likely means the bucket does not exist, continue
        }

        // 2. Create
        println('Provisioning Bucket. This may take a while.')
        Bucket bucket = client.createBucket(CreateBucketRequest.builder()
            .namespaceName(namespaceName)
            .createBucketDetails(CreateBucketDetails.builder()
                .compartmentId(compartmentId)
                .name(bucketName)
                .build())
            .build())
            .bucket

        println("Bucket '${bucketName}' has been provisioned.")
        if (verbose) printBucket(owner, bucket, 0)
        bucket
    }
}
