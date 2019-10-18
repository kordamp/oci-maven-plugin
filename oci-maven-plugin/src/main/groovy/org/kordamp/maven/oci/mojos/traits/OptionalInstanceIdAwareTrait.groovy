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

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait OptionalInstanceIdAwareTrait implements PathAware {
    @Parameter(property = 'oci.instance.id', name = 'instanceId')
    String instanceId

    String getInstanceId() {
        stringProperty('OCI_INSTANCE_ID', 'oci.instance.id', this.@instanceId)
    }

    void validateInstanceId() {
        if (isNotBlank(getInstanceId()) && !isValid(getInstanceId())) {
            throw new IllegalStateException("Instance id '${getInstanceId()}' is invalid")
        }
    }
}
