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

package org.o3project.optsdn.don.frame;

import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.netty.channel.ChannelFuture;
import org.o3project.optsdn.don.nwc.Port;
import org.o3project.optsdn.don.openflow.ConnectorToOpenFlowController;
import org.o3project.optsdn.don.util.AntiAliasingPanel;
import org.o3project.optsdn.don.util.Config;
import org.o3project.optsdn.don.util.Constants;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 * NE frame.
 */
public class NeFrame extends JFrame {
  public static Logger logger = LoggerFactory.getLogger(NeFrame.class);

  private static final int TABLE_HEIGHT = 100;
  private static final String OFPORT_NOTFOUND = "E";

  private String neTitle;
  private List<Port> portList;
  private Map<Integer, Port> portMap;
  private List<List<Port>> linkList;
  private List<List<Port>> omsConnectionInfoList;
  private String ptId;
  private long dpid;
  private JButton displayNeStatusButton;
  private JButton connectButton;

  /* Flowmod status label. */
  private JTextArea flowmodStatusLabel;

  /*
   * The List for new links.
   * (Remove a link from the List after 10 seconds.)
   */
  private List<List<Integer>> linkListNew;

  /*
   * The Set for old links.
   * (Removed link from the Set of new links)
   */
  private Set<List<Integer>> linkSetOld;

  private final Color ptColor;
  private final Color neOduColor;
  private final Color neOchColor;
  private final Color oduXcColor;
  private final Color lambdaSwColor;
  private final Color xcColor;
  private final Color flowmodLinkOldColor;
  private final Color flowmodLinkNewColor;

  /* Information Model ID Columns. */
  private enum InformationModelIdCols {
    OF_PORT("Port"),
    INFORMATION_MODEL_ID("Information Model ID");

    private String name;

    private InformationModelIdCols(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  /* Information Model ID Column names. */
  private static List<String> INFORMATION_MODEL_ID_COLNAME_LIST;

  static {
    INFORMATION_MODEL_ID_COLNAME_LIST = new ArrayList<String>();
    for (InformationModelIdCols col : InformationModelIdCols.values()) {
      INFORMATION_MODEL_ID_COLNAME_LIST.add(col.getName());
    }
  }

  /* OMS connection informations Columns. */
  private enum OmsConnectionInfoCols {
    OF_PORT("Port"),
    CONNECTED_TO("Connected to");

    private String name;

    private OmsConnectionInfoCols(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  /* Information Model ID Column names. */
  private static List<String> OMS_CONNECTION_INFO_COLNAME_LIST;

  static {
    OMS_CONNECTION_INFO_COLNAME_LIST = new ArrayList<String>();
    for (OmsConnectionInfoCols col : OmsConnectionInfoCols.values()) {
      OMS_CONNECTION_INFO_COLNAME_LIST.add(col.getName());
    }
  }

  /**
   * Constructor.
   * 
   * @param neTitle                NE frame title
   * @param portList               Ports
   * @param linkList               Links
   * @param omsConnectionInfoList  OMS connections
   * @param ptId                   PT ID
   * @param dpid                   Datapath ID
   * @param displayNeStatusButton  NE button
   */
  public NeFrame(
      String neTitle,
      List<Port> portList,
      List<List<Port>> linkList,
      List<List<Port>> omsConnectionInfoList,
      String ptId,
      long dpid,
      JButton displayNeStatusButton) {

    this.neTitle = neTitle;

    this.portList = portList;
    portMap = new HashMap<Integer, Port>();
    for (Port port : portList) {
      Integer openFlowPortId = port.getOpenFlowPortId();
      if (openFlowPortId == null) {
        continue;
      }
      portMap.put(openFlowPortId, port);
    }

    this.flowmodStatusLabel = new JTextArea();
    this.linkListNew = new ArrayList<List<Integer>>();
    this.linkSetOld = new HashSet<List<Integer>>();

    this.linkList = linkList;
    this.omsConnectionInfoList = omsConnectionInfoList;
    this.ptId = ptId;
    this.dpid = dpid;
    this.displayNeStatusButton = displayNeStatusButton;

    ptColor = getPropertyColor(
        "ptColorR", "ptColorG", "ptColorB", "ptColorA",
        new Color(204, 236, 255, 255));

    neOduColor = getPropertyColor(
        "neOduColorR", "neOduColorG", "neOduColorB", "neOduColorA",
        new Color(204, 255, 204, 255));

    neOchColor = getPropertyColor(
        "neOchColorR", "neOchColorG", "neOchColorB", "neOchColorA",
        new Color(255, 230, 153, 255));

    oduXcColor = getPropertyColor(
        "oduXcColorR", "oduXcColorG", "oduXcColorB", "oduXcColorA",
        new Color(255, 255, 255, 255));

    lambdaSwColor = getPropertyColor(
        "lambdaSwColorR", "lambdaSwColorG", "lambdaSwColorB", "lambdaSwColorA",
        new Color(255, 255, 255, 255));

    xcColor = getPropertyColor(
        "xcColorR", "xcColorG", "xcColorB", "xcColorA",
        new Color(0, 0, 255, 255));

    flowmodLinkOldColor = getPropertyColor(
        "flowmodLinkOldColorR",
        "flowmodLinkOldColorG",
        "flowmodLinkOldColorB",
        "flowmodLinkOldColorA",
        new Color(128, 128, 128, 126));

    flowmodLinkNewColor = getPropertyColor(
        "flowmodLinkNewColorR",
        "flowmodLinkNewColorG",
        "flowmodLinkNewColorB",
        "flowmodLinkNewColorA",
        new Color(255, 0, 0, 255));

    createNeFrame();
  }

  /**
   * Get Flowmod status label.
   * 
   * @return the Flowmod status label
   */
  public JTextArea getFlowmodStatusLabel() {
    return flowmodStatusLabel;
  }

  /**
   * Get the List for new links.
   * 
   * @return The List for new links
   */
  public List<List<Integer>> getLinkListNew() {
    return linkListNew;
  }

  /**
   * Get the Set for old links.
   * 
   * @return The Set for old links
   */
  public Set<List<Integer>> getLinkSetOld() {
    return linkSetOld;
  }

  /**
   * Get property color.
   * 
   * @param keyR         R Property Key
   * @param keyG         G Property Key
   * @param keyB         B Property Key
   * @param keyA         A Property Key
   * @param defaultColor The Default Color
   * @return The Property Color
   */
  private Color getPropertyColor(
      String keyR, String keyG, String keyB, String keyA,
      Color defaultColor) {

    String propertyR = Config.getProperty(keyR);
    String propertyG = Config.getProperty(keyG);
    String propertyB = Config.getProperty(keyB);
    String propertyA = Config.getProperty(keyA);

    if (NumberUtils.isNumber(propertyR)
        && NumberUtils.isNumber(propertyG)
        && NumberUtils.isNumber(propertyB)
        && NumberUtils.isNumber(propertyA)) {

      return new Color(
          Integer.valueOf(propertyR),
          Integer.valueOf(propertyG),
          Integer.valueOf(propertyB),
          Integer.valueOf(propertyA));

    } else {
      return defaultColor;
    }
  }

  /**
   * Create a NE frame.
   */
  private void createNeFrame() {
    setTitle(neTitle);
    add(createNePanel());
    pack();
  }

  /**
   * Create a NE frame panel.
   * 
   * @return The NE frame panel
   */
  private JPanel createNePanel() {
    JPanel nePanel = new JPanel();
    nePanel.setBackground(Color.WHITE);
    nePanel.setLayout(new BorderLayout());

    JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    centerPanel.add(createDrawingNePane());
    centerPanel.add(createOmsConnectionInfoPane());
    centerPanel.add(createInformationModelIdListPane(portList));

    nePanel.add(centerPanel, BorderLayout.CENTER);
    nePanel.add(createConnectButtonPanel(), BorderLayout.SOUTH);

    return nePanel;
  }

  /**
   * Create a pane that draws initial NE display.
   * 
   * @return The pane that draws initial NE display
   */
  private JScrollPane createDrawingNePane() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(createFlowmodStatusPanel(), BorderLayout.NORTH);
    panel.add(new DrawingNePanel(), BorderLayout.CENTER);

    JScrollPane pane = new JScrollPane();
    pane.getViewport().setView(panel);
    return pane;
  }

  /**
   * Create a panel that displays Flowmod message.
   * 
   * @return The panel that displays Flowmod message
   */
  private JPanel createFlowmodStatusPanel() {
    JPanel flowmodStatusPanel = new JPanel();
    flowmodStatusPanel.setBackground(Color.WHITE);

    // Set maximum text size for the Flowmod message
    flowmodStatusLabel.setText(
        "[match] in_port=65509, odu_sigtype=11, odu_sigid={TPN=1, tslen=8, tsmap=11111111}\n"
        + "[actions] output=65509, odu_sigtype=11, odu_sigid={TPN=1, tslen=8, tsmap=11111111}");
    flowmodStatusPanel.add(flowmodStatusLabel);
    flowmodStatusPanel.setPreferredSize(flowmodStatusPanel.getPreferredSize());

    // Set default text for the Flowmod message
    flowmodStatusLabel.setText(Constants.FLOWMOD_INFO_TEXT_DEFAULT);
    return flowmodStatusPanel;
  }

  /**
   * Create a pane that displays OMS connection information.
   * 
   * @return The pane that displays OMS connection information
   */
  private JScrollPane createOmsConnectionInfoPane() {
    Object[] header = OMS_CONNECTION_INFO_COLNAME_LIST.toArray();

    String[][] rowDataList = new String[omsConnectionInfoList.size()][header.length];
    for (int i = 0; i < rowDataList.length; i++) {
      List<Port> omsConnectionInfo = omsConnectionInfoList.get(i);

      Port port = omsConnectionInfo.get(0);
      int ofPortIndex = OMS_CONNECTION_INFO_COLNAME_LIST.indexOf(
          OmsConnectionInfoCols.OF_PORT.getName());
      Integer openFlowPortId1 = port.getOpenFlowPortId();
      String ofPortValue1;
      if (openFlowPortId1 == null) {
        ofPortValue1 = OFPORT_NOTFOUND;
      } else {
        ofPortValue1 = String.valueOf(openFlowPortId1);
      }
      rowDataList[i][ofPortIndex] = ofPortValue1;

      Port port2 = omsConnectionInfo.get(1);
      int connectedToIndex = OMS_CONNECTION_INFO_COLNAME_LIST.indexOf(
          OmsConnectionInfoCols.CONNECTED_TO.getName());
      Integer openFlowPortId2 = port2.getOpenFlowPortId();
      String ofPortValue2;
      if (openFlowPortId2 == null) {
        ofPortValue2 = OFPORT_NOTFOUND;
      } else {
        ofPortValue2 = String.valueOf(openFlowPortId2);
      }
      rowDataList[i][connectedToIndex] = port2.getNeId() + ", " + ofPortValue2;
    }

    JTable connectedTable = new JTable(rowDataList, header);

    connectedTable.getTableHeader().setBackground(Color.WHITE);

    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    renderer.setHorizontalAlignment(SwingConstants.CENTER);
    for (int i = 0; i < header.length; i++) {
      TableColumn column = connectedTable.getColumnModel().getColumn(i);
      column.setCellRenderer(renderer);
    }

    JScrollPane connectedPane = new JScrollPane();
    connectedPane.getViewport().setView(connectedTable);
    connectedPane.setPreferredSize(new Dimension(
        connectedPane.getPreferredSize().width, TABLE_HEIGHT));
    return connectedPane;
  }

  /**
   * Create a pane that displays Information Model ID list.
   * 
   * @param portList Ports
   * @return The pane that displays Information Model ID list
   */
  private JScrollPane createInformationModelIdListPane(List<Port> portList) {
    Collections.sort(portList, new PortComparator());

    Object[] columnNames = INFORMATION_MODEL_ID_COLNAME_LIST.toArray();
    DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
    for (Port port : portList) {
      Port[] column = { port, port };
      tableModel.addRow(column);
    }

    JTable informationModeldTable = new JTable(tableModel);
    informationModeldTable.getTableHeader().setBackground(Color.WHITE);

    DefaultTableCellRenderer renderer = new InformationModelIdRenderer();
    renderer.setHorizontalAlignment(SwingConstants.CENTER);
    informationModeldTable.setDefaultRenderer(Object.class, renderer);

    JScrollPane informationModelIdPane = new JScrollPane();
    informationModelIdPane.getViewport().setView(informationModeldTable);
    informationModelIdPane.setPreferredSize(new Dimension(
        informationModelIdPane.getPreferredSize().width, TABLE_HEIGHT));

    return informationModelIdPane;
  }

  /**
   * The comparator for sorting layer, port type, port ID.
   */
  private class PortComparator implements Comparator<Port> {
    @Override
    public int compare(Port port1, Port port2) {
      if (port1.getLayerPriority() < port2.getLayerPriority()) {
        return 1;
      } else if (port1.getLayerPriority() > port2.getLayerPriority()) {
        return -1;
      }
      if (port1.getPortPriority() < port2.getPortPriority()) {
        return 1;
      } else if (port1.getPortPriority() > port2.getPortPriority()) {
        return -1;
      }
      if (port1.getPortId() > port2.getPortId()) {
        return 1;
      } else if (port1.getPortId() < port2.getPortId()) {
        return -1;
      }
      return 0;
    }
  }

  /**
   * Create a panel that displays Connect button.
   * 
   * @return The panel that displays Connect button
   */
  private JPanel createConnectButtonPanel() {
    connectButton = new JButton("Connect");
    connectButton.addActionListener(new ConnectToOpenFlowControllerListener(this));
    connectButton.setMaximumSize(new Dimension(
        Short.MAX_VALUE,
        connectButton.getMaximumSize().height));

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    buttonPanel.add(connectButton);
    return buttonPanel;
  }

  /**
   * The class that draws NE panel with the received Flowmod message.
   */
  private class DrawingNePanel extends JPanel {
    private static final int PT_HEIGHT = 30;
    private static final int GAP = 20;
    private static final int LEGEND_GAP = 15;
    private static final int TOP_X = 0;
    private static final int TOP_Y = 0;
    private static final int NE_TOP_Y = TOP_Y + PT_HEIGHT + GAP;
    private static final int PORT_D = 16;
    private static final int PORT_R = PORT_D / 2;
    private static final int XC_HEIGHT = 24;
    private static final int NE_ODU_HEIGHT = XC_HEIGHT + PORT_D * 2 + GAP * 3 + GAP / 2;
    private static final int NE_OCH_HEIGHT = XC_HEIGHT + PORT_D + PORT_R + GAP * 2 + GAP / 2;
    private static final int NE_HEIGHT = NE_ODU_HEIGHT + NE_OCH_HEIGHT;
    private static final int LEGEND_STRING_WIDTH = 22;
    private static final int LEGEND_WIDTH =
        LEGEND_GAP + PORT_D + LEGEND_GAP + LEGEND_STRING_WIDTH + LEGEND_GAP;
    private static final int LEGEND_HEIGHT = PORT_D * 2 + LEGEND_GAP * 3;
    private static final int ODU_XC_STRING_WIDTH = 46;
    private static final int LAMBDA_XC_STRING_WIDTH = 70;
    private static final int CHAR_WIDTH = 4;
    private static final int CHAR_HEIGHT = 10;
    private static final int OCH_CTP_TO_XC_LENGTH = PORT_R + GAP + XC_HEIGHT / 2;
    private static final float LINK_WIDTH = 2.0f;
    private static final String X_LABEL = "X";

    private static final int PT_TTP_Y = TOP_Y + PT_HEIGHT - PORT_R;
    private static final int ODU_TTP_Y = NE_TOP_Y + GAP;
    private static final int ODU_XC_Y = ODU_TTP_Y + PORT_D + GAP;
    private static final int ODU_CTP_Y = ODU_TTP_Y + PORT_D + GAP + XC_HEIGHT + GAP;
    private static final int OCH_TTP_Y = ODU_CTP_Y + PORT_D + GAP;
    private static final int LAMBDA_SW_Y = OCH_TTP_Y + PORT_D + GAP;
    private static final int OCH_CTP_Y = LAMBDA_SW_Y + XC_HEIGHT + GAP;
    private static final int NE_Y = TOP_Y + PT_HEIGHT + GAP;
    private static final int XC_X = TOP_X + GAP;
    private static final int LEGEND_TTP_Y = TOP_Y + LEGEND_GAP;
    private static final int LEGEND_CTP_Y = LEGEND_TTP_Y + PORT_D + LEGEND_GAP;

    private final int neWidth;
    private final int xcWidth;
    private Map<Port, Pos> portPosMap;

    /**
     * Constructor.
     */
    public DrawingNePanel() {
      super();

      xcWidth = calcNeWidth();
      neWidth = xcWidth + GAP * 2;

      initPortsPos();

      Dimension dimension = new Dimension(
          GAP + neWidth + GAP + LEGEND_WIDTH + GAP,
          GAP + PT_HEIGHT + GAP + NE_HEIGHT + GAP
      );

      JPanel portPanel = new PortPanel();
      portPanel.setSize(dimension);
      portPanel.setOpaque(false);

      BasePanel basePanel = new BasePanel();
      basePanel.setSize(dimension);
      basePanel.setOpaque(false);

      JLayeredPane pane = new JLayeredPane();
      pane.add(portPanel);
      pane.add(basePanel);
      pane.setPreferredSize(dimension);
      this.add(pane);

      this.setPreferredSize(dimension);
      this.setBackground(Color.WHITE);
    }

    /**
     * Calculate the width of NE display.
     * The width is increased if the number of ports is more than 10.
     * 
     * @return The width of NE display
     */
    private int calcNeWidth() {
      int[] portCounts = {
          Port.searchPorts(portList, null, Constants.ETHER, Constants.TTP).size(),
          Port.searchPorts(portList, null, Constants.ODU, Constants.TTP).size(),
          Port.searchPorts(portList, null, Constants.ODU, Constants.CTP).size(),
          Port.searchPorts(portList, null, Constants.OCH, Constants.TTP).size(),
          Port.searchPorts(portList, null, Constants.OCH, Constants.CTP).size(),
      };

      Arrays.sort(portCounts);
      int maxPortCount = portCounts[portCounts.length - 1];
      final int minimumPortCount = 10;
      int portCount;
      if (maxPortCount > minimumPortCount) {
        portCount = maxPortCount;
      } else {
        portCount = minimumPortCount;
      }
      return PORT_D * portCount;
    }

    /**
    * Initialize the positions of ports in NE display.
    */
    private void initPortsPos() {
      portPosMap = new HashMap<Port, Pos>();
      calculatePortsPos(Constants.ETHER, Constants.TTP, TOP_X, PT_TTP_Y);
      calculatePortsPos(Constants.ODU, Constants.TTP, TOP_X, ODU_TTP_Y);
      calculatePortsPos(Constants.ODU, Constants.CTP, TOP_X, ODU_CTP_Y);
      calculatePortsPos(Constants.OCH, Constants.TTP, TOP_X, OCH_TTP_Y);
      calculatePortsPos(Constants.OCH, Constants.CTP, TOP_X, OCH_CTP_Y);
    }

    /**
     * Initialize the ports' positions of the layer by port type.
     * Notes:
     *   - Ports' positions are centering.
     *   - Sort ascending by port ID.
     *   - Connected ports are sort adjacently.
     * 
     * @param layer The Layer
     * @param portType The Port Type
     * @param srcX The X-coordinate of the layer in NE panel
     * @param srcY The Y-coordinate of the layer in NE panel
     */
    private void calculatePortsPos(String layer, String portType, int srcX, int srcY) {
      List<Port> targetPortList = Port.searchPorts(portList, null, layer, portType);

      // Sort ascending by OpenFlow port ID
      Collections.sort(targetPortList, new PortIdComparator());
      // Connected ports are sort adjacently
      targetPortList = createUncrossedConnectingPorts(targetPortList);

      srcX += calculatePortStartX(targetPortList.size());
      for (Port port : targetPortList) {
        portPosMap.put(port, new Pos(srcX, srcY));
        srcX += PORT_D;
      }
    }

    /**
     * The Comparator for sorting port ID.
     */
    private class PortIdComparator implements Comparator<Port> {
      @Override
      public int compare(Port port1, Port port2) {
        Integer portId1 = port1.getOpenFlowPortId();
        Integer portId2 = port2.getOpenFlowPortId();
        if (portId1 == null && portId2 == null) {
          return 0;
        } else if (portId1 != null && portId2 == null) {
          return 1;
        } else if (portId1 == null && portId2 != null) {
          return -1;
        } else if (portId1 > portId2) {
          return 1;
        } else if (portId2 > portId1) {
          return -1;
        } else {
          return 0;
        }
      }
    }

    /**
     * Create a port list that sorts connected ports adjacently (uncrossed connecting).
     * 
     * @param portList The Port List
     * @return The port list that sorts connected ports adjacently
     */
    private List<Port> createUncrossedConnectingPorts(List<Port> portList) {
      List<Port> sortingList = new ArrayList<Port>();
      // Sorts connected ports adjacently
      for (Port port : portList) {
        // Skip if already exists
        if (sortingList.contains(port)) {
          continue;
        }
        // add self
        sortingList.add(port);

        // add the connected port
        // if it is the same port type in the same layer
        for (List<Port> link : linkList) {
          Port port1 = link.get(0);
          Port port2 = link.get(1);

          if (!port1.getLayer().equals(port2.getLayer())
              || !port1.getPortType().equals(port2.getPortType())) {
            continue;
          }

          if (port.equals(port1)) {
            sortingList.add(port2);
          } else if (port.equals(port2)) {
            sortingList.add(port1);
          }
        }
      }
      return sortingList;
    }

    /**
     * The class that draws BasePanel (without ports) under the PortPanel.
     * (NE panel is constructed with a BasePanel and a PortPanel)
     */
    private class BasePanel extends AntiAliasingPanel {
      @Override
      protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        // PT
        if (ptId != null) {
          drawfillRect(graphics, neWidth, PT_HEIGHT, ptColor, TOP_X, TOP_Y);
          graphics.drawString(ptId,
              TOP_X + neWidth / 2 - CHAR_WIDTH * ptId.length(),
              TOP_Y + PT_HEIGHT / 2 + CHAR_HEIGHT / 2);
        }

        // NE
        drawfillRect(graphics, neWidth, NE_ODU_HEIGHT, neOduColor, TOP_X, NE_Y);
        drawfillRect(graphics, neWidth, NE_OCH_HEIGHT, neOchColor, TOP_X, NE_Y + NE_ODU_HEIGHT);

        // ODU XC
        drawfillRoundRect(
            graphics, xcWidth, XC_HEIGHT,
            oduXcColor,
            XC_X, ODU_XC_Y);

        String oduXcLabel = Config.getProperty("oduXcLabel");
        if (oduXcLabel == null) {
          oduXcLabel = "";
        }
        graphics.drawString(
            oduXcLabel,
            XC_X + xcWidth / 2 - ODU_XC_STRING_WIDTH / 2,
            ODU_XC_Y + XC_HEIGHT / 2 + CHAR_HEIGHT / 2);

        // Lambda SW
        drawfillRoundRect(
            graphics, xcWidth, XC_HEIGHT,
            lambdaSwColor,
            XC_X, LAMBDA_SW_Y);

        String lambdaSwLabel = Config.getProperty("lambdaSwLabel");
        if (lambdaSwLabel == null) {
          lambdaSwLabel = "";
        }
        graphics.drawString(
            lambdaSwLabel,
            XC_X + xcWidth / 2 - LAMBDA_XC_STRING_WIDTH / 2,
            LAMBDA_SW_Y + XC_HEIGHT / 2 + CHAR_HEIGHT / 2);

        int legendX = TOP_X + neWidth + 10;

        // Legend
        graphics.drawRect(legendX, TOP_Y, LEGEND_WIDTH, LEGEND_HEIGHT);

        // Legend - TTP
        legendX += LEGEND_GAP;
        drawBlackPort(graphics, X_LABEL, legendX, LEGEND_TTP_Y);
        graphics.drawString(Constants.TTP,
            legendX + PORT_D + LEGEND_GAP,
            LEGEND_TTP_Y + PORT_R + CHAR_HEIGHT / 2);

        // Legend - CTP
        drawWhitePort(graphics, X_LABEL, legendX, LEGEND_CTP_Y);
        graphics.drawString(Constants.CTP,
            legendX + PORT_D + LEGEND_GAP,
            LEGEND_CTP_Y + PORT_R + CHAR_HEIGHT / 2);

        // Link
        for (List<Port> link : linkList) {
          drawLink(graphics, link, xcColor);
        }

        // draws old links
        for (List<Integer> link : linkSetOld) {
          drawLinkByPortNumber(graphics, link, flowmodLinkOldColor);
        }

        // draws new links
        for (List<Integer> link : linkListNew) {
          drawLinkByPortNumber(graphics, link, flowmodLinkNewColor);
        }
      }
    }

    /**
     * The class that draws PortPanel upon the BasePanel.
     * (NE panel is constructed with a BasePanel and a PortPanel)
     */
    private class PortPanel extends AntiAliasingPanel {
      @Override
      protected void paintComponent(Graphics graphics) {
        drawPortsToLayer(graphics, Constants.ETHER, Constants.TTP);
        drawPortsToLayer(graphics, Constants.ODU, Constants.TTP);
        drawPortsToLayer(graphics, Constants.ODU, Constants.CTP);
        drawPortsToLayer(graphics, Constants.OCH, Constants.TTP);
        drawPortsToLayer(graphics, Constants.OCH, Constants.CTP);
      }
    }

    /**
     * Draws a black port.
     * 
     * @param graphics  The Graphics
     * @param portId    The Port ID
     * @param posX      The X-coordinate
     * @param posY      The Y-coordinate
     */
    private void drawBlackPort(Graphics graphics, String portId, int posX, int posY) {
      graphics.setColor(Color.BLACK);
      graphics.fillRoundRect(posX, posY, PORT_D, PORT_D, PORT_D, PORT_D);
      graphics.setColor(Color.WHITE);
      drawStringOnPort(graphics, portId, posX, posY);
      graphics.setColor(Color.BLACK);
    }

    /**
     * Draws a white port.
     * 
     * @param graphics  The Graphics
     * @param portId    The Port ID
     * @param posX      The X-coordinate
     * @param posY      The Y-coordinate
     */
    private void drawWhitePort(Graphics graphics, String portId, int posX, int posY) {
      graphics.setColor(Color.WHITE);
      graphics.fillRoundRect(posX, posY, PORT_D, PORT_D, PORT_D, PORT_D);
      graphics.setColor(Color.BLACK);
      graphics.drawRoundRect(posX, posY, PORT_D, PORT_D, PORT_D, PORT_D);
      drawStringOnPort(graphics, portId, posX, posY);
    }

    /**
     * Draw a port ID on the port.
     * 
     * @param graphics  The Graphics
     * @param portId    The Port ID
     * @param posX      The X-coordinate
     * @param posY      The Y-coordinate
     */
    private void drawStringOnPort(Graphics graphics, String portId, int posX, int posY) {
      graphics.drawString(
          portId,
          posX + PORT_R - CHAR_WIDTH * portId.length(),
          posY + PORT_R + CHAR_HEIGHT / 2
      );
    }

    /**
     * Draw a rectangle with filled color.
     * 
     * @param graphics   The Graphics
     * @param width      The Width
     * @param height     The Height
     * @param fillColor  The Color
     * @param srcX       The X-coordinate
     * @param srcY       The Y-coordinate
     */
    private void drawfillRect(
        Graphics graphics, int width, int height, Color fillColor, int srcX, int srcY) {
      graphics.setColor(fillColor);
      graphics.fillRect(srcX, srcY, width, height);
      graphics.setColor(Color.BLACK);
      graphics.drawRect(srcX, srcY, width, height);
    }

    /**
     * Draw a round corner rectangle with filled color.
     * 
     * @param graphics   The Graphics
     * @param width      The Width
     * @param height     The Height
     * @param fillColor  The Color
     * @param srcX       The X-coordinate
     * @param srcY       The Y-coordinate
     */
    private void drawfillRoundRect(
        Graphics graphics, int width, int height, Color fillColor, int srcX, int srcY) {
      graphics.setColor(fillColor);
      graphics.fillRoundRect(srcX, srcY, width, height, width / 5, height / 5);
      graphics.setColor(Color.BLACK);
      graphics.drawRoundRect(srcX, srcY, width, height, width / 5, height / 5);
    }

    /**
     * Draw a color link by port ID.
     * 
     * @param graphics The Graphics
     * @param link     The list with paired port IDs
     * @param color    The Color
     */
    private void drawLinkByPortNumber(Graphics graphics, List<Integer> link, Color color) {
      Port port1 = portMap.get(link.get(0));
      Port port2 = portMap.get(link.get(1));
      drawLink(graphics, port1, port2, color);
    }

    /**
     * Draw a color link by port.
     * 
     * @param graphics The Graphics
     * @param link     The list with paired ports
     * @param color    The Color
     */
    private void drawLink(Graphics graphics, List<Port> link, Color color) {
      Port port1 = link.get(0);
      Port port2 = link.get(1);
      drawLink(graphics, port1, port2, color);
    }

    /**
     * Draw a color link by specified ports.
     * 
     * @param graphics The Graphics
     * @param port1    The Port1
     * @param port2    The Port2
     * @param color    The Color
     */
    private void drawLink(Graphics graphics, Port port1, Port port2, Color color) {
      Pos port1Pos = portPosMap.get(port1);
      Pos port2Pos = portPosMap.get(port2);

      if (port1Pos == null) {
        logger.warn("Port is not found (" + port1 + ").");
        return;
      }

      if (port2Pos == null) {
        logger.warn("Port is not found (" + port2 + ").");
        return;
      }

      graphics.setColor(color);

      int x1 = port1Pos.posX + PORT_R;
      int y1 = port1Pos.posY + PORT_R;
      int x2 = port2Pos.posX + PORT_R;
      int y2 = port2Pos.posY + PORT_R;

      BasicStroke stroke = new BasicStroke(LINK_WIDTH);
      Graphics2D graphics2d = (Graphics2D) graphics;
      Stroke currentStroke = graphics2d.getStroke();
      graphics2d.setStroke(stroke);

      if (port1.getPortType().equals(Constants.CTP)
          && port2.getPortType().equals(Constants.CTP)) {
        drawLinkCutThrough(graphics, x1, y1, x2, y2);
      } else {
        graphics.drawLine(x1, y1, x2, y2);
      }

      graphics2d.setStroke(currentStroke);
      graphics.setColor(Color.BLACK);
    }

    /**
     * Draw a cut through link.
     * 
     * @param graphics The Graphics
     * @param x1       The X-coordinate of port1
     * @param y1       The Y-coordinate of port1
     * @param x2       The X-coordinate of port2
     * @param y2       The Y-coordinate of port2
     */
    private void drawLinkCutThrough(Graphics graphics, int x1, int y1, int x2, int y2) {
      graphics.drawLine(x1, y1, x1, y1 - OCH_CTP_TO_XC_LENGTH);
      graphics.drawLine(x1, y1 - OCH_CTP_TO_XC_LENGTH, x2, y2 - OCH_CTP_TO_XC_LENGTH);
      graphics.drawLine(x2, y2 - OCH_CTP_TO_XC_LENGTH, x2, y2);
    }

    /**
     * Draw ports of the layer by port type.
     *   TTP: black
     *   CTP: white
     * 
     * @param graphics  The Graphics
     * @param layer     The Layer
     * @param portType  The Port Type
     */
    private void drawPortsToLayer(Graphics graphics, String layer, String portType) {
      List<Port> searchedPortList = Port.searchPorts(portList, null, layer, portType);

      for (Port port : searchedPortList) {
        Pos pos = portPosMap.get(port);
        if (pos == null) {
          logger.warn("Port is not found (" + port + ")");
          continue;
        }

        String portId;
        if (port.getLayer().equals(Constants.ETHER)) {
          portId = "";
        } else {
          Integer ofPortId = port.getOpenFlowPortId();
          if (ofPortId != null) {
            portId = String.valueOf(ofPortId);
          } else {
            portId = OFPORT_NOTFOUND;
          }
        }

        if (portType.equals(Constants.TTP)) {
          drawBlackPort(graphics, portId, pos.posX, pos.posY);
        } else {
          drawWhitePort(graphics, portId, pos.posX, pos.posY);
        }
      }
    }

    /**
     * Calculate X-coordinate for centering ports.
     * 
     * @param portNum The Port Number
     * @return The X-coordinate for centering ports
     */
    private int calculatePortStartX(int portNum) {
      return neWidth / 2 - PORT_D * portNum / 2;
    }

    /**
     * The coordinate class.
     */
    private class Pos {
      private final int posX;
      private final int posY;

      /**
       * Constructor.
       * 
       * @param posX The X-coordinate
       * @param posY The Y-coordinate
       */
      private Pos(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
      }
    }
  }

  /**
   * The class for rendering the Information Model ID table.
   */
  private class InformationModelIdRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      Port port = (Port) value;

      if (column == INFORMATION_MODEL_ID_COLNAME_LIST.indexOf(
          InformationModelIdCols.OF_PORT.getName())) {
        if (port.getLayer().equals(Constants.ETHER)) {
          setText(String.valueOf("-"));
        } else {
          Integer openFlowPortId = port.getOpenFlowPortId();
          if (openFlowPortId == null) {
            setText(String.valueOf(OFPORT_NOTFOUND));
          } else {
            setText(String.valueOf(openFlowPortId));
          }
        }
      } else if (column == INFORMATION_MODEL_ID_COLNAME_LIST.indexOf(
          InformationModelIdCols.INFORMATION_MODEL_ID.getName())) {
        setText(port.getInformationModelId());
      }

      switch (port.getLayer()) {
        case Constants.ETHER:
          setBackground(ptColor);
          break;
        case Constants.ODU:
          setBackground(neOduColor);
          break;
        case Constants.OCH:
          setBackground(neOchColor);
          break;
        default:
          break;
      }
      if (isSelected) {
        setBackground(table.getSelectionBackground());
      }

      return this;
    }
  }

  /**
   * The class for Connect button Action Listener.
   */
  private class ConnectToOpenFlowControllerListener implements ActionListener {
    private NeFrame neFrame;

    /**
     * Constructor.
     * 
     * @param neFrame The NE frame
     */
    public ConnectToOpenFlowControllerListener(NeFrame neFrame) {
      super();
      this.neFrame = neFrame;
    }

    /**
     * Connect to OpenFlow controller if connect button is clicked.
     * If succeeded, show success message and start to confirm living state.
     * If failed, show error message.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          OFVersion of13 = OFVersion.OF_13;
          ChannelFuture channelFuture = null;
          String orgText = displayNeStatusButton.getText();

          connectButton.setEnabled(false);
          connectButton.setText("Connecting...");

          String ofcHostname = Config.getProperty("ofcHostname");
          if (ofcHostname == null) {
            JOptionPane.showMessageDialog(neFrame, 
                "ofcHostname[" + ofcHostname + "] setting is invalid.",
                "Failed", JOptionPane.ERROR_MESSAGE);
            return;
          }

          String ofcPortNumberString = Config.getProperty("ofcPortNumber");
          Integer ofcPortNumber;
          try {
            ofcPortNumber = Integer.valueOf(ofcPortNumberString);
          } catch (NumberFormatException e) {
            logger.error("", e);
            JOptionPane.showMessageDialog(neFrame,
                "ofcPortNumber[" + ofcPortNumberString + "] setting is invalid.",
                "Failed", JOptionPane.ERROR_MESSAGE);
            return;
          }

          ConnectorToOpenFlowController connector = new ConnectorToOpenFlowController();
          try {
            channelFuture = connector.connectToOpenFlowController(
                ofcHostname,
                ofcPortNumber,
                of13,
                dpid,
                neFrame
            );
          } catch (Exception e) {
            logger.error("", e);

            JOptionPane.showMessageDialog(neFrame,
                orgText + " failed to connect to OpenFlow controller.",
                "Failed", JOptionPane.ERROR_MESSAGE);

            setComponentsDefaultStatus(orgText);

            return;
          }

          JOptionPane.showMessageDialog(neFrame,
              orgText + " is connected to OpenFlow controller.", 
              "Succeeded", JOptionPane.INFORMATION_MESSAGE);

          connectButton.setText("Connected");
          displayNeStatusButton.setText(orgText + " - Connected -");
          flowmodStatusLabel.setText(Constants.FLOWMOD_INFO_TEXT_WAITING);

          try {
            connector.comfirmOpenFlowControllerLiving(channelFuture, of13);
          } catch (Exception e) {
            logger.error("", e);

            JOptionPane.showMessageDialog(neFrame,
                orgText + " is disconnected to OpenFlow controller.",
                "Error", JOptionPane.ERROR_MESSAGE);
            setComponentsDefaultStatus(orgText);

            return;
          }
        }

        /**
         * Set NE frame to initial status.
         * 
         * @param neDefaultText The initial text of NE button.
         */
        private void setComponentsDefaultStatus(String neDefaultText) {
          connectButton.setText("Connect");
          connectButton.setEnabled(true);
          displayNeStatusButton.setText(neDefaultText);
          flowmodStatusLabel.setText(Constants.FLOWMOD_INFO_TEXT_DEFAULT);
        }

      });
      thread.setName("Connection Thread");
      thread.start();
    }
  }
}
