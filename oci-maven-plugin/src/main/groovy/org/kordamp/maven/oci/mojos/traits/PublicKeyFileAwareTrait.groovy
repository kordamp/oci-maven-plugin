/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Andres Almiray.
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
package org.kordamp.maven.oci.mojos.traits

import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.oci.mojos.interfaces.PathAware

import static org.kordamp.maven.PropertyUtils.fileProperty

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait PublicKeyFileAwareTrait implements PathAware {
    @Parameter(property = 'oci.public.key.file', name = 'publicKeyFile')
    private File publicKeyFile

    File getPublicKeyFile() {
        fileProperty('OCI_PUBLIC_KEY_FILE', 'oci.public.key.file', this.@publicKeyFile)
    }

    void validatePublicKeyFile() {
        if (!publicKeyFile) {
            throw new IllegalStateException("Missing value for 'publicKeyFile' in $path")
        }
    }
}