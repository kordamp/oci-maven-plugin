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
import com.oracle.bmc.objectstorage.model.BucketSummary
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalLimitAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalPageAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

import static org.kordamp.maven.StringUtils.isNotBlank
import static org.kordamp.maven.plugin.oci.mojos.printers.BucketSummaryPrinter.printBucketSummary

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'list-buckets')
class ListBucketsMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait,
    NamespaceNameAwareTrait,
    OptionalPageAwareTrait,
    OptionalLimitAwareTrait,
    VerboseAwareTrait {
    @Override
    protected List<String> resolveInterpolationProperties() {
        [
            'compartmentId',
            'namespaceName',
            'page',
            'limit'
        ]
    }

    @Override
    protected void executeGoal() {
        validateCompartmentId()
        validateNamespaceName()

        ObjectStorageClient client = createObjectStorageClient()
        ListBucketsRequest.Builder builder = ListBucketsRequest.builder()
            .compartmentId(getCompartmentId())
            .namespaceName(getNamespaceName())

        Integer limit = getLimit() ?: 1000
        if (null != limit) {
            builder = builder.limit(limit)
        }

        String page = getPage()
        if (isNotBlank(page)) {
            builder = builder.page(page)
        }

        ListBucketsResponse response = client.listBuckets(builder.build())

        println('Total Buckets: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (BucketSummary bucketSummary : response.items) {
            println(bucketSummary.name + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                printBucketSummary(this, bucketSummary, 0)
            }
        }
    }
}
