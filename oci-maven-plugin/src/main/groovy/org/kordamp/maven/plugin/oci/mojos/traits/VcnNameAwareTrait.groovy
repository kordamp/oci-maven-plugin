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
package org.kordamp.maven.plugin.oci.mojos.traits

import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.plugin.oci.mojos.interfaces.ExecutionIdAware
import org.kordamp.maven.plugin.oci.mojos.interfaces.LogAware
import org.kordamp.maven.plugin.oci.mojos.interfaces.PathAware

import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait VcnNameAwareTrait implements PathAware, ExecutionIdAware, LogAware {
    @Parameter(property = 'oci.vcn.name', name = 'vcnName')
    String vcnName

    String getVcnName() {
        stringProperty(this, 'OCI_VCN_NAME', 'oci.vcn.name', this.@vcnName)
    }

    void validateVcnName() {
        if (isBlank(getVcnName())) {
            setVcnName('vcn-' + UUID.randomUUID().toString())
            log.warn("Missing value for 'vcnName' in $path. Value set to ${getVcnName()}")
        }
    }
}
