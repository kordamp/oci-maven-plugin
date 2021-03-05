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

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Shape
import com.oracle.bmc.core.requests.ListShapesRequest
import com.oracle.bmc.core.responses.ListShapesResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.plugin.oci.mojos.interfaces.ExecutionIdAware
import org.kordamp.maven.plugin.oci.mojos.interfaces.PathAware

import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait ShapeAwareTrait implements PathAware, ExecutionIdAware {
    @Parameter(property = 'oci.shape', name = 'shape')
    String shape

    String getShape() {
        stringProperty(this, 'OCI_SHAPE', 'oci.shape', this.@shape)
    }

    void validateShape() {
        if (isBlank(getShape())) {
            throw new IllegalStateException("Missing value for 'shape' in $path")
        }
    }

    Shape validateShape(ComputeClient client, String compartmentId) {
        ListShapesResponse response = client.listShapes(ListShapesRequest.builder()
            .compartmentId(compartmentId)
            .build())
        Shape shape = response.items.find { Shape sh -> sh.shape == getShape() }
        if (!shape) throw new IllegalStateException("Invalid shape ${getShape()}")
        shape
    }
}
