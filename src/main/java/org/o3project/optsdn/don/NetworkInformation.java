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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.o3project.optsdn.don.nwc.Port;
import org.o3project.optsdn.don.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read and manage network information.
 */
public class NetworkInformation {
  public static Logger logger = LoggerFactory.getLogger(NetworkInformation.class);

  private static final String LOWER_NW_PORT = "lower_nw_port";
  private static final String UPPER_NW_PORT = "upper_nw_port";
  private static final String OCH_LINK = "ochlink";
  private static final String TERMINATION1 = "termination1";
  private static final String TERMINATION2 = "termination2";
  private static final String DPID = "dpid";
  private static final String PORT = "port";

  /* The set for NE ID(network element ID). */
  private Set<String> neIdSet;
  /* The Set for ports. */
  private Set<Port> portSet;
  /* The Map for links(inside NE). (Key:NE ID, Value:link) */
  private Map<String, List<List<Port>>> linkMap;
  /* The Map for NE connections with OMS(interconnect).
     (Key:NE ID, Value:OMS connection information list) */
  private Map<String, List<List<Port>>> omsConnectionInfoListMap;
  /* The Map for PT ID management. (Key:NE ID, Value:PT ID) */
  private Map<String, String> nePtMap;
  /* The Map for DP ID management. (Key:NE ID, Value:DP ID) */
  private Map<String, Long> dpidMap;

  /**
   * Read defined network information.
   * 
   * @throws Exception File Read Failed
   */
  public void readNetworkInformationFiles() throws Exception {
    neIdSet = new TreeSet<String>();
    portSet = new LinkedHashSet<Port>();
    linkMap = new HashMap<String, List<List<Port>>>();
    nePtMap = new HashMap<String, String>();
    dpidMap = new HashMap<String, Long>();

    final String dataDirName = "data";

    final String ll1FilePath = dataDirName + File.separator + "ll1.txt";
    try {
      parseBoundaryFile(ll1FilePath);
    } catch (Exception e) {
      logger.error("File Read Failed: {}", ll1FilePath);
      throw e;
    }

    final String ll2FilePath = dataDirName + File.separator + "ll2.txt";
    try {
      parseBoundaryFile(ll2FilePath);
    } catch (Exception e) {
      logger.error("File Read Failed: {}", ll2FilePath);
      throw e;
    }

    final String ochLinkFilePath = dataDirName + File.separator + "och_link.txt";
    try {
      parseOchLinkFile(ochLinkFilePath);
    } catch (Exception e) {
      logger.error("File Read Failed: {}", ochLinkFilePath);
      throw e;
    }

    final String idExFilePath = dataDirName + File.separator + "idex.txt";
    try {
      parseIdExFile(idExFilePath);
    } catch (Exception e) {
      logger.error("File Read Failed: {}", idExFilePath);
      throw e;
    }

    logNetworkInformations();
  }

  /**
   * Get the set for NE ID.
   * 
   * @return The set for NE ID
   */
  public Set<String> getNeIdSet() {
    return neIdSet;
  }

  /**
   * Get the Map for OMS connections.
   * 
   * @return The Map for OMS connections
   */
  public Map<String, List<List<Port>>> getOmsConnectionInfoListMap() {
    return omsConnectionInfoListMap;
  }

  /**
   * Get the Map for DP ID management.
   * 
   * @return The Map for DP ID management
   */
  public Map<String, Long> getDpidMap() {
    return dpidMap;
  }

  /**
   * Get the Set for ports.
   * 
   * @return The Set for ports
   */
  public Set<Port> getPortSet() {
    return portSet;
  }

  /**
   * Get the Map for PT ID management.
   * 
   * @return The Map for PT ID management
   */
  public Map<String, String> getNePtMap() {
    return nePtMap;
  }

  /**
   * Get the Map for links.
   * 
   * @return The Map for links
   */
  public Map<String, List<List<Port>>> getLinkMap() {
    return linkMap;
  }

  /**
   * Get informations from IDEx file(idex.txt).
   * - DP ID
   * - OF Port
   * 
   * @param filepath The IDEx file path
   * @throws Exception File Read Failed
   */
  private void parseIdExFile(String filepath) throws Exception {
    String idexJson = readIdexAsJson(filepath);

    ObjectMapper mapper = new ObjectMapper();
    @SuppressWarnings("unchecked")
    Map<String, Map<String, String>> value = mapper.readValue(idexJson, Map.class);

    Set<Entry<String, Map<String, String>>> entrySet = value.entrySet();
    dpidMap = new HashMap<String, Long>();
    Map<String, Integer> ofPortMap = new HashMap<String, Integer>();
    for (Entry<String, Map<String, String>> entry : entrySet) {
      Map<String, String> params = entry.getValue();
      BigInteger bigintDpid = new BigInteger(params.get(DPID));
      if (bigintDpid.compareTo(BigInteger.valueOf(0)) < 0
          || bigintDpid.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
        throw new Exception("DP ID is out of boundary. (DP ID valid between 0 and 2^63-1)");
      }
      long dpid = bigintDpid.longValue();

      String informationModelId = entry.getKey();
      Port port = new Port(informationModelId);
      String neId = port.getNeId();

      Long existDpid = dpidMap.get(neId);
      if (existDpid == null) {
        dpidMap.put(neId, dpid);
      } else if (!existDpid.equals(dpid)) {
        logger.warn("Fail to add DP ID[" + dpid + "]. "
            + "The DP ID of NE[" + neId + "] is already set"
            + "(exist DP ID[" + existDpid + "]).");
      }

      int ofPortId = Integer.valueOf(params.get(PORT));
      Integer existOfPortId = ofPortMap.get(informationModelId);
      if (existOfPortId != null) {
        if (!existOfPortId.equals(ofPortId)) {
          logger.warn("Fail to add OpenFlow Port ID[" + ofPortId + "]. "
              + "The OpenFlow Port ID of Port[" + informationModelId + "] is already set"
              + "(exist OpenFlow Port ID[" + existOfPortId + "]).");
        }
      } else {
        if (ofPortId < 0 && ofPortId > Integer.MAX_VALUE) {
          throw new Exception("OpenFlow Port ID is out of boundary. "
              + "(OpenFlow Port ID valid between 0 and 2^31-1)");
        }

        ofPortMap.put(informationModelId, ofPortId);
      }
    }

    for (Port port : portSet) {
      Integer openFlowPortId = ofPortMap.get(port.getInformationModelId());
      if (openFlowPortId == null) {
        continue;
      }
      port.setOpenFlowPortId(openFlowPortId);
    }

    for (List<List<Port>> linkList : omsConnectionInfoListMap.values()) {
      for (List<Port> link : linkList) {
        Port port1 = link.get(0);
        Integer openFlowPortId1 = ofPortMap.get(port1.getInformationModelId());
        if (openFlowPortId1 != null) {
          port1.setOpenFlowPortId(openFlowPortId1);
        }

        Port port2 = link.get(1);
        Integer openFlowPortId2 = ofPortMap.get(port2.getInformationModelId());
        if (openFlowPortId2 != null) {
          port2.setOpenFlowPortId(openFlowPortId2);
        }
      }
    }
  }

  /**
   * Read IDEx file(idex.txt) and transform to JSON format.
   * Example:
   *   Change the format from
   *     Key1:Value1
   *     Key2:Value2
   *   to
   *     {Key1:Value1,Key2:Value2}
   * 
   * @param filepath The IDEx file path
   * @return The string of JSON format
   * @throws IOException File Read Failed
   */
  private String readIdexAsJson(String filepath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(new File(filepath)));

    StringBuilder builder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      // Ignore empty line
      if (line.isEmpty()) {
        continue;
      }

      // Add comma at the end of line
      if (builder.length() > 0) {
        builder.append(",");
      }

      builder.append(line);
    }

    reader.close();

    // Add curly bracket
    return "{" + builder.toString() + "}";
  }

  /**
   * Get informations from LL1 file(ll1.txt) or LL2 file(ll2.txt).
   * - Port ID
   * - Link
   * - PT ID (Get PT ID if ODU layer-TTP is connect with Ether layer-TTP)
   * 
   * @param filepath The LL1 or LL2 file path
   * @throws IOException File Read Failed
   */
  private void parseBoundaryFile(String filepath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(new File(filepath)));
    ObjectMapper mapper = new ObjectMapper();
    String line;
    while ((line = reader.readLine()) != null) {
      // Ignore empty line
      if (line.isEmpty()) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, String> jsonMap = mapper.readValue(line, Map.class);

      Port lowerPort = new Port(jsonMap.get(LOWER_NW_PORT));
      Port upperPort = new Port(jsonMap.get(UPPER_NW_PORT));

      portSet.add(lowerPort);
      portSet.add(upperPort);

      addLink(lowerPort.getNeId(), lowerPort, upperPort);

      // Get PT ID if ODU layer-TTP is connect with Ether layer-TTP
      if (lowerPort.getLayer().equals(Constants.ODU)
          && lowerPort.getPortType().equals(Constants.TTP)
          && upperPort.getLayer().equals(Constants.ETHER)
          && upperPort.getPortType().equals(Constants.TTP)) {
        nePtMap.put(lowerPort.getNeId(), upperPort.getNeId());
      }
    }
    reader.close();
  }

  /**
   * Get informations from OCh Link file(och_link.txt).
   * - NE ID
   * - Port ID
   * - Link
   * - NE connections with OMS
   * 
   * @param filepath The OCh Link file path.
   * @throws IOException File Read Failed
   */
  private void parseOchLinkFile(String filepath) throws IOException {
    omsConnectionInfoListMap = new HashMap<String, List<List<Port>>>();

    BufferedReader reader = new BufferedReader(new FileReader(new File(filepath)));
    String omsConnectionInfoJson = reader.readLine();
    reader.close();

    ObjectMapper mapper = new ObjectMapper();
    @SuppressWarnings("rawtypes")
    Map value = mapper.readValue(omsConnectionInfoJson, Map.class);
    @SuppressWarnings({ "unchecked", "rawtypes" })
    List<Map> omsConnectionInfoList = (List<Map>) value.get(OCH_LINK);
    for (@SuppressWarnings("rawtypes") Map omsConnectionInfo : omsConnectionInfoList) {
      String informationModelId1 = (String) omsConnectionInfo.get(TERMINATION1);
      Port port1 = new Port(informationModelId1);
      portSet.add(port1);

      String informationModelId2 = (String) omsConnectionInfo.get(TERMINATION2);
      Port port2 = new Port(informationModelId2);
      portSet.add(port2);

      String neId1 = port1.getNeId();
      String neId2 = port2.getNeId();
      String layer1 = port1.getLayer();
      String layer2 = port2.getLayer();
      String portType1 = port1.getPortType();
      String portType2 = port2.getPortType();

      // If different NE
      if (!neId1.equals(neId2)
          && layer1.equals(Constants.OCH)
          && layer2.equals(Constants.OCH)
          && portType1.equals(Constants.CTP)
          && portType2.equals(Constants.CTP)) {
        // Add OMS connections
        addOmsConnection(port1, port2);
        addOmsConnection(port2, port1);

      // If same NE
      } else if (neId1.equals(neId2)
          && layer1.equals(Constants.OCH)
          && layer2.equals(Constants.OCH)) {
        if (portType1.equals(Constants.TTP) && portType2.equals(Constants.CTP)
            || portType1.equals(Constants.CTP) && portType2.equals(Constants.TTP)
            || portType1.equals(Constants.CTP) && portType2.equals(Constants.CTP)) {
          // Add link
          addLink(neId1, port1, port2);
        }
      }

      neIdSet.add(neId1);
      neIdSet.add(neId2);

    }
  }

  /**
   * Add a link to the link Map.
   * 
   * @param neId   NE ID that the link belongs to
   * @param port1  Port1
   * @param port2  Port2
   */
  private void addLink(String neId, Port port1, Port port2) {
    if (linkMap.get(neId) == null) {
      linkMap.put(neId, new ArrayList<List<Port>>());
    }
    List<Port> edgePorts = Arrays.asList(port1, port2);
    linkMap.get(neId).add(edgePorts);
  }

  /**
   * Add a OMS connection to OMS Map.
   * 
   * @param port1 Source port.
   * @param port2 Destination port.
   */
  private void addOmsConnection(Port port1, Port port2) {
    String neId = port1.getNeId();
    if (omsConnectionInfoListMap.get(neId) == null) {
      omsConnectionInfoListMap.put(neId, new ArrayList<List<Port>>());
    }
    List<Port> link = Arrays.asList(port1, port2);
    omsConnectionInfoListMap.get(neId).add(link);
  }

  /**
   * Output the network informations.
   */
  private void logNetworkInformations() {
    logger.info("Read - NE: {}", neIdSet);
    logger.info("Read - Port: {}", portSet);
    logger.info("Read - Link: {}", linkMap);

    List<String> ptLogList = new ArrayList<String>();
    for (Entry<String, String> entry : nePtMap.entrySet()) {
      ptLogList.add(entry.getValue() + "(" + entry.getKey() + ")");
    }
    logger.info("Read - PT: {}", StringUtils.join(ptLogList, ", "));

    List<String> dpidLogList = new ArrayList<String>();
    for (Entry<String, Long> entry : dpidMap.entrySet()) {
      dpidLogList.add(entry.getValue() + "(" + entry.getKey() + ")");
    }
    logger.info("Read - DPID: {}", StringUtils.join(dpidLogList, ", "));
  }
}
