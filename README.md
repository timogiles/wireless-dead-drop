Wireless Digital Dead Drop
======

The Wireless Digital Dead Drop project is a new take on the old idea of a [Dead drop](https://en.wikipedia.org/wiki/Dead_drop).  The "dead drop" is an ESP8266 programmed to be a WIFI access point and a webserver.  A wifi device connects to the ESP8266 and accesses a website.  The ESP8266 only responds to one website request.  When that correct website is accessed, the ESP8266 loads a text file off of an SD card and sends it to the requesting WIFI device.

To make the project truly spygrade I've written a very simple android app.  When it is activated it will look for a specific WIFI SSID.  Upon finding that SSID it will force android to connect to the AP.  Once connected, the app tries to load a webpage from the specified addess and saves the results to a text file in the download folder.  

The process to use the Wireless Digital Dead Drop is simple
* Edit the Arduino Sketch with your own SSID, password and secret website
* Program the ESP8266
* Save a file named "Secret.txt" on the SD card
* Power on the ESP8266 in a hidden location
* Build the Anrdoid App with new defaults for the SSID, output file name, website URL
* Install and start the Android App
* Hit the "Activate" button in the app

That's it!  Once you are in range of the ESP8266 it will auto connect and download the file.

A pre-built version of the app can be found in DeadDropGrab/app/build/outputs/apk/

