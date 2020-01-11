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
package org.kordamp.maven.internal.hash

import groovy.transform.CompileStatic
import org.kordamp.maven.internal.UncheckedException

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class HashUtil {
    HashUtil() {
    }

    static HashValue createHash(String scriptText, String algorithm) {
        MessageDigest messageDigest = createMessageDigest(algorithm)
        messageDigest.update(scriptText.getBytes())
        return new HashValue(messageDigest.digest())
    }

    static HashValue createHash(File file, String algorithm) {
        try {
            return createHash((InputStream) (new FileInputStream(file)), algorithm)
        } catch (UncheckedIOException var3) {
            throw new UncheckedIOException(String.format("Failed to create %s hash for file %s.", algorithm, file.getAbsolutePath()), var3.getCause())
        } catch (FileNotFoundException var4) {
            throw new UncheckedIOException(var4)
        }
    }

    static HashValue createHash(InputStream instr, String algorithm) {
        try {
            MessageDigest messageDigest = createMessageDigest(algorithm)
            byte[] buffer = new byte[4096]

            try {
                while (true) {
                    int nread = instr.read(buffer)
                    if (nread < 0) {
                        return new HashValue(messageDigest.digest())
                    }

                    messageDigest.update(buffer, 0, nread)
                }
            } finally {
                instr.close()
            }
        } catch (IOException var9) {
            throw new UncheckedIOException(var9)
        }
    }

    private static MessageDigest createMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm)
        } catch (NoSuchAlgorithmException var2) {
            throw UncheckedException.throwAsUncheckedException(var2)
        }
    }

    static String createCompactMD5(String scriptText) {
        return createHash(scriptText, "MD5").asCompactString()
    }

    static String compactStringFor(byte[] digest) {
        return (new HashValue(digest)).asCompactString()
    }

    static HashValue sha1(byte[] bytes) {
        return createHash((InputStream) (new ByteArrayInputStream(bytes)), "SHA1")
    }

    static HashValue sha1(InputStream inputStream) {
        return createHash(inputStream, "SHA1")
    }

    static HashValue sha1(File file) {
        return createHash(file, "SHA1")
    }

    static HashValue sha256(byte[] bytes) {
        return createHash((InputStream) (new ByteArrayInputStream(bytes)), "SHA-256")
    }

    static HashValue sha256(InputStream inputStream) {
        return createHash(inputStream, "SHA-256")
    }

    static HashValue sha256(File file) {
        return createHash(file, "SHA-256")
    }
}
