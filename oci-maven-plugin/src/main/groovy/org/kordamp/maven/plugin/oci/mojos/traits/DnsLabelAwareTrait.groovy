/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Andres Almiray.
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

import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Parameter
import org.kordamp.maven.internal.hash.HashUtil
import org.kordamp.maven.plugin.oci.mojos.interfaces.LogAware
import org.kordamp.maven.plugin.oci.mojos.interfaces.PathAware

import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
trait DnsLabelAwareTrait implements PathAware, LogAware {
    @Parameter(property = 'oci.dns.label', name = 'dnsLabel')
    String dnsLabel

    void setDnsLabel(String dnsLabel) {
        String label = dnsLabel?.replace('.', '')?.replace('-', '')
        if (label?.length() > 15) label = label?.substring(0, 14)
        this.@dnsLabel = label
    }

    String getDnsLabel() {
        stringProperty('OCI_DNS_LABEL', 'oci.dns.label', this.@dnsLabel)
    }

    void validateDnsLabel(String seed) {
        if (isBlank(getDnsLabel())) {
            setDnsLabel('dns' + HashUtil.sha1(seed.bytes).asHexString()[0..11])
            log.warn("Missing value for 'dnsLabel' in $path. Value set to ${getDnsLabel()}")
        } else {
            setDnsLabel(getDnsLabel())
        }
    }
}
