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
import com.oracle.bmc.objectstorage.model.CopyObjectDetails
import com.oracle.bmc.objectstorage.model.WorkRequest
import com.oracle.bmc.objectstorage.model.WorkRequestLogEntry
import com.oracle.bmc.objectstorage.requests.CopyObjectRequest
import com.oracle.bmc.objectstorage.requests.GetWorkRequestRequest
import com.oracle.bmc.objectstorage.requests.ListWorkRequestLogsRequest
import com.oracle.bmc.objectstorage.responses.CopyObjectResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.DestinationObjectNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.ObjectNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDestinationBucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDestinationNamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDestinationRegionAwareTrait

import static org.kordamp.maven.plugin.oci.mojos.object.HeadObjectMojo.headObject

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'copy-object')
class CopyObjectMojo extends AbstractOCIMojo implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    ObjectNameAwareTrait,
    OptionalDestinationRegionAwareTrait,
    OptionalDestinationNamespaceNameAwareTrait,
    OptionalDestinationBucketNameAwareTrait,
    DestinationObjectNameAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'namespaceName',
            'bucketName',
            'objectName',
            'destinationRegion',
            'destinationNamespaceName',
            'destinationBucketName',
            'destinationObjectName'
        ]
    }

    @Override
    protected void executeGoal() {
        validateNamespaceName()
        validateBucketName()
        validateObjectName()
        validateDestinationObjectName()

        // TODO: check if destination exists

        ObjectStorageClient client = createObjectStorageClient()

        String _destinationRegion = getDestinationRegion() ?: getRegion()
        String _destinationNamespace = getDestinationNamespaceName() ?: getNamespaceName()
        String _destinationBucket = getDestinationBucketName() ?: getBucketName()

        CopyObjectDetails.Builder details = CopyObjectDetails.builder()
            .sourceObjectName(getObjectName())
            .destinationRegion(_destinationRegion)
            .destinationNamespace(_destinationNamespace)
            .destinationBucket(_destinationBucket)
            .destinationObjectName(getDestinationObjectName())

        CopyObjectResponse response = client.copyObject(CopyObjectRequest.builder()
            .namespaceName(getNamespaceName())
            .bucketName(getBucketName())
            .copyObjectDetails(details.build())
            .build())

        println("Waiting for copy to finish.")
        WorkRequest workRequest = client.waiters
            .forWorkRequest(GetWorkRequestRequest.builder()
                .workRequestId(response.getOpcWorkRequestId())
                .build())
            .execute().
            workRequest
        println("Work request is in ${state(workRequest.status.name())} state.")

        println("Verifying that the object has been copied.")
        switch (workRequest.status) {
            case WorkRequest.Status.Completed:
                client.setRegion(_destinationRegion)

                headObject(this,
                    createObjectStorageClient(),
                    _destinationNamespace,
                    _destinationBucket,
                    getDestinationObjectName())
                break
            default:
                List<WorkRequestLogEntry> entries = client.listWorkRequestLogs(ListWorkRequestLogsRequest.builder()
                    .workRequestId(workRequest.id)
                    .build())
                    .items
                for (WorkRequestLogEntry entry : entries) {
                    println("[${console.cyan(format(entry.timestamp))}] $entry.message")
                }
                break
        }
    }
}
