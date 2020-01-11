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
package org.kordamp.maven.plugin.oci.mojos.list

import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Image
import com.oracle.bmc.core.requests.ListImagesRequest
import com.oracle.bmc.core.responses.ListImagesResponse
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.kordamp.maven.plugin.oci.mojos.AbstractOCIMojo
import org.kordamp.maven.plugin.oci.mojos.printers.ImagePrinter
import org.kordamp.maven.plugin.oci.mojos.traits.CompartmentIdAwareTrait
import org.kordamp.maven.plugin.oci.mojos.traits.VerboseAwareTrait

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
@Mojo(name = 'list-images')
class ListImagesMojo extends AbstractOCIMojo implements CompartmentIdAwareTrait, VerboseAwareTrait {
    @Override
    protected void executeGoal() {
        validateCompartmentId()

        ComputeClient client = createComputeClient()
        ListImagesResponse response = client.listImages(ListImagesRequest.builder()
            .compartmentId(getCompartmentId())
            .build())

        println('Total Images: ' + console.cyan(response.items.size().toString()))
        println(' ')
        for (Image image : response.items) {
            println(image.displayName + (isVerbose() ? ':' : ''))
            if (isVerbose()) {
                ImagePrinter.printImage(this, image, 0)
            }
        }
    }
}
