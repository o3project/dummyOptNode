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

package org.o3project.optsdn.don.util;

/**
 * Define Constant values.
 */
public class Constants {
  /** Ether layer. */
  public static final String ETHER = "Ether";
  /** ODU layer. */
  public static final String ODU = "ODU";
  /** OCh layer. */
  public static final String OCH = "OCh";
  /** TTP. */
  public static final String TTP = "TTP";
  /** CTP. */
  public static final String CTP = "CTP";

  /** Initial message on Flowmod state. */
  public static final String FLOWMOD_INFO_TEXT_DEFAULT = "";
  /** Waiting message on Flowmod state. */
  public static final String FLOWMOD_INFO_TEXT_WAITING = "Waiting for Flowmod...";
}
