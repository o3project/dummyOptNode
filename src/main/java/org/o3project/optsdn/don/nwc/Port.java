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

package org.o3project.optsdn.don.nwc;

import org.o3project.optsdn.don.util.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port object class.
 */
public class Port {
  private final String informationModelId;
  private final String nw;
  private final String neId;
  private final String layer;
  private String portType;
  private int portId;
  private Integer openFlowPortId;

  private static final String NW = "NW";
  private static final String NE = "NE";
  private static final String LAYER = "Layer";

  /**
   * Constructor.
   * 
   * @param informationModelId  Information Model ID
   * @param nw                  NW ID
   * @param neId                NE ID
   * @param layer               Layer
   * @param portType            Port type
   * @param portId              Port ID
   */
  public Port(
      String informationModelId,
      String nw,
      String neId,
      String layer,
      String portType,
      Integer portId) {

    super();
    this.informationModelId = informationModelId;
    this.nw = nw;
    this.neId = neId;
    this.layer = layer;
    this.portType = portType;
    this.portId = portId;
  }

  /**
   * Constructor.
   * 
   * @param informationModelId  Information Model ID
   */
  public Port(String informationModelId) {
    super();
    this.informationModelId = informationModelId;

    Map<String, String> informationModelIdMap = createInformationModelIdMap(informationModelId);
    nw = informationModelIdMap.get(NW);
    neId = informationModelIdMap.get(NE);
    layer = informationModelIdMap.get(LAYER);

    String ttp = informationModelIdMap.get(Constants.TTP);
    String ctp = informationModelIdMap.get(Constants.CTP);
    if (ttp != null && ctp == null) {
      portType = Constants.TTP;
      portId = Integer.valueOf(ttp);
    } else if (ctp != null && ttp == null) {
      portType = Constants.CTP;
      portId = Integer.valueOf(ctp);
    }
  }

  /**
   * Create a Map object with InformationModelID.
   * (example: NW=SDN, NE=OPT1, Layer=ODU, CTP=1)
   * 
   * @param informationModelId The Information Model ID
   * @return The Map object
   */
  private Map<String, String> createInformationModelIdMap(String informationModelId) {
    String[] imIdExpressions = informationModelId.split(",");
    Map<String, String> imIdMap = new HashMap<String, String>();
    for (String expression : imIdExpressions) {
      int firstEqualIndex = expression.indexOf("=");
      imIdMap.put(
          expression.substring(0, firstEqualIndex).trim(),
          expression.substring(firstEqualIndex + 1, expression.length()).trim());
    }
    return imIdMap;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((layer == null) ? 0 : layer.hashCode());
    result = prime * result + ((neId == null) ? 0 : neId.hashCode());
    result = prime * result + ((nw == null) ? 0 : nw.hashCode());
    result = prime * result + ((informationModelId == null) ? 0 : informationModelId.hashCode());
    result = prime * result + portId;
    result = prime * result + ((portType == null) ? 0 : portType.hashCode());
    return result;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Port other = (Port) obj;
    if (informationModelId == null) {
      if (other.informationModelId != null) {
        return false;
      }
    } else if (!informationModelId.equals(other.informationModelId)) {
      return false;
    }
    if (nw == null) {
      if (other.nw != null) {
        return false;
      }
    } else if (!nw.equals(other.nw)) {
      return false;
    }
    if (neId == null) {
      if (other.neId != null) {
        return false;
      }
    } else if (!neId.equals(other.neId)) {
      return false;
    }
    if (layer == null) {
      if (other.layer != null) {
        return false;
      }
    } else if (!layer.equals(other.layer)) {
      return false;
    }
    if (portType == null) {
      if (other.portType != null) {
        return false;
      }
    } else if (!portType.equals(other.portType)) {
      return false;
    }
    if (portId != other.portId) {
      return false;
    }
    return true;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "\"" + informationModelId + "\"";
  }

  /**
   * Get port list with search condition.
   * 
   * @param ports     Target search port list
   * @param neId      Condition: NE ID
   * @param layer     Condition: Layer
   * @param portType  Condition: Port type
   * @return Port list
   */
  public static List<Port> searchPorts(
      Collection<Port> ports, String neId, String layer, String portType) {
    List<Port> portList = new ArrayList<Port>();
    for (Port port : ports) {
      if (neId != null && !port.neId.equals(neId)) {
        continue;
      }
      if (layer != null && !port.layer.equals(layer)) {
        continue;
      }
      if (portType != null && !port.portType.equals(portType)) {
        continue;
      }
      portList.add(port);
    }
    return portList;
  }

  /**
   * Get priority of the port.
   * Priority: TTP > CTP
   * 
   * @return Port Priority
   */
  public int getPortPriority() {
    switch (portType) {
      case Constants.TTP:
        return 2;
      case Constants.CTP:
        return 1;
      default:
        return 0;
    }
  }

  /**
   * Get priority of the layer.
   * Priority: ETHER > ODU > OCh
   * 
   * @return Layer Priority
   */
  public int getLayerPriority() {
    switch (layer) {
      case Constants.ETHER:
        return 2;
      case Constants.ODU:
        return 1;
      case Constants.OCH:
        return 0;
      default:
        return -1;
    }
  }

  /**
   * Get the Information Model ID.
   * 
   * @return The Information Model ID
   */
  public String getInformationModelId() {
    return informationModelId;
  }

  /**
   * Get the NE ID.
   * 
   * @return The NE ID
   */
  public String getNeId() {
    return neId;
  }

  /**
   * Get the port type.
   * 
   * @return The Port Type
   */
  public String getPortType() {
    return portType;
  }

  /**
   * Get the layer.
   * 
   * @return The Layer
   */
  public String getLayer() {
    return layer;
  }

  /**
   * Get the port ID.
   * 
   * @return The Port ID
   */
  public int getPortId() {
    return portId;
  }

  /**
   * Get the OpenFlow port ID.
   * 
   * @return The OpenFlow Port ID
   */
  public Integer getOpenFlowPortId() {
    return openFlowPortId;
  }

  /**
   * Set the OpenFlow port ID.
   * 
   * @param openFlowPortId The OpenFlow Port ID
   */
  public void setOpenFlowPortId(Integer openFlowPortId) {
    this.openFlowPortId = openFlowPortId;
  }
}
