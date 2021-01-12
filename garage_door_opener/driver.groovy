/**
 * Driver:     Shelly Garage Door Opener
 * Author:     Mirco Caramori
 * Repository: https://github.com/mircolino/shelly/tree/main/garage_door_opener
 * Import URL: https://raw.githubusercontent.com/mircolino/shelly/main/garage_door_opener/driver.groovy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 */

public static String version() { return "v1.10.33"; }

/**
 * Change Log:
 *
 * 2021.01.08 - Initial implementation
 * 2021.01.10 - Added "opening" and "closing" emulation
 * 2021.01.10 - Changed Reed switch logic from NO to NC
 * 2021.01.11 - Removed double "https//" from importUrl
 *
 */

// Metadata -------------------------------------------------------------------------------------------------------------------

metadata {
  definition(name: "Shelly Garage Door Opener", namespace: "mircolino", author: "Mirco Caramori", importUrl: "https://raw.githubusercontent.com/mircolino/shelly/main/garage_door_opener/driver.groovy") {
    capability "Sensor";
    capability "Actuator";
    capability "Contact Sensor";
    capability "Garage Door Control";
    capability "Refresh";

    // command "open";
    // command "close";
    // command "refresh";

    // attribute "contact", "string";                              // "closed", "open"
    // attribute "door", "string";                                 // "closed", "closing", "open", "opening", "unknown"
  }

  preferences {
    input(name: "shellyAddress", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Address</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly IP address or hostname</font>", defaultValue: "", required: true);
    input(name: "shellyUsername", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Username</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly login username (default: none)</font>", defaultValue: "", required: false);
    input(name: "shellyPassword", type: "password", title: "<font style='font-size:12px; color:#1a77c9'>Password</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly login password (default: none)</font>", defaultValue: "", required: false);
    input(name: "shellyChannel", type: "number", title: "<font style='font-size:12px; color:#1a77c9'>Channel</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly channel (default: 0)</font>", defaultValue: "0", required: true);
    input(name: "cycleTime", type: "number", title: "<font style='font-size:12px; color:#1a77c9'>Run Time</font>", description: "<font style='font-size:12px; font-style: italic'>Door open/close time (default: 12 sec)</font>", defaultValue: "12", required: true);
    input(name: "logLevel", type: "enum", title: "<font style='font-size:12px; color:#1a77c9'>Log Verbosity</font>", description: "<font style='font-size:12px; font-style: italic'>Default: 'Debug' for 30 min and 'Info' thereafter</font>", options: [0:"Error", 1:"Warning", 2:"Info", 3:"Debug", 4:"Trace"], multiple: false, defaultValue: 3, required: true);
  }
}

/*
 * State variables used by the driver:
 *
 * initialized                  set to true when the device has been succesfully initialized (valid ip/hostname, username, password and channel)
 *
 */

// Preferences -----------------------------------------------------------------------------------------------------------------

private String shellyGetAddress() {
  //
  // Return the Shelly ip address or hostname, or "" if undefined
  //
  if (settings.shellyAddress != null) return (settings.shellyAddress.toString());
  return ("");
}

// -------------------------------------------------------------

private String shellyGetLogin() {
  //
  // Return the Shelly login info, or "" if undefined
  //
  String username = (settings.shellyUsername != null)? settings.shellyUsername.toString(): "";
  String password = (settings.shellyPassword != null)? settings.shellyPassword.toString(): "";  

  if (username && password) return ("${username}:${password}@");
  return ("");
}

// -------------------------------------------------------------

private Integer shellyGetChannel() {
  //
  // Return the Shelly switch channel used by the garage door, or 0 if undefined
  //
  if (settings.shellyChannel != null) return (settings.shellyChannel.toInteger());
  return (0);
}

// -------------------------------------------------------------

private Integer cycleGetTime() {
  //
  // Return the total time in seconds it takes the garage door to fully close or open
  //
  if (settings.cycleTime != null) return (settings.cycleTime.toInteger());
  return (12);
}

// -------------------------------------------------------------

private Integer logGetLevel() {
  //
  // Get the log level as an Integer:
  //
  //   0) log only Errors
  //   1) log Errors and Warnings
  //   2) log Errors, Warnings and Info
  //   3) log Errors, Warnings, Info and Debug
  //   4) log Errors, Warnings, Info, Debug and Trace/diagnostic (everything)
  //
  // If the level is not yet set in the driver preferences, return a default of 2 (Info)
  // Declared public because it's being used by the child-devices as well
  //
  if (settings.logLevel != null) return (settings.logLevel.toInteger());
  return (2);
}

// Logging ---------------------------------------------------------------------------------------------------------------------

void logDebugOff() {
  //
  // runIn() callback to disable "Debug" logging after 30 minutes
  // Cannot be private
  //
  if (logGetLevel() > 2) device.updateSetting("logLevel", [type: "enum", value: "2"]);
}

// -------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (logGetLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (logGetLevel() > 1) log.info(str); }
private void logDebug(String str) { if (logGetLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (logGetLevel() > 3) log.trace(str); }

// -------------------------------------------------------------

private void logData(Map data) {
  //
  // Log all data received from the wifi gateway
  // Used only for diagnostic/debug purposes
  //
  if (logGetLevel() > 3) {
    data.each {
      logTrace("$it.key = $it.value");
    }
  }
}

// Attribute handling ----------------------------------------------------------------------------------------------------------

private String attributeGet(String attribute) {
  //
  // Return current <attribute> value
  // Return null if <attribute> is not defined
  //
  return (device.currentValue(attribute) as String);
}

// -------------------------------------------------------------

private Boolean attributeSet(String attribute, String val) {
  //
  // Only set <attribute> if new <val> is different
  // Return true if <attribute> has actually been updated/created
  //
  if (attributeGet(attribute) != val) {
    sendEvent(name: attribute, value: val);
    return (true);
  }

  return (false);
}

// -------------------------------------------------------------

private Boolean isDoorClosed() {

  return ((attributeGet("contact") == "closed")? true: false);
}

// Shelly HTTP commands --------------------------------------------------------------------------------------------------------

private Map shellyStatus() {
  //
  // Return null if error or a map of parameters:
  //
  //      ip: ipAddress
  //     mac: macAddress
  //   relay: 0 (off), 1 (on), null (channel doesn't exist)
  // contact: 0 (off), 1 (on), null (channel doesn't exist)
  //
  Map status = null;
  
  String uri = "http://${shellyGetLogin()}${shellyGetAddress()}/status";

  try {
    httpGet(uri) { /* groovyx.net.http.HttpResponseDecorator */ resp ->
      if (resp.data) {
        Integer channel = shellyGetChannel();
        status = [:];

        status.ip = resp.data.wifi_sta.ip;
        status.mac = resp.data.mac;

        if (resp.data.relays && resp.data.relays.size() > channel) status.relay = (resp.data.relays[channel].ison as Boolean)? 1: 0;
        else status.relay = null;

        if (resp.data.inputs && resp.data.inputs.size() > channel) status.contact = (resp.data.inputs[channel].input as Integer)? 1: 0;
        else status.contact = null;
      }  
    }
  }
  catch (Exception e) {
    logError("Exception in shellyStatus(${uri}): ${e}");
  }

  return (status);
}

// -------------------------------------------------------------

private Boolean shellyRelay(String action) {
  //
  // <action> == "on"  switch relay on
  // <action> == "off" switch relay off
  // Return false if error
  //
  Boolean ok = false;

  String uri = "http://${shellyGetLogin()}${shellyGetAddress()}/relay/${shellyGetChannel()}?turn=${action}";

  try {
    httpPost(uri, "") {}
    ok = true;
  }
  catch (Exception e) {
    logError("Exception in open(${uri}): ${e}");
  }

  return (ok);
}

// Shelly HTTP callbacks -------------------------------------------------------------------------------------------------------

private void contact(String state) {
  //
  // <state> == "on"  door is open
  // <state> == "off" door is closed 
  //
  logDebug("contact(${state})");

  if (state == "off") {
    // Door closed
    attributeSet("contact", "closed");
    attributeSet("door", "closed");
  }
  else {
    // Door opening
    attributeSet("contact", "open");
    attributeSet("door", "opening");

    // Wait for the door to open and update its state
    runIn(cycleGetTime(), refresh);
  }
}

// -------------------------------------------------------------

private void relay(String state) {
  //
  // <state> == "on"  relay is on
  // <state> == "off" relay is off 
  //
  logDebug("relay(${state})");

  if (state == "on" && !isDoorClosed()) {

    // Door closing
    attributeSet("door", "closing");

    // Wait for the door to close and update its state
    runIn(cycleGetTime(), refresh);
  }
}

// Commands --------------------------------------------------------------------------------------------------------------------

void open() {
  logDebug("open()");

  if (!state.initialized) logError("Device not initialized");
  else {
    if (!isDoorClosed()) refresh(); 
    else if (shellyRelay("on") == false) logError("Unable to communicate with Shelly device");
  }
}

// -------------------------------------------------------------

void close() {
  logDebug("close()");

  if (!state.initialized) logError("Device not initialized");
  else {
    if (isDoorClosed()) refresh(); 
    else if (shellyRelay("on") == false) logError("Unable to communicate with Shelly device");
  }
}

// -------------------------------------------------------------
void refresh() {
  logDebug("refresh()");

  if (!state.initialized) logError("Device not initialized");
  else {
    Map status = shellyStatus();
    if (status == null) logError("Unable to communicate with Shelly device");
    else if (status.contact == null) logError("Invalid Shelly device channel");
    else {
      // Update device attributes
      attributeSet("contact", status.contact? "open": "closed");
      attributeSet("door", status.contact? "open": "closed");
    }
  }
}

// Driver lifecycle ------------------------------------------------------------------------------------------------------------

void installed() {
  //
  // Called once when the driver is created
  //
  try {
    logDebug("installed()");
  }
  catch (Exception e) {
    logError("Exception in installed(): ${e}");
  }
}

// -------------------------------------------------------------

void updated() {
  //
  // Called everytime the user saves the driver preferences
  //
  try {
    logDebug("updated()");

    // Clear previous states
    state.clear();    

    // Unschedule possible previous runIn() calls
    unschedule();

    // Turn off debug log in 30 minutes
    if (logGetLevel() > 2) runIn(1800, logDebugOff);

    // Initialize device with new preferences
    Map status = shellyStatus();
    if (status == null) logError("Unable to communicate with Shelly device");
    else if (status.contact == null) logError("Invalid Shelly device channel");
    else {
      // Update device attributes
      attributeSet("contact", status.contact? "open": "closed");
      attributeSet("door", status.contact? "open": "closed");

      // Set DNI if different
      if (status.mac != device.getDeviceNetworkId()) device.setDeviceNetworkId(status.mac);

      // Flag the device as properly initialized
      state.initialized = true;
    }
  }
  catch (Exception e) {
    logError("Exception in updated(): ${e}");
  }
}

// -------------------------------------------------------------

void uninstalled() {
  //
  // Called once when the driver is deleted
  //
  try {
    logDebug("uninstalled()");
  }
  catch (Exception e) {
    logError("Exception in uninstalled(): ${e}");
  }
}

// -------------------------------------------------------------

void parse(String msg) {
  //
  // Called everytime a GET/POST message is received from the WiFi Gateway
  //
  try {
    logDebug("parse()");

    if (!state.initialized) logError("Device not initialized");
    else {
      // Parse GET/POST message
      Map data = parseLanMessage(msg);

      // Process the header and extract parameters from URL
      String header = data["header"];   
      List<String> token = header.tokenize(" ");
      if (token.size() > 1) {
        token = token[1].tokenize("/"); 
        if (token.size() > 2) {
          // We ignore token[1] (Shelly channel) since we have no child devices
          if (token[0] == "contact") contact(token[2]);
          if (token[0] == "relay") relay(token[2]);
        }
      }
    }
  }
  catch (Exception e) {
    logError("Exception in parse(): ${e}");
  }
}

// Recycle ---------------------------------------------------------------------------------------------------------------------

/*

*/

// EOF -------------------------------------------------------------------------------------------------------------------------
