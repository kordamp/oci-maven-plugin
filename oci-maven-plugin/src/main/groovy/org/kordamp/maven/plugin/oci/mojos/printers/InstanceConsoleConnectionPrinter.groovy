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
package org.kordamp.maven.plugin.oci.mojos.printers


import com.oracle.bmc.core.model.InstanceConsoleConnection
import groovy.transform.CompileStatic
import org.kordamp.maven.plugin.oci.mojos.interfaces.ValuePrinter

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class InstanceConsoleConnectionPrinter {
    static void printInstanceConsoleConnection(ValuePrinter printer, InstanceConsoleConnection connection, int offset) {
        printer.printKeyValue('ID', connection.id, offset + 1)
        printer.printKeyValue('Compartment ID', connection.compartmentId, offset + 1)
        printer.printKeyValue('Instance ID', connection.instanceId, offset + 1)
        printer.printKeyValue('Fingerprint', connection.fingerprint, offset + 1)
        printer.printKeyValue('Connection String', connection.connectionString, offset + 1)
        printer.printKeyValue('VNC Connection String', connection.vncConnectionString, offset + 1)
        printer.printKeyValue('Lifecycle State', printer.state(connection.lifecycleState.name()), offset + 1)
        printer.printMap('Defined Tags', connection.definedTags, offset + 1)
        printer.printMap('Freeform Tags', connection.freeformTags, offset + 1)
    }
}
