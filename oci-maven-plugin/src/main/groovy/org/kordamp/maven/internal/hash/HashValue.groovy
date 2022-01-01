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
package org.kordamp.maven.internal.hash

import groovy.transform.CompileStatic

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class HashValue {
    private final BigInteger digest

    HashValue(byte[] digest) {
        this.digest = new BigInteger(1, digest)
    }

    HashValue(String hexString) {
        this.digest = new BigInteger(hexString, 16)
    }

    static HashValue parse(String inputString) {
        return inputString != null && inputString.length() != 0 ? new HashValue(parseInput(inputString)) : null
    }

    private static String parseInput(String inputString) {
        if (inputString == null) {
            return null
        } else {
            String cleaned = inputString.trim().toLowerCase()
            int spaceIndex = cleaned.indexOf(32)
            if (spaceIndex != -1) {
                String firstPart = cleaned.substring(0, spaceIndex)
                if (!firstPart.startsWith("md") && !firstPart.startsWith("sha")) {
                    if (firstPart.endsWith(":")) {
                        cleaned = cleaned.substring(spaceIndex + 1).replace(" ", "")
                    } else {
                        cleaned = cleaned.substring(0, spaceIndex)
                    }
                } else {
                    cleaned = cleaned.substring(cleaned.lastIndexOf(32) + 1)
                }
            }

            return cleaned
        }
    }

    String asCompactString() {
        return this.digest.toString(36)
    }

    String asHexString() {
        return this.digest.toString(16)
    }

    byte[] asByteArray() {
        return this.digest.toByteArray()
    }

    BigInteger asBigInteger() {
        return this.digest
    }

    String asZeroPaddedHexString(int expectedLength) {
        return this.asHexString().padLeft(expectedLength, '0')
    }

    boolean equals(Object other) {
        if (this == other) {
            return true
        } else if (!(other instanceof HashValue)) {
            return false
        } else {
            HashValue otherHashValue = (HashValue) other
            return this.digest == otherHashValue.digest
        }
    }

    int hashCode() {
        return this.digest.hashCode()
    }
}
