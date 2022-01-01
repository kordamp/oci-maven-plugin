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
package org.kordamp.maven.plugin.oci.mojos.list

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalDelimiterAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalEndAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalFieldsAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalLimitAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalPrefixAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalStartAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

import static org.kordamp.maven.StringUtils.isNotBlank
import static org.kordamp.maven.plugin.oci.mojos.printers.ObjectSummaryPrinter.printObjectSummary

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'list-objects')
class ListObjectsMojo extends AbstractOCIMojo implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    OptionalPrefixAwareTrait,
    OptionalDelimiterAwareTrait,
    OptionalStartAwareTrait,
    OptionalEndAwareTrait,
    OptionalLimitAwareTrait,
    OptionalFieldsAwareTrait,
    VerboseAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'namespaceName',
            'bucketName',
            'prefix',
            'delimiter',
            'start',
            'end',
            'limit',
            'fields'
        ]
    }

    @Override
    protected void executeGoal() {
        validateNamespaceName()
        validateBucketName()

        ObjectStorageClient client = createObjectStorageClient()
        ListObjectsRequest.Builder builder = ListObjectsRequest.builder()
            .namespaceName(getNamespaceName())
            .bucketName(getBucketName())

        Integer limit = getLimit() ?: 1000
        if (null != limit) {
            builder = builder.limit(limit)
        }

        String s = getPrefix()
        if (isNotBlank(s)) {
            builder = builder.prefix(s)
        }
        s = getDelimiter()
        if (isNotBlank(s)) {
            builder = builder.delimiter(s)
        }
        s = getStart()
        if (isNotBlank(s)) {
            builder = builder.start(s)
        }
        s = getEnd()
        if (isNotBlank(s)) {
            builder = builder.end(s)
        }
        s = getFields()
        if (isNotBlank(s)) {
            builder = builder.fields(s)
        }

        ListObjectsResponse response = client.listObjects(builder.build())

        println('Total Objects: ' + console.cyan(response.listObjects.objects.size().toString()))
        println(' ')
        for (ObjectSummary objectSummary : response.listObjects.objects) {
            println(objectSummary.name + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                printObjectSummary(this, objectSummary, 0)
            }
        }
    }
}
