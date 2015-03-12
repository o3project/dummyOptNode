/* 
* Copyright 2015 FUJITSU LIMITED. 
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
*   http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/

package org.o3project.optsdn.don.openflow;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.o3project.optsdn.don.frame.NeFrame;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;

/**
 * Connect to OpenFlow controller.
 */
public class ConnectorToOpenFlowController {
  public static Logger logger = LoggerFactory.getLogger(ConnectorToOpenFlowController.class);

  private final ClientBootstrap bootstrap;

  /**
   * Constructor.
   */
  public ConnectorToOpenFlowController() {
    bootstrap = new ClientBootstrap(
        new NioClientSocketChannelFactory(
            Executors.newSingleThreadExecutor(),
            Executors.newFixedThreadPool(100)
        )
    );
  }

  /**
   * Connect to OpenFlow controller.
   * 
   * @param hostname        Host name/IP address
   * @param portNumber      TCP port number
   * @param ofVersion       OpenFlow version
   * @param dpid            OpenFlow datapath ID
   * @param neFrame         NE frame to refresh
   * @return ChannelFuture Connection
   * @throws Exception Fail Connection
   */
  public ChannelFuture connectToOpenFlowController(
      String hostname,
      int portNumber,
      OFVersion ofVersion,
      long dpid,
      NeFrame neFrame) throws Exception {

    bootstrap.setOption("tcpNoDelay", true);
    bootstrap.setOption("keepAlive", true);
    bootstrap.setOption("reuseAddress", true);
    bootstrap.setOption("connectTimeoutMillis", 1000 * 10);

    bootstrap.setPipelineFactory(
        new OpenFlowPipelineFactory(
            new OpenFlowHandler(OFFactories.getFactory(ofVersion), dpid, neFrame)
        )
    );

    SocketAddress address = new InetSocketAddress(hostname, portNumber);

    logger.info("Connecting to {}.", address);
    ChannelFuture channelFuture = bootstrap.connect(address);
    channelFuture.awaitUninterruptibly();

    if (!channelFuture.isSuccess()) {
      bootstrap.releaseExternalResources();
      throw new Exception(channelFuture.getCause().getMessage());
    }

    return channelFuture;
  }

  /**
   * Send "ECHO REQUEST" per 10 seconds.
   * 
   * @param channelFuture  Connection
   * @param ofVersion      OpenFlow Version
   * @throws Exception Fail Connection
   */
  public void comfirmOpenFlowControllerLiving(
      ChannelFuture channelFuture, OFVersion ofVersion) throws Exception {

    while (true) {
      Thread.sleep(1000 * 10);

      if (channelFuture.getChannel().isWritable()) {

        channelFuture.getChannel().write(
            OFFactories.getFactory(ofVersion).buildEchoRequest().build()
        );

        channelFuture.addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (!channelFuture.isSuccess()) {
              bootstrap.releaseExternalResources();
              throw new Exception("Living confirmation is failed.");
            }
          }
        });

      } else {
        bootstrap.releaseExternalResources();
        throw new Exception("Living confirmation is failed.");
      }
    }
  }
}
