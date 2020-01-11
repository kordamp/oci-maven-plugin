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
package org.kordamp.maven.plugin.oci.mojos.object

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.responses.PutObjectResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.traits.BucketNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.FileAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.NamespaceNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.ObjectNameAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalContentEncodingAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalContentLanguageAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalContentMD5AwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.OptionalContentTypeAwareTrait

import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
@Mojo(name = 'put-object')
class PutObjectMojo extends AbstractOCIMojo implements NamespaceNameAwareTrait,
    BucketNameAwareTrait,
    ObjectNameAwareTrait,
    FileAwareTrait,
    OptionalContentMD5AwareTrait,
    OptionalContentLanguageAwareTrait,
    OptionalContentEncodingAwareTrait,
    OptionalContentTypeAwareTrait {
    @Override
    protected void executeGoal() {
        validateNamespaceName()
        validateBucketName()
        validateObjectName()
        validateFile()

        File theFile = getFile()
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
            .namespaceName(getNamespaceName())
            .bucketName(getBucketName())
            .objectName(getObjectName())
            .contentLength(theFile.size())
            .putObjectBody(new FileInputStream(theFile))

        String s = getContentEncoding()
        if (isNotBlank(s)) {
            builder = builder.contentEncoding(s)
        }
        s = getContentLanguage()
        if (isNotBlank(s)) {
            builder = builder.contentLanguage(s)
        }
        s = getContentMD5()
        if (isNotBlank(s)) {
            builder = builder.contentMD5(s)
        }
        s = getContentType()
        if (isNotBlank(s)) {
            builder = builder.contentType(s)
        }

        ObjectStorageClient client = createObjectStorageClient()
        PutObjectResponse response = client.putObject(builder.build())

        println(getObjectName() + ':')
        printKeyValue('ETag', response.ETag, 1)
        printKeyValue('Last Modified', response.lastModified, 1)
        printKeyValue('Content MD5', response.opcContentMd5, 1)
    }
}
