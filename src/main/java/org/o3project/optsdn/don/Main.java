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

package org.o3project.optsdn.don;

import org.o3project.optsdn.don.frame.MainFrame;
import org.o3project.optsdn.don.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

/**
 * Launching DummyOptNode[DON](Main class).
 */
public class Main {
  public static Logger logger = LoggerFactory.getLogger(Main.class);

  /**
   * Start DON(Main method).
   * 
   * @param args (unused)
   */
  public static void main(String[] args) {
    logger.info("Starting DON.");
    try {
      launchGui();
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  /**
   * Launch DON.
   * 
   * @throws Exception Launch Failed
   */
  public static void launchGui() throws Exception {
    NetworkInformation networkInformation;
    MainFrame mainFrame;
    try {
      Config.load("config.properties");

      networkInformation = new NetworkInformation();
      networkInformation.readNetworkInformationFiles();

      mainFrame = new MainFrame(networkInformation);

    } catch (Exception e) {
      JOptionPane.showMessageDialog(null,
          "Fail to launch DON.", "Error", JOptionPane.ERROR_MESSAGE);
      throw e;
    }

    mainFrame.setVisible(true);
  }
}
