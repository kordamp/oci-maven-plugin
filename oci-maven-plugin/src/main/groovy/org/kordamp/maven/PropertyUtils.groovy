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
package org.kordamp.maven

import groovy.transform.CompileStatic
import org.kordamp.maven.plugin.oci.mojos.interfaces.ExecutionIdAware

import java.nio.file.Paths

import static StringUtils.isBlank
import static StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class PropertyUtils {
    private static String normalizePath(String path, String delimiter) {
        if (':' == path) {
            return ''
        }
        return path.replace(':', delimiter) + delimiter
    }

    private static String resolveValue(String envKey,
                                       String propertyKey,
                                       ExecutionIdAware executionIdAware) {
        String value = System.getenv(normalizePath(executionIdAware.executionId, '_').toUpperCase() + envKey)
        if (isBlank(value)) value = System.getProperty(normalizePath(executionIdAware.executionId, '.') + propertyKey)
        if (isBlank(value)) value = System.getenv(envKey)
        if (isBlank(value)) value = System.getProperty(propertyKey)

        value
    }

    static String stringProperty(ExecutionIdAware executionIdAware,
                                 String envKey,
                                 String propertyKey,
                                 String alternateValue) {
        String value = resolveValue(envKey, propertyKey, executionIdAware)
        return isNotBlank(value) ? value : alternateValue
    }

    static boolean booleanProperty(ExecutionIdAware executionIdAware,
                                   String envKey,
                                   String propertyKey,
                                   boolean alternateValue) {
        String value = resolveValue(envKey, propertyKey, executionIdAware)
        if (isNotBlank(value)) {
            return Boolean.parseBoolean(value)
        }
        return alternateValue
    }

    static Integer integerProperty(ExecutionIdAware executionIdAware,
                                   String envKey,
                                   String propertyKey,
                                   Integer alternateValue) {
        String value = resolveValue(envKey, propertyKey, executionIdAware)
        if (isNotBlank(value)) {
            return Integer.parseInt(value)
        }
        return alternateValue
    }

    static File fileProperty(ExecutionIdAware executionIdAware,
                             String envKey,
                             String propertyKey,
                             File alternateValue) {
        String value = resolveValue(envKey, propertyKey, executionIdAware)
        if (isNotBlank(value)) {
            return Paths.get(value).toFile()
        }
        return alternateValue
    }

    static File directoryProperty(ExecutionIdAware executionIdAware,
                                  String envKey,
                                  String propertyKey,
                                  File alternateValue) {
        return fileProperty(executionIdAware, envKey, propertyKey, alternateValue)
    }
}
