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
package org.kordamp.maven

import groovy.transform.CompileStatic

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class AnsiConsole {
    private static final String START = '\u001B['
    private static final String END = '\u001b[0m'
    private boolean plain

    AnsiConsole() {
        plain = 'plain'.equalsIgnoreCase(System.getProperty('org.kordamp.maven.console'))
    }

    String black(CharSequence s) {
        (plain ? s : START + '30m' + s + END).toString()
    }

    String red(CharSequence s) {
        (plain ? s : START + '31m' + s + END).toString()
    }

    String green(CharSequence s) {
        (plain ? s : START + '32m' + s + END).toString()
    }

    String yellow(CharSequence s) {
        (plain ? s : START + '33m' + s + END).toString()
    }

    String blue(CharSequence s) {
        (plain ? s : START + '34m' + s + END).toString()
    }

    String magenta(CharSequence s) {
        (plain ? s : START + '35m' + s + END).toString()
    }

    String cyan(CharSequence s) {
        (plain ? s : START + '36m' + s + END).toString()
    }

    String white(CharSequence s) {
        (plain ? s : START + '37m' + s + END).toString()
    }

    String erase(CharSequence s) {
        (plain ? s : START + '2K' + s).toString()
    }
}
