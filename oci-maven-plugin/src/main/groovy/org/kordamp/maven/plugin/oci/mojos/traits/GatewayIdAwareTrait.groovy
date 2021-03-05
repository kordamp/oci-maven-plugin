/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021 Andres Almiray.
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
import org.kordamp.maven.plugin.oci.mojos.interfaces.PathAware

import static com.oracle.bmc.OCID.isValid
import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.4.0
 */
@CompileStatic
trait GatewayIdAwareTrait implements PathAware, ExecutionIdAware {
    @Parameter(property = 'oci.gateway.id', name = 'igatewayId')
    String gatewayId

    String getGatewayId() {
        stringProperty(this, 'OCI_GATEWAY_ID', 'oci.gateway.id', this.@gatewayId)
    }

    void validateGatewayId() {
        if (isBlank(getGatewayId())) {
            throw new IllegalStateException("Missing value for 'gatewayId' in $path")
        }
        if (!isValid(getGatewayId())) {
            throw new IllegalStateException("Gateway id '${getGatewayId()}' is invalid")
        }
    }
}
