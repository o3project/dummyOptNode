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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;

import java.util.List;

/**
 * OpenFlow Pipeline Factory.
 */
public class OpenFlowPipelineFactory implements ChannelPipelineFactory {
  private OpenFlowHandler openFlowHandler;

  /**
   * Constructor.
   * 
   * @param openFlowHandler OpenFlow Handler
   */
  public OpenFlowPipelineFactory(OpenFlowHandler openFlowHandler) {
    super();
    this.openFlowHandler = openFlowHandler;
  }

  /**
   * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
   */
  @Override
  public ChannelPipeline getPipeline() throws Exception {
    ChannelPipeline pipeline = Channels.pipeline();

    pipeline.addLast("decoder", new FrameDecoder() {

      @Override
      protected Object decode(ChannelHandlerContext ctx, Channel channel,
          ChannelBuffer channelBuffer) throws Exception {
        if (!channel.isConnected()) {
          return null;
        }

        OFMessageReader<OFMessage> ofMessageReader = OFFactories.getGenericReader();
        OFMessage ofMessage = ofMessageReader.readFrom(channelBuffer);
        return ofMessage;
      }
    });

    pipeline.addLast("encoder", new OneToOneEncoder() {

      @Override
      protected Object encode(ChannelHandlerContext arg0, Channel arg1,
          Object message) throws Exception {
        if (message instanceof OFMessage) {
          OFMessage ofMessage = (OFMessage) message;
          ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
          ofMessage.writeTo(buffer);
          return buffer;
        } else if (message instanceof List) {
          List<?> messageList = (List<?>) message;
          ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
          for (Object ofMessage : messageList) {
            if (!(ofMessage instanceof OFMessage)) {
              continue;
            }
            ((OFMessage) ofMessage).writeTo(buffer);
          }
          return buffer;
        } else {
          return message;
        }
      }
    });

    pipeline.addLast("handler", openFlowHandler);

    return pipeline;
  }

}
