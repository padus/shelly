/**
 * Driver:     Shelly Detached
 * Author:     Mirco Caramori
 * Repository: https://github.com/padus/shelly/tree/main/detached
 * Import URL: https://raw.githubusercontent.com/padus/shelly/main/detached/driver.groovy
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

public static String version() { return "v1.0.5"; }

/**
 * Change Log:
 *
 * 2021.03.19 - Initial implementation
 *            - Removed channel selection forcing it to 0
 * 2021.03.22 - Added diagnostic data
 * 2021.08.18 - Relocated repository: mircolino -> padus
 *
 */

// Metadata -------------------------------------------------------------------------------------------------------------------

metadata {
  definition(name: "Shelly Detached", namespace: "mircolino", author: "Mirco Caramori", importUrl: "https://raw.githubusercontent.com/padus/shelly/main/detached/driver.groovy") {
    capability "Sensor";
    capability "Contact Sensor";
    capability "Actuator";
    capability "Switch";
    capability "Refresh";

    // command "on";
    // command "off";
    // command "refresh";

    // attribute "contact", "string";                              // "closed", "open"
    // attribute "switch", "string";                               // "on", "off"
  }

  preferences {
    input(name: "deviceAddress", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Address</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly IP address or hostname</font>", defaultValue: "", required: true);
    input(name: "deviceUsername", type: "string", title: "<font style='font-size:12px; color:#1a77c9'>Username</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly login username (default: none)</font>", defaultValue: "", required: false);
    input(name: "devicePassword", type: "password", title: "<font style='font-size:12px; color:#1a77c9'>Password</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly login password (default: none)</font>", defaultValue: "", required: false);
    // input(name: "deviceChannel", type: "number", title: "<font style='font-size:12px; color:#1a77c9'>Channel</font>", description: "<font style='font-size:12px; font-style: italic'>Shelly channel (default: 0)</font>", defaultValue: "0", required: true);
    input(name: "timeDebounce", type: "number", title: "<font style='font-size:12px; color:#1a77c9'>Debounce</font>", description: "<font style='font-size:12px; font-style: italic'>Milliseconds during which next switch is ignored (default: 0)</font>", defaultValue: "0", required: true);
    input(name: "logLevel", type: "enum", title: "<font style='font-size:12px; color:#1a77c9'>Log Verbosity</font>", description: "<font style='font-size:12px; font-style: italic'>Default: 'Debug' for 30 min and 'Info' thereafter</font>", options: [0:"Error", 1:"Warning", 2:"Info", 3:"Debug", 4:"Trace"], multiple: false, defaultValue: 3, required: true);
  }
}

/*
 * State variables used by the driver:
 *
 * timeStamp                    set to epoch (in milliseconds) when the device has been succesfully initialized (valid ip/hostname, username, password and channel)
 *                              also used to de-bounce the Reed state change
 *
 */

// Preferences -----------------------------------------------------------------------------------------------------------------

private String deviceAddress() {
  //
  // Return the device login info and ip address or hostname, or "" if invalid
  //
  String address = (settings.deviceAddress != null)? settings.deviceAddress.toString(): "";
  String username = (settings.deviceUsername != null)? settings.deviceUsername.toString(): "";
  String password = (settings.devicePassword != null)? settings.devicePassword.toString(): "";  

  if (username && password && address) return ("${username}:${password}@${address}");

  return (address);
}

// -------------------------------------------------------------

private Integer deviceChannel() {
  //
  // Return the switch channel used by this device object, or 0 if undefined
  //
  // if (settings.deviceChannel != null) return (settings.deviceChannel.toInteger());
  return (0);
}

// -------------------------------------------------------------

private Integer timeDebounce() {
  //
  // Return the timespan (in milliseconds) within which switching is ignored, or 0 if undefined
  //
  if (settings.timeDebounce != null) return (settings.timeDebounce.toInteger());
  return (0);
}

// -------------------------------------------------------------

private Integer logLevel() {
  //
  // Get the log level as an Integer:
  //
  //   0) log only Errors
  //   1) log Errors and Warnings
  //   2) log Errors, Warnings and Info
  //   3) log Errors, Warnings, Info and Debug
  //   4) log Errors, Warnings, Info, Debug and Trace (everything)
  //
  // If the level is not yet set in the driver preferences, return a default of 2 (Info)
  // Declared public because it's being used by the child-devices as well
  //
  if (settings.logLevel != null) return (settings.logLevel.toInteger());
  return (2);
}

// -------------------------------------------------------------

private Boolean isDeviceInitialized() {
  return ((state.timeStamp == null)? false: true);
}

// Logging ---------------------------------------------------------------------------------------------------------------------

private void logError(String str) { log.error(str); }
private void logWarning(String str) { if (logLevel() > 0) log.warn(str); }
private void logInfo(String str) { if (logLevel() > 1) log.info(str); }
private void logDebug(String str) { if (logLevel() > 2) log.debug(str); }
private void logTrace(String str) { if (logLevel() > 3) log.trace(str); }

// -------------------------------------------------------------

private void logResponse(String id, Object obj) {
  //
  // Log a generic groovy object
  // Used only for diagnostic/debug purposes
  //
  if (logLevel() > 3) {
    String text = id;
    obj.properties.each {
      text += "\n${it}";
    }
    logTrace(text);
  }
}

// -------------------------------------------------------------

private void logData(String id, Map data) {
  //
  // Log all data received from the device
  // Used only for diagnostic/debug purposes
  //
  if (logLevel() > 3) {
    String text = id;    
    data.each {
      text += "\n${it.key} = ${it.value}";
    }
    logTrace(text);    
  }
}

// -------------------------------------------------------------

void logDebugOff() {
  //
  // runIn() callback to disable "Debug" logging after 30 minutes
  // Cannot be private
  //
  if (logLevel() > 2) device.updateSetting("logLevel", [type: "enum", value: "2"]);
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

// Shelly HTTP -----------------------------------------------------------------------------------------------------------------

private Map shellyCommandStatus(String address, Integer channel) {
  //
  // Return null if error or a map of parameters:
  //
  //      ip: ipAddress
  //     mac: macAddress
  //   relay: 0 (off), 1 (on), null (channel doesn't exist)
  // contact: 0 (off), 1 (on), null (channel doesn't exist)
  //
  Map status = null;
  
  String uri = "http://${address}/status";

  try {
    httpGet(uri) { /* groovyx.net.http.HttpResponseDecorator */ resp ->
      // If debug is enabled, log the response received from the device
      logResponse("status(${uri})", resp);

      if ((resp.status as Integer) != 200) throw new Exception("http status = ${resp.status}")

      status = [:];

      status.ip = resp.data.wifi_sta.ip;
      status.mac = resp.data.mac;

      if (resp.data.relays && resp.data.relays.size() > channel) status.relay = (resp.data.relays[channel].ison as Boolean)? 1: 0;
      else status.relay = null;

      if (resp.data.inputs && resp.data.inputs.size() > channel) status.contact = (resp.data.inputs[channel].input as Integer)? 1: 0;
      else status.contact = null;
    }
  }
  catch (Exception e) {
    logError("status(${uri}): ${e}");
  }

  return (status);
}

// -------------------------------------------------------------

private Boolean shellyCommandRelay(String address, Integer channel, String action) {
  //
  // <action> == "on"  switch relay on
  // <action> == "off" switch relay off
  // Return false if error
  //
  Boolean ok = false;

  String uri = "http://${address}/relay/${channel}?turn=${action}";

  try {
    httpPost(uri, "") { /* groovyx.net.http.HttpResponseDecorator */ resp ->
      // If debug is enabled, log the response received from the device
      logResponse("relay(${uri})", resp);

      if ((resp.status as Integer) != 200) throw new Exception("http status = ${resp.status}")
      ok = true;
    }
  }
  catch (Exception e) {
    logError("relay(${uri}): ${e}");
  }

  return (ok);
}

// -------------------------------------------------------------

private void shellyCallbackContact(String channel, String action) {
  //
  // <action> == "on"  contact is closed
  // <action> == "off" contact is open 
  //
  Integer val = timeDebounce();
  if (val) {
    // Debounce code to prevent multiple sequential Reed state change notifications due to oscillation
    Long timeNow = now();
    Long timeMax = state.timeStamp + val;
    if (timeNow < timeMax) {
      val = ((timeMax - timeNow) + 999) / 1000;
      logInfo("contact(${channel}, ${action}): debounce refresh scheduled in ${val} sec");

      runIn(val, refresh);
      return;
    }

    state.timeStamp = timeNow as Long;
  }

  logInfo("contact(${channel}, ${action})");

  if (action == "off") {
    // Contact open
    attributeSet("contact", "open");
  }
  else {
    // Contact closed
    attributeSet("contact", "closed");
  }
}

// -------------------------------------------------------------

private void shellyCallbackRelay(String channel, String action) {
  //
  // <action> == "on"  relay is on
  // <action> == "off" relay is off 
  //
  logInfo("relay(${channel}, ${action})");

  if (action == "off") {
    // Switch off
    attributeSet("switch", "off");
  }
  else {
    // Switch on
    attributeSet("switch", "on");
  }
}

// Commands --------------------------------------------------------------------------------------------------------------------

void on() {
  logInfo("on()");

  if (!isDeviceInitialized()) logError("on(): device not initialized");
  else {
    if (shellyCommandRelay(deviceAddress(), deviceChannel(), "on") == false) logError("on(): unable to communicate with device");
  }
}

// -------------------------------------------------------------

void off() {
  logInfo("off()");  

  if (!isDeviceInitialized()) logError("off(): device not initialized");
  else {
    if (shellyCommandRelay(deviceAddress(), deviceChannel(), "off") == false) logError("off(): unable to communicate with device");
  }
}

// -------------------------------------------------------------

void refresh() {
  logInfo("refresh()");

  if (!isDeviceInitialized()) logError("refresh(): device not initialized");
  else {
    Map status = shellyCommandStatus(deviceAddress(), deviceChannel());
    if (status == null) logError("refresh(): unable to communicate with device");
    else if (status.contact == null || status.relay == null) logError("refresh(): invalid device channel");
    else {
      // Update device attributes
      attributeSet("contact", status.contact? "closed": "open");      
      attributeSet("switch", status.relay? "on": "off");
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
    logError("installed(): ${e}");
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
    if (logLevel() > 2) runIn(1800, logDebugOff);

    // Initialize device with new preferences
    Map status = shellyCommandStatus(deviceAddress(), deviceChannel());
    if (status == null) logError("updated(): unable to communicate with device");
    else if (status.contact == null || status.relay == null) logError("updated(): invalid device channel");
    else {
      // Update device attributes
      attributeSet("contact", status.contact? "closed": "open");      
      attributeSet("switch", status.relay? "on": "off");

      // Set DNI if different
      if (status.mac != device.getDeviceNetworkId()) device.setDeviceNetworkId(status.mac);

      // Save the time the device as been properly initialized
      state.timeStamp = now() as Long;
    }
  }
  catch (Exception e) {
    logError("updated(): ${e}");
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
    logError("uninstalled(): ${e}");
  }
}

// -------------------------------------------------------------

void parse(String msg) {
  //
  // Called everytime a GET/POST message is received from the WiFi Gateway
  //
  try {
    // Parse GET/POST message
    Map data = parseLanMessage(msg);

    // Log raw data received from the device if trace is enabled
    logData("parse()", data);

    // Process the header and extract parameters from URL
    String header = data["header"];   
    List<String> token = header.tokenize(" ");
    if (token.size() > 1) {
      token = token[1].tokenize("/"); 
      if (token.size() > 2) {
        if (token[0] == "contact") shellyCallbackContact(token[1], token[2]);
        if (token[0] == "relay") shellyCallbackRelay(token[1], token[2]);    
      }
    }
  }
  catch (Exception e) {
    logError("parse(): ${e}");
  }
}

// Recycle ---------------------------------------------------------------------------------------------------------------------

/*

*/

// EOF -------------------------------------------------------------------------------------------------------------------------
