/**
 * Copyright 2014 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.coprocessor.endpoints;

import java.util.ArrayList;

import org.apache.hadoop.hbase.DoNotRetryIOException;
import org.apache.hadoop.hbase.NotServingRegionException;
import org.apache.hadoop.hbase.coprocessor.endpoints.EndpointManager.EndpointInfo;
import org.apache.hadoop.hbase.ipc.thrift.exceptions.ThriftHBaseException;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.regionserver.HRegionServer;

/**
 * An endpoint server.
 */
public class EndpointServer implements IEndpointServer {

  private HRegionServer server;

  public EndpointServer(HRegionServer server) {
    this.server = server;
  }

  @Override
  public byte[] callEndpoint(String epName, String methodName,
      ArrayList<byte[]> params, final byte[] regionName, final byte[] startRow,
      final byte[] stopRow) throws ThriftHBaseException {
    try {
      EndpointInfo ent = EndpointManager.get().getEndpointEntry(epName);
      if (ent == null) {
        // TODO daviddeng make a special exception for this
        throw new DoNotRetryIOException("Endpoint " + epName
            + " does not exists");
      }

      // Create an IEndpoint instance.
      IEndpoint ep = ent.createEndpoint();

      // Set the context.
      ep.setContext(new IEndpointContext() {
        @Override
        public HRegion getRegion() throws NotServingRegionException {
          return EndpointServer.this.server.getRegion(regionName);
        }

        @Override
        public byte[] getStartRow() {
          return startRow;
        }

        @Override
        public byte[] getStopRow() {
          return stopRow;
        }
      });

      // Invoke the specified method with parameters, the return value is
      // encoded and returned.
      return ent.invoke(ep, methodName, params);
    } catch (Exception e) {
      throw new ThriftHBaseException(e);
    }
  }
}
