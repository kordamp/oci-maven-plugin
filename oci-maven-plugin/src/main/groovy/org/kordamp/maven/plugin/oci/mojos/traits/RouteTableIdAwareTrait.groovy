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
package org.kordamp.maven.plugin.oci.mojos.traits

import com.oracle.bmc.OCID
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.plugin.oci.mojos.interfaces.PathAware

import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait RouteTableIdAwareTrait implements PathAware {
    @Parameter(property = 'oci.route.tale.id', name = 'routeTableId')
    String routeTableId

    String getRouteTableId() {
        stringProperty('OCI_ROUTE_TABLE_ID', 'oci.route.tale.id', this.@routeTableId)
    }

    void validateRouteTableId() {
        if (isBlank(getRouteTableId())) {
            throw new IllegalStateException("Missing value for 'routeTableId' in $path")
        }
        if (!OCID.isValid(getRouteTableId())) {
            throw new IllegalStateException("RouteTable id '${getRouteTableId()}' is invalid")
        }
    }
}
