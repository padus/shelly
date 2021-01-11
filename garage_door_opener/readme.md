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
- 2 Solid Conductor Bell Wire

### Installation Instructions

This Hubitat integration is based on ["The Hook Up" video tutorial](https://www.youtube.com/watch?v=WEZUxXNiERQ).
However I reversed the Reed switch logic and location so that when the magnet comes in proximity of the switch, the garage door is 100% closed and not 100% open.

#### Shelly Switch:

1.  Set the Shelly input voltage to 12V with the dedicated jumper.
2.  Wire the Shelly switch as follow:

    <img src="https://github.com/mircolino/ecowitt/raw/master/images/D01.png" width="300" height="600">

3.  Install the Reed switch in a location where the magnet is in proximity of the switch when the garage door is fully closed.

#### Shelly Website:

#### Hubitat: 

***

### Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
