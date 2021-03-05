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
package org.kordamp.maven.internal

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Callable

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
final class UncheckedException extends RuntimeException {
    private UncheckedException(Throwable cause) {
        super(cause)
    }

    private UncheckedException(String message, Throwable cause) {
        super(message, cause)
    }

    static RuntimeException throwAsUncheckedException(Throwable t) {
        return throwAsUncheckedException(t, false)
    }

    static RuntimeException throwAsUncheckedException(Throwable t, boolean preserveMessage) {
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt()
        }

        if (t instanceof RuntimeException) {
            throw (RuntimeException) t
        } else if (t instanceof Error) {
            throw (Error) t
        } else if (t instanceof IOException) {
            if (preserveMessage) {
                throw new UncheckedIOException(t.getMessage(), t)
            } else {
                throw new UncheckedIOException(t)
            }
        } else if (preserveMessage) {
            throw new UncheckedException(t.getMessage(), t)
        } else {
            throw new UncheckedException(t)
        }
    }

    static <T> T callUnchecked(Callable<T> callable) {
        try {
            return callable.call()
        } catch (Exception var2) {
            throw throwAsUncheckedException(var2)
        }
    }

    static RuntimeException unwrapAndRethrow(InvocationTargetException e) {
        return throwAsUncheckedException(e.getTargetException())
    }
}
