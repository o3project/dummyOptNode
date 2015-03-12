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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Read configuration file.
 */
public class Config {
  /** Properties. */
  private static Properties properties;

  /**
   * Read configuration file.
   * 
   * @param filepath The configuration file path(*.properties)
   * @throws IOException File Read Failed
   */
  public static void load(String filepath) throws IOException {
    properties = new Properties();
    InputStream inputStream = new FileInputStream(filepath);
    properties.load(inputStream);
  }

  /**
   * Get property value.
   * 
   * @param key Property Key
   * @return Property Value
   */
  public static String getProperty(String key) {
    return properties.getProperty(key);
  }
}
