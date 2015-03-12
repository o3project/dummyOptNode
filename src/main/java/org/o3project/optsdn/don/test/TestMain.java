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

package org.o3project.optsdn.don.test;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.o3project.optsdn.don.Main;
import org.o3project.optsdn.don.openflow.ConnectorToOpenFlowController;
import org.o3project.optsdn.don.util.Config;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The class for testing DummyOptNode(DON).
 */
public class TestMain {
  public static Logger logger = LoggerFactory.getLogger(TestMain.class);

  @Test
  public void test() {
    // Test1.Read Configuration file
    try {
      Config.load("config.properties");
    } catch (IOException e) {
      logger.error("", e);
      fail("Fail to read configuration file.");
    }

    // Test2.launch DON
    try {
      Main.launchGui();
    } catch (Exception e) {
      logger.error("", e);
      fail("Fail to launch DON.");
    }

    // Test3.Connect to OpenFlow controller(RYU OTN extension)
    try {
      String ofcHostname = Config.getProperty("ofcHostname");
      Integer ofcPortNumber = Integer.valueOf(Config.getProperty("ofcPortNumber"));
      ConnectorToOpenFlowController connector = new ConnectorToOpenFlowController();
      connector.connectToOpenFlowController(
          ofcHostname,
          ofcPortNumber,
          OFVersion.OF_13,
          (long) 0,
          null
      );
    } catch (Exception e) {
      logger.error("", e);
      fail("Fail to connect to OpenFlow controller.");
    }
  }
}
