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
package org.kordamp.maven.plugin.oci.mojos

import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.Region
import com.oracle.bmc.apigateway.GatewayClient
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.database.DatabaseClient
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.resourcesearch.ResourceSearchClient
import groovy.transform.CompileStatic
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.kordamp.maven.plugin.oci.Banner
import org.kordamp.maven.plugin.oci.mojos.interfaces.OCIMojo

import java.text.SimpleDateFormat
import java.util.function.Supplier

import static org.kordamp.maven.PropertyUtils.booleanProperty
import static org.kordamp.maven.PropertyUtils.fileProperty
import static org.kordamp.maven.PropertyUtils.stringProperty
import static org.kordamp.maven.StringUtils.isBlank
import static org.kordamp.maven.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractOCIMojo extends AbstractReportingMojo implements OCIMojo {
    protected static final String CONFIG_LOCATION = '~/.oci/config'
    protected static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    protected final List<AutoCloseable> closeables = new ArrayList<>()
    private AuthenticationDetailsProvider authenticationDetailsProvider

    @Parameter(defaultValue = '${project}', readonly = true, required = true, name = 'project')
    private MavenProject project

    @Parameter(property = 'oci.profile', defaultValue = 'DEFAULT')
    private String profile

    @Parameter(property = 'oci.region')
    private String region

    @Parameter(property = 'oci.userId')
    private String userId

    @Parameter(property = 'oci.tenantId')
    private String tenantId

    @Parameter(property = 'oci.fingerprint')
    private String fingerprint

    @Parameter(property = 'oci.passphrase')
    private String passphrase

    @Parameter(property = 'oci.keyfile')
    private File keyfile

    @Parameter(property = 'oci.input.properties')
    private File inputProperties

    @Parameter(property = 'oci.display.stream.logs')
    private boolean displayStreamLogs

    @Parameter
    private boolean skip

    private final Properties _inputProperties = new Properties()

    String getProfile() {
        stringProperty(this, 'OCI_PROFILE', 'oci.profile', this.@profile)
    }

    String getRegion() {
        stringProperty(this, 'OCI_REGION', 'oci.region', this.@region)
    }

    String getUserId() {
        stringProperty(this, 'OCIT_USERID', 'oci.userId', this.@userId)
    }

    String getTenantId() {
        stringProperty(this, 'OCI_TENANTID', 'oci.tenantId', this.@tenantId)
    }

    String getFingerprint() {
        stringProperty(this, 'OCI_FINGERPRINT', 'oci.fingerprint', this.@fingerprint)
    }

    String getPassphrase() {
        stringProperty(this, 'OCI_PASSPHRASE', 'oci.passphrase', this.@passphrase)
    }

    File getKeyfile() {
        fileProperty(this, 'OCI_KEYFILE', 'oci.keyfile', this.@keyfile)
    }

    boolean isDisplayStreamLogs() {
        booleanProperty(this, 'OCI_DISPLAY_STREAM_LOGS', 'oci.display.stream.logs', this.@displayStreamLogs)
    }

    final void execute() {
        if (skip) {
            log.info('Execution skipped"')
            return
        }

        Banner.display(project, getLog())
        System.setProperty('sun.net.http.allowRestrictedHeaders', 'true')
        System.setProperty('oci.javasdk.extra.stream.logs.enabled', isDisplayStreamLogs().toString())

        loadProperties()
        interpolateProperties()
        executeGoal()

        closeables.each { c -> c.close() }
        closeables.clear()
    }

    private void loadProperties() {
        if (inputProperties != null && inputProperties.exists()) {
            try {
                _inputProperties.load(new FileInputStream(inputProperties))
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    private void interpolateProperties() {
        List<String> props = [
            'profile',
            'region',
            'userId',
            'tenantId',
            'fingerprint',
            'passphrase'
        ] + resolveInterpolationProperties()

        for (String p : props) {
            String propertyKey = String.valueOf(getProperty(p))
            if (isPropertyKey(propertyKey)) {
                setProperty(p, getInputProperty(propertyKey))
            }
        }
    }

    protected List<String> resolveInterpolationProperties() {
        []
    }

    private String getInputProperty(String key) {
        _inputProperties.getProperty(key[2..-2])
    }

    private boolean isPropertyKey(String str) {
        isNotBlank(str) && (str.startsWith('#{') && str.endsWith('}') && str.size() > 3)
    }

    abstract protected void executeGoal()

    protected AuthenticationDetailsProvider resolveAuthenticationDetailsProvider() {
        if (authenticationDetailsProvider) {
            return authenticationDetailsProvider
        }

        if (isBlank(getUserId()) && isBlank(getTenantId()) && isBlank(getFingerprint()) && !getKeyfile()) {
            ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(CONFIG_LOCATION, getProfile())
            return new ConfigFileAuthenticationDetailsProvider(configFile)
        }

        List<String> errors = []
        if (isBlank(getUserId())) {
            errors << "Missing value for 'userId' for goal $path".toString()
        }
        if (isBlank(getRegion())) {
            errors << "Missing value for 'tenantId' for goal $path".toString()
        }
        if (isBlank(getRegion())) {
            errors << "Missing value for 'fingerprint' for goal $path".toString()
        }
        if (isBlank(getRegion())) {
            errors << "Missing value for 'region' for goal $path".toString()
        }
        if (!getKeyfile()) {
            errors << "Missing value for 'keyfile' for goal $path".toString()
        }

        if (errors.size() > 0) {
            throw new IllegalStateException(errors.join('\n'))
        }

        authenticationDetailsProvider = SimpleAuthenticationDetailsProvider.builder()
            .userId(getUserId())
            .tenantId(getTenantId())
            .fingerprint(getFingerprint())
            .region(Region.fromRegionId(getRegion()))
            .privateKeySupplier(new Supplier<InputStream>() {
                @Override
                InputStream get() {
                    new FileInputStream(getKeyfile())
                }
            })
            .passPhrase(getPassphrase() ? getPassphrase() : '')
            .build()

        authenticationDetailsProvider
    }

    @Override
    protected void doPrintMapEntry(String key, value, int offset) {
        if (value instanceof CharSequence) {
            if (isNotBlank((String.valueOf(value)))) {
                super.doPrintMapEntry(key, value, offset)
            }
        } else {
            super.doPrintMapEntry(key, value, offset)
        }
    }

    @Override
    void printKeyValue(String key, Object value, int offset) {
        doPrintMapEntry(key, value, offset)
    }

    @Override
    void printMap(String key, Map<String, ?> map, int offset) {
        if (map && !map.isEmpty()) {
            println(('    ' * offset) + key + ':')
            doPrintMap(map, offset + 1)
        }
    }

    @Override
    void printCollection(String key, Collection<?> collection, int offset) {
        if (collection && !collection.isEmpty()) {
            println(('    ' * offset) + key + ':')
            doPrintCollection(collection, offset + 1)
        }
    }

    @Override
    String state(String state) {
        if (isNotBlank(state)) {
            switch (state) {
                case 'Creating':
                case 'Provisioning':
                case 'Restoring':
                case 'Importing':
                case 'Exporting':
                case 'Starting':
                case 'CreatingImage':
                case 'InProgress':
                case 'Canceling':
                case 'Deleting':
                case 'Terminating':
                case 'Moving':
                    return console.yellow(state)
                case 'Available':
                case 'Running':
                case 'Active':
                case 'Completed':
                    return console.green(state)
                case 'Inactive':
                case 'Stopping':
                case 'Stopped':
                case 'Accepted':
                    return console.cyan(state)
                case 'Disabled':
                case 'Deleted':
                case 'Terminated':
                case 'Faulty':
                case 'Failed':
                case 'Canceled':
                    return console.red(state)
            }
        }
        state
    }

    static String format(Date date) {
        return new SimpleDateFormat(TIMESTAMP_FORMAT).format(date)
    }

    @Override
    String getPath() {
        ':' + getClass().getDeclaredAnnotation(Mojo)?.name()
    }

    protected IdentityClient createIdentityClient() {
        IdentityClient client = new IdentityClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected ComputeClient createComputeClient() {
        ComputeClient client = new ComputeClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected VirtualNetworkClient createVirtualNetworkClient() {
        VirtualNetworkClient client = new VirtualNetworkClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected BlockstorageClient createBlockstorageClient() {
        BlockstorageClient client = new BlockstorageClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected ResourceSearchClient createResourceSearchClient() {
        ResourceSearchClient client = new ResourceSearchClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected ObjectStorageClient createObjectStorageClient() {
        ObjectStorageClient client = new ObjectStorageClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected ObjectStorageAsyncClient createObjectStorageAsyncClient() {
        ObjectStorageAsyncClient client = new ObjectStorageAsyncClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected DatabaseClient createDatabaseClient() {
        DatabaseClient client = new DatabaseClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }

    protected GatewayClient createGatewayClient() {
        GatewayClient client = new GatewayClient(resolveAuthenticationDetailsProvider())
        if (isNotBlank(getRegion())) {
            client.setRegion(getRegion())
        }
        closeables << client
        client
    }
}
