package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.jmx.mxbeans.CoreHandlerMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base IScopeHandler implementation
 */
public class CoreHandler implements IScopeHandler, CoreHandlerMXBean {

	protected static Logger log = LoggerFactory.getLogger(CoreHandler.class);

	/** {@inheritDoc} */
    public boolean addChildScope(IBasicScope scope) {
		return true;
	}

    /**
     * Connects client to the scope
     *
     * @param conn                 Client conneciton
     * @param scope                Scope
     * @return                     true if client was registred within scope, false otherwise
     */
    public boolean connect(IConnection conn, IScope scope) {
		return connect(conn, scope, null);
	}

    /**
     * Connects client to the scope
     *
     * @param conn                  Client connection
     * @param scope                 Scope
     * @param params                Params passed from client side with connect call
     * @return                      true if client was registered within scope, false otherwise
     */
    public boolean connect(IConnection conn, IScope scope, Object[] params) {
    	// DW urrr this is a slightly strange place to do this, but this is where we create the Client object that consolidates connections
    	// from a single client/FP. Now for more strangeness, I've only been looking at RTMPConnection derivatives, but it's setup() method
    	// seems the only way that the session id is passed in to the newly established connection and this is currently *always* passed in
    	// as null. I'm guessing that either the Flash Player passes some kind of unique id to us that is not being used, or that the idea
    	// originally was to make our own session id, for example by combining client information with the IP address or something like that.
		log.debug("Connect to core handler ?");
		boolean connect = false;

        // Get session id
        String id = conn.getSessionId();
        log.trace("Session id: {}", id);

		// Use client registry from scope the client connected to.
		IScope connectionScope = Red5.getConnectionLocal().getScope();
		log.debug("Connection scope: {}", (connectionScope == null ? "is null" : "not null"));

        // when the scope is null bad things seem to happen, if a null scope is OK then
        // this block will need to be removed - Paul
        if (connectionScope != null) {
            // Get client registry for connection scope
            IClientRegistry clientRegistry = connectionScope.getContext().getClientRegistry();
    		log.debug("Client registry: {}", (clientRegistry == null ? "is null" : "not null"));
    		if (clientRegistry != null) {
				IClient client = null;
    			if (conn.getClient() != null) {
    				// this is an existing connection that is being moved to another scope server-side
    				client = conn.getClient();
    			}
    			else if (clientRegistry.hasClient(id)) {
        			// Use session id as client id. DW this is required for remoting
    				client = clientRegistry.lookupClient(id);
    			} else {
    				// This is a new connection. Create a new client to hold it
    				client = clientRegistry.newClient(params);
    			}
				// Assign connection to client
				conn.initialize(client);
        		// we could checked for banned clients here
        		connect = true;
    		} else {
        		log.error("No client registry was found, clients cannot be looked-up or created");
    		}
        } else {
    		log.error("No connection scope was found");
        }

        return connect;
	}

	/** {@inheritDoc} */
    public void disconnect(IConnection conn, IScope scope) {
		// do nothing here
	}

	/** {@inheritDoc} */
    public boolean join(IClient client, IScope scope) {
		return true;
	}

	/** {@inheritDoc} */
    public void leave(IClient client, IScope scope) {
		// do nothing here
	}

	/** {@inheritDoc} */
    public void removeChildScope(IBasicScope scope) {
		// do nothing here
	}

    /**
     * Remote method invocation
     *
     * @param conn         Connection to invoke method on
     * @param call         Service call context
     * @return             true on success
     */
    public boolean serviceCall(IConnection conn, IServiceCall call) {
		final IContext context = conn.getScope().getContext();
		if (call.getServiceName() != null) {
			context.getServiceInvoker().invoke(call, context);
		} else {
			context.getServiceInvoker().invoke(call, conn.getScope().getHandler());
		}
		return true;
	}

	/** {@inheritDoc} */
    public boolean start(IScope scope) {
		return true;
	}

	/** {@inheritDoc} */
    public void stop(IScope scope) {
		// do nothing here
	}

	/** {@inheritDoc} */
    public boolean handleEvent(IEvent event) {
		return false;
	}

}
