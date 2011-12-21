/** 
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements. See the NOTICE file 
 * distributed with this work for additional information 
 * regarding copyright ownership. The ASF licenses this file 
 * to you under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations 
 * under the License. 
 */
package org.apache.cxf.dosgi.dsw.service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class RemoteServiceAdminInstance implements RemoteServiceAdmin {
    private static final Logger LOG = LogUtils.getL7dLogger(RemoteServiceAdminInstance.class);

    private BundleContext bctx;
    private RemoteServiceAdminCore rsaCore;

    private boolean closed = false;

    private List<ImportRegistration> importedServices = new ArrayList<ImportRegistration>();
    private List<ExportRegistration> exportedServices = new ArrayList<ExportRegistration>();

    public RemoteServiceAdminInstance(BundleContext bc, RemoteServiceAdminCore core) {
        bctx = bc;
        rsaCore = core;
    }

    public List /* ExportRegistration */exportService(ServiceReference ref, Map properties)
        throws IllegalArgumentException, UnsupportedOperationException {

        SecurityManager sm = System.getSecurityManager();
        EndpointPermission epp = new EndpointPermission("*", EndpointPermission.EXPORT);
        
        
        if (sm != null) {
            sm.checkPermission(epp);
        }
        
        
        final ServiceReference refFinal = ref;
        final Map propertiesFinal = properties;
        
        return AccessController.doPrivileged(new PrivilegedAction<List>() {
            public List run() {

                if (closed)
                    return Collections.EMPTY_LIST;

                synchronized (exportedServices) {
                    List er = rsaCore.exportService(refFinal, propertiesFinal);
                    if (er != null)
                        exportedServices.addAll(er);
                    return er;
                }
            }
        });
    }

    public Collection getExportedServices() {
        
        SecurityManager sm = System.getSecurityManager();
        EndpointPermission epp = new EndpointPermission("*", EndpointPermission.READ);
        if (sm != null) {
            sm.checkPermission(epp);
        }
        
        if (closed)
            return null;
        return rsaCore.getExportedServices();
    }

    public Collection getImportedEndpoints() {
        
        SecurityManager sm = System.getSecurityManager();
        EndpointPermission epp = new EndpointPermission("*", EndpointPermission.READ);
        if (sm != null) {
            sm.checkPermission(epp);
        }
        
        if (closed)
            return null;
        return rsaCore.getImportedEndpoints();
    }

    public ImportRegistration importService(EndpointDescription endpoint) {

        final EndpointDescription epd = endpoint;

        SecurityManager sm = System.getSecurityManager();
        EndpointPermission epp = new EndpointPermission(epd, OsgiUtils.getUUID(bctx),
                                                        EndpointPermission.IMPORT);
        if (sm != null) {
            sm.checkPermission(epp);
        }

        return AccessController.doPrivileged(new PrivilegedAction<ImportRegistration>() {
            public ImportRegistration run() {

                if (closed)
                    return null;

                synchronized (importedServices) {
                    ImportRegistration ir = rsaCore.importService(epd);
                    if (ir != null)
                        importedServices.add(ir);
                    return ir;
                }
            }
        });
    }

    public void close() {
        closed = true;

        synchronized (importedServices) {
            LOG.info("Removing all services imported by this RSA instance");
            for (ImportRegistration ir : importedServices) {
                LOG.finest("Closing ImportRegistration " + ir);
                ir.close();
            }
        }
        synchronized (exportedServices) {
            LOG.info("Removing all services exported by this RSA instance");
            for (ExportRegistration er : exportedServices) {
                LOG.finest("Closing ExportRegistration " + er);
                er.close();
            }
        }
    }

}
