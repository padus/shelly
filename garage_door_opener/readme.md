## Shelly Garage Door Opener
*A WiFi garage door opener for Hubitat Elevation using common Shelly and Reed switches*

### Features

- Inexpensive Shelly and Reed switches.
- Easy web-based WiFi setup.
- No z-wave inclusion/exclusion network nightmares.
- No batteries.
- No tilt sensor false positives.
- Accurate and reliable operation.

### Parts

- Allterco Shelly Switch
- 12V Power Adapter
- Normally Open (NO) Reed Switch
- Solid 2-Conductor Bell Wire

### Installation Instructions

This Hubitat integration is based on ["The Hook Up" video tutorial](https://www.youtube.com/watch?v=WEZUxXNiERQ).<br>
However I reversed the Reed switch logic and location so that when the magnet comes in proximity of the switch, the garage door is 100% closed and not 100% open.

#### Shelly Switch:

1.  Set the Shelly input voltage to 12V with the dedicated jumper.
2.  Wire the Shelly switch as follow:

    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/01_wiring.png" width="50%" height="50%">

3.  Install the Reed switch in a location where the magnet is in proximity of the switch when the garage door is fully closed.

#### Shelly Website:

1.  Set the Shelly button type to "detached" so that the internal relay and the external Reed switch will have independent states:

    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/02_detached.png" width="40%" height="40%">

2.  Add a 1 sec auto-off timer to emulate pressing the garage physical button:

    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/03_timer.png" width="40%" height="40%">

3.  Add Hubitat callbacks so that the Shelly switch can notify Hubitat when either the internal relay or the external Reed switch changes state:

    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/04_contact_on.png" width="40%" height="40%"><br>
    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/05_contact_off.png" width="40%" height="40%"><br>
    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/06_relay_on.png" width="40%" height="40%"><br>
    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/07_relay_off.png" width="40%" height="40%">

#### Hubitat Website:

1.  Add the Shelly Garage Door Opener [source code](https://https://raw.githubusercontent.com/mircolino/shelly/main/garage_door_opener/driver.groovy) to the Hubitat "Drivers Code" page.

2.  Create a new Virtual Device, select type: "Shelly Garage Door Opener" and press &lt;Save Device&gt;:

    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/08_new_device.png" width="50%" height="50%">

3.  Open the "Shelly Garage Door Opener" device page, enter the Shelly switch ip address or hostname, the login credentials (if any) and press &lt;Save Preferences&gt;:

    <img src="https://github.com/mircolino/shelly/raw/main/garage_door_opener/images/09_garage_door_device.png" width="50%" height="50%">

The Shelly Garage Door Opener and the Hubitat Integration should now be fully operational.

***

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
