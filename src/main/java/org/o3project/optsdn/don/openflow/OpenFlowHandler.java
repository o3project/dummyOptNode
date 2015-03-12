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

import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.o3project.optsdn.don.frame.NeFrame;
import org.o3project.optsdn.don.util.Config;
import org.o3project.optsdn.don.util.Constants;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFHello;
import org.projectfloodlight.openflow.protocol.OFHello.Builder;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionWriteActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOduSigid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOduSigtype;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OduSigid;
import org.projectfloodlight.openflow.types.OduSigtype;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OpenFlow Message Handler.
 */
public class OpenFlowHandler extends IdleStateAwareChannelHandler {
  public static Logger logger = LoggerFactory.getLogger(OpenFlowHandler.class);

  private OFFactory factory;
  private long dpid;
  private NeFrame neFrame;

  /**
   * The set for Timer Thread.
   */
  private List<TimerThread> timerThreadSet;

  private Integer flowHighlightTime;

  /**
   * Constructor.
   * 
   * @param factory  OpenFlow factory
   * @param dpid     Datapath ID
   * @param neFrame  NE frame
   */
  public OpenFlowHandler(OFFactory factory, long dpid, NeFrame neFrame) {
    super();
    this.factory = factory;
    this.dpid = dpid;
    this.neFrame = neFrame;
    this.timerThreadSet = new ArrayList<OpenFlowHandler.TimerThread>();

    try {
      flowHighlightTime = Integer.valueOf(Config.getProperty("flowHighlightTime"));
    } catch (Exception e) {
      flowHighlightTime = 10;
    }
  }

  /**
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent event)
      throws Exception {
    logger.info("Channel connected.");
    Builder buildHello = factory.buildHello();
    OFHello build = buildHello.build();
    ctx.getChannel().write(build);
  }

  /**
   * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent event)
      throws Exception {
    logger.error("", event.getCause());
    ctx.getChannel().close();
  }

  /**
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent event)
      throws Exception {
    logger.info("Channel closed.");
  }

  /**
   * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
   */
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent event)
      throws Exception {
    OFMessage ofMessage = (OFMessage) event.getMessage();
    switch (ofMessage.getType()) {
      case HELLO:
        logger.info("Hello message Received.");
        break;
      case ECHO_REQUEST:
        logger.info("Echo Request message Received.");
        ctx.getChannel().write(factory.buildEchoReply()
            .setXid(ofMessage.getXid())
            .build());
        break;
      case FEATURES_REQUEST:
        logger.info("Features Request message Received.");
        ctx.getChannel().write(factory.buildFeaturesReply()
            .setXid(ofMessage.getXid())
            .setDatapathId(DatapathId.of(dpid))
            .build());
        break;
      case STATS_REQUEST:
        logger.info("Multipart Request message Received.");
        ctx.getChannel().write(factory.buildPortDescStatsReply()
            .setXid(ofMessage.getXid())
            .build());
        break;
      case FLOW_MOD:
        logger.info("Flowmod message Received.");
        proceedFlowmod(ofMessage);
        break;
      default:
        break;
    }
  }

  /**
   * Proceed Flowmod massage.
   * (Support only ADD command.)
   * 
   * @param ofMessage The Flowmod message.
   */
  private void proceedFlowmod(OFMessage ofMessage) {
    OFFlowMod ofFlowmod = (OFFlowMod) ofMessage;
    switch (ofFlowmod.getCommand()) {
      case ADD:
        proceedFlowmodAdd(ofFlowmod);
        break;
      case MODIFY:
        break;
      case MODIFY_STRICT:
        break;
      case DELETE:
        break;
      case DELETE_STRICT:
        break;
      default:
        break;
    }
  }

  /**
   * Proceed Flowmod ADD massage.
   * - Draw a link with determined color
   *   (i.e. "flowmodLinkNewColorR/G/B/A" in config.properties file)
   * - Display Flowmod message on Flowmod state
   * 
   * @param ofFlowmod The Flowmod message
   */
  private void proceedFlowmodAdd(OFFlowMod ofFlowmod) {
    OduSigtype setFieldOduSigtype = null;
    OduSigid setFieldOduSigid = null;
    OFPort outputPort = null;

    for (OFInstruction instruction : ofFlowmod.getInstructions()) {
      switch (instruction.getType()) {
        case WRITE_ACTIONS:
          if (!(instruction instanceof OFInstructionWriteActions)) {
            break;
          }
          OFInstructionWriteActions writeActions = (OFInstructionWriteActions) instruction;
          for (OFAction action : writeActions.getActions()) {
            switch (action.getType()) {
              case SET_FIELD:
                if (!(action instanceof OFActionSetField)) {
                  break;
                }
                OFActionSetField actionSetField = (OFActionSetField) action;
                OFOxm<?> field = actionSetField.getField();
                if (field instanceof OFOxmOduSigtype) {
                  OFOxmOduSigtype oxmOduSigid = (OFOxmOduSigtype) field;
                  setFieldOduSigtype = oxmOduSigid.getValue();
                } else if (field instanceof OFOxmOduSigid) {
                  OFOxmOduSigid oxmOduSigid = (OFOxmOduSigid) field;
                  setFieldOduSigid = oxmOduSigid.getValue();
                }
                break;
              case OUTPUT:
                if (!(action instanceof OFActionOutput)) {
                  break;
                }
                OFActionOutput actionOutput = (OFActionOutput) action;
                outputPort = actionOutput.getPort();
                break;
              default:
                break;
            }
          }
          break;
        default:
          break;
      }
    }

    Match match = ofFlowmod.getMatch();
    OFPort matchInPort = match.get(MatchField.IN_PORT);
    OduSigtype matchOduSigtype = match.get(MatchField.ODU_SIGTYPE);
    OduSigid matchOduSigid = match.get(MatchField.ODU_SIGID);

    displayFlowmodMessage(
        matchInPort,
        matchOduSigtype,
        matchOduSigid,
        outputPort,
        setFieldOduSigtype,
        setFieldOduSigid
    );

    List<Integer> edgePorts;
    if (matchInPort != null && outputPort != null) {
      edgePorts = Arrays.asList(
          matchInPort.getPortNumber(),
          outputPort.getPortNumber()
      );

      addLink(edgePorts);
    } else {
      edgePorts = null;
    }

    // Create Timer Thread.
    // (Change link color from flowmodLinkNewColor to flowmodLinkOldColor)
    TimerThread thread = new TimerThread(edgePorts);
    thread.start();

    // Add to Timer Thread Set.
    timerThreadSet.add(thread);
  }

  /**
   * Display Flowmod message (Match, Actions).
   * 
   * @param matchInPort         Match: In port
   * @param matchOduSigtype     Match: ODU SIGTYPE
   * @param matchOduSigid       Match: ODU SIGID
   * @param outputPort          Actions: Output port
   * @param setFieldOduSigtype  Actions: ODU SIGTYPE
   * @param setFieldOduid       Actions: ODU SIGID
   */
  private void displayFlowmodMessage(
      OFPort matchInPort,
      OduSigtype matchOduSigtype,
      OduSigid matchOduSigid,
      OFPort outputPort,
      OduSigtype setFieldOduSigtype,
      OduSigid setFieldOduid) {
    // Match
    String matchInfo = createFlowmodMessageText(
        "in_port=", matchInPort,
        "odu_sigtype=", matchOduSigtype,
        "odu_sigid=", matchOduSigid);

    // Actions
    String actionsInfo = createFlowmodMessageText(
        "output=", outputPort,
        "odu_sigtype=", setFieldOduSigtype,
        "odu_sigid=", setFieldOduid);

    // Display Flowmod message
    String text = "[match] " + matchInfo + "\r\n" 
        + "[actions] " + actionsInfo;
    logger.info("\r\n" 
        + "DPID=" + dpid + "\r\n" 
        + text);
    neFrame.getFlowmodStatusLabel().setText(text);
  }

  /**
   * Create the display text for Flowmod message
   * (example. label="port", value=1 -> display "port=1")
   * 
   * @param portLabel         Port Label
   * @param portValue         Port Value
   * @param oduSigTypeLabel   ODU SIGTYPE label
   * @param oduSigTypeValue   ODU SIGTYPE value
   * @param oduSigidLabel     ODU SIGID label
   * @param oduSigidValue     ODU SIGID value
   * @return The display text for Flowmod message
   */
  private String createFlowmodMessageText(
      String portLabel, OFPort portValue,
      String oduSigTypeLabel, OduSigtype oduSigTypeValue,
      String oduSigidLabel, OduSigid oduSigidValue) {
    List<String> matchInfoList = new ArrayList<String>();
    if (portValue != null) {
      matchInfoList.add(portLabel + portValue.getPortNumber());
    }
    if (oduSigTypeValue != null) {
      matchInfoList.add(oduSigTypeLabel + oduSigTypeValue.getOduSigtypeNumber());
    }
    if (oduSigidValue != null) {
      matchInfoList.add(createOduSigidText(oduSigidLabel, oduSigidValue));
    }
    return StringUtils.join(matchInfoList, ", ");
  }

  /**
   * Create ODU SIGID text.
   * 
   * @param label ODU SIGID Label
   * @param value ODU SIGID Value
   * @return ODU SIGID text
   */
  private String createOduSigidText(String label, OduSigid value) {
    return label + "{TPN=" + value.getTpn()
        + ", tslen=" + value.getTslen()
        + ", tsmap=" + toTsmapBinary(value) + "}";
  }

  /**
   * Get binary value of the tsmap in the ODU SIGID.
   * 
   * @param oduSigid ODU SIGID
   * @return Binary Value
   */
  private String toTsmapBinary(OduSigid oduSigid) {
    short tsmapNum = oduSigid.getTsmap();
    String binary = Integer.toBinaryString(tsmapNum);
    String spacePadding = String.format("%8s", binary);
    String zeroPadding = spacePadding.replace(" ", "0");
    return zeroPadding;
  }

  /**
   * Add a link and refresh NE frame.
   * 
   * @param link Link
   */
  private void addLink(List<Integer> link) {
    neFrame.getLinkListNew().add(link);
    neFrame.repaint();
  }

  /**
   * Timer Thread.
   * - Change link color from flowmodLinkNewColor to flowmodLinkOldColor
   * - Display Waiting message on Flowmod state
   */
  private class TimerThread extends Thread {
    private List<Integer> edgePorts;

    /**
     * Constructor.
     * 
     * @param edgePorts The link to change color
     */
    public TimerThread(List<Integer> edgePorts) {
      super();
      this.edgePorts = edgePorts;
    }

    /**
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      try {
        sleep(1000 * flowHighlightTime);
      } catch (InterruptedException e) {
        logger.error("", e);
      }

      if (edgePorts != null) {
        List<List<Integer>> linkListNew = neFrame.getLinkListNew();
        synchronized (linkListNew) {
          linkListNew.remove(edgePorts);
        }
        neFrame.getLinkSetOld().add(edgePorts);
      }

      // Display Waiting message on Flowmod state,
      // if the other thread is not alive.
      if (!isOtherThreadAlive()) {
        neFrame.getFlowmodStatusLabel().setText(Constants.FLOWMOD_INFO_TEXT_WAITING);
      }

      neFrame.repaint();

      timerThreadSet.remove(this);
    }

    /**
     * Check if the other thread is alive.
     *
     * @return true: if other thread is alive
     *         false: otherwise
     */
    private boolean isOtherThreadAlive() {
      for (TimerThread flowmodTask : timerThreadSet) {
        if (flowmodTask.equals(this)) {
          continue;
        }
        if (flowmodTask.isAlive()) {
          return true;
        }
      }
      return false;
    }
  }
}
