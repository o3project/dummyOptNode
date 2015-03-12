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

import org.o3project.optsdn.don.NetworkInformation;
import org.o3project.optsdn.don.nwc.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

/**
 * Main frame.
 */
public class MainFrame extends JFrame {
  public static Logger logger = LoggerFactory.getLogger(MainFrame.class);

  /**
   * Constructor.
   * 
   * @param networkInformation Network Information
   * @throws Exception Network Information is incorrect
   */
  public MainFrame(NetworkInformation networkInformation) throws Exception {
    super();

    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.setBackground(Color.WHITE);

    // Topology (Image File)
    Panel fixedPanel = new Panel();
    fixedPanel.setLayout(new BoxLayout(fixedPanel, BoxLayout.Y_AXIS));
    fixedPanel.add(new JLabel("Topology"));
    fixedPanel.add(createTopologyLabel("img" + File.separator + "topology.png"));
    fixedPanel.add(Box.createVerticalStrut(5));

    // Separator
    fixedPanel.add(new JSeparator());

    // NE List
    fixedPanel.add(new JLabel("NE List"));
    Panel variablePanel = new Panel();
    variablePanel.setLayout(new BoxLayout(variablePanel, BoxLayout.Y_AXIS));
    variablePanel.add(createNeListPane(networkInformation));

    // Copyright
    JLabel label = new JLabel("COPYRIGHT FUJITSU LIMITED 2015");
    label.setHorizontalAlignment(JLabel.RIGHT);

    // Set fixed panel to main panel
    mainPanel.add(fixedPanel, BorderLayout.NORTH);
    // Set flexible panel to main panel
    mainPanel.add(variablePanel, BorderLayout.CENTER);
    // Set copyright panel to main panel
    mainPanel.add(label, BorderLayout.SOUTH);

    // Title
    setTitle("DummyOptNode");
    add(mainPanel);
    pack();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Add listener
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent event) {
        super.windowClosing(event);
        logger.info("Closing DON.");
      }
    });
  }

  /**
   * Create topology label.
   * 
   * @param filepath The topology image file path
   * @return Topology Label
   */
  private JLabel createTopologyLabel(String filepath) {
    JLabel label = new JLabel();
    if (!new File(filepath).exists()) {
      logger.warn("The topology image file does not exist: {}", filepath);
      return label;
    }
    label.setIcon(new ImageIcon(filepath));
    return label;
  }

  /**
   * Create NE List Pane.
   * 
   * @param networkInformation Network Information
   * @return NE List Pane (scalable).
   * @throws Exception Network Information is incorrect
   */
  private JScrollPane createNeListPane(NetworkInformation networkInformation) throws Exception {
    JPanel neListPanel = new JPanel();
    neListPanel.setLayout(new BoxLayout(neListPanel, BoxLayout.Y_AXIS));

    for (String neId : networkInformation.getNeIdSet()) {

      List<List<Port>> omsConnectionInfoList =
          networkInformation.getOmsConnectionInfoListMap().get(neId);
      if (omsConnectionInfoList == null) {
        omsConnectionInfoList = new ArrayList<List<Port>>();
        logger.warn("OMS connections of " + neId + " are not found.");
      }

      Long dpid = networkInformation.getDpidMap().get(neId);
      if (dpid == null) {
        throw new Exception("DPID of " + neId + " is not found.");
      }
      String neTitle = neId + "(DPID=" + dpid + ")";

      List<Port> portList =
          Port.searchPorts(networkInformation.getPortSet(), neId, null, null);

      String ptId = networkInformation.getNePtMap().get(neId);
      if (ptId != null) {
        portList.addAll(Port.searchPorts(networkInformation.getPortSet(), ptId, null, null));
      }

      List<List<Port>> linkList = networkInformation.getLinkMap().get(neId);
      if (linkList == null) {
        linkList = new ArrayList<List<Port>>();
        logger.warn("Links of " + neId + " are not found.");
      }

      JButton displayNeStatusButton = new JButton();
      displayNeStatusButton.addActionListener(
          new NeViewActionListener(
              neTitle,
              portList,
              linkList,
              omsConnectionInfoList,
              ptId,
              dpid,
              displayNeStatusButton
          )
      );
      displayNeStatusButton.setText(neTitle);

      displayNeStatusButton.setMaximumSize(
          new Dimension(Short.MAX_VALUE, displayNeStatusButton.getMaximumSize().height)
      );
      neListPanel.add(displayNeStatusButton);
    }
    JScrollPane neListScrollPane = new JScrollPane();
    neListScrollPane.setPreferredSize(new Dimension(neListPanel.getPreferredSize().width, 150));
    neListScrollPane.getViewport().setView(neListPanel);
    return neListScrollPane;
  }

  /**
   * Action Listener for NE button.
   */
  private class NeViewActionListener implements ActionListener {
    private String neTitle;
    private List<Port> portList;
    private List<List<Port>> linkList;
    private List<List<Port>> omsConnectionInfoList;
    private String ptId;
    private long dpid;
    private NeFrame neFrame;
    private JButton displayNeStatusButton;

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
    private NeViewActionListener(
        String neTitle, List<Port> portList, List<List<Port>> linkList,
        List<List<Port>> omsConnectionInfoList,
        String ptId, long dpid, JButton displayNeStatusButton) {

      this.neTitle = neTitle;
      this.portList = portList;
      this.linkList = linkList;
      this.omsConnectionInfoList = omsConnectionInfoList;
      this.ptId = ptId;
      this.dpid = dpid;
      this.displayNeStatusButton = displayNeStatusButton;
    }

    /**
     * Display NE frame if NE button is clicked.
     * Forefront the frame if the frame is already exist.
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      if (neFrame == null) {
        neFrame = new NeFrame(
            neTitle, portList, linkList, omsConnectionInfoList, ptId, dpid, displayNeStatusButton
        );
        neFrame.setLocationByPlatform(true);
        neFrame.setVisible(true);
      } else {
        if (neFrame.isVisible()) {
          neFrame.toFront();
        } else {
          neFrame.setVisible(true);
        }
      }
    }
  }
}
