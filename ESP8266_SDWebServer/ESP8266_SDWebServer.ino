/*
Written by Tim Giles, 2/9/2017
  this is mostly just example code that I combined to do my bidding.
  I borrowed from the ESP8266/DNSserver and ESP8266/SDWebServer examples 
  inluded with the ESP8266 arduino library
*/

#include <ESP8266WiFi.h>
//#include <DNSServer.h>
#include "./DNSServer.h"
#include <ESP8266WebServer.h>
#include <SD.h>


const int chipSelect = 15;

const byte DNS_PORT = 53;
IPAddress apIP(192, 168, 1, 1);
DNSServer dnsServer;
ESP8266WebServer webServer(80);

const char *ssid = "ESPsf";           //AP SSID
const char *password = "password";    //AP password
const char *host = "www.test.com";  //the only site that will work
const char *filename = "/Secret.txt"; //file to load from SD card

String getContentType(String filename){
  if(webServer.hasArg("download")) return "application/octet-stream";
  else if(filename.endsWith(".htm")) return "text/html";
  else if(filename.endsWith(".html")) return "text/html";
  else if(filename.endsWith(".css")) return "text/css";
  else if(filename.endsWith(".js")) return "application/javascript";
  else if(filename.endsWith(".png")) return "image/png";
  else if(filename.endsWith(".gif")) return "image/gif";
  else if(filename.endsWith(".jpg")) return "image/jpeg";
  else if(filename.endsWith(".ico")) return "image/x-icon";
  else if(filename.endsWith(".xml")) return "text/xml";
  else if(filename.endsWith(".pdf")) return "application/x-pdf";
  else if(filename.endsWith(".zip")) return "application/x-zip";
  else if(filename.endsWith(".gz")) return "application/x-gzip";
  return "text/plain";
}

char* StringToCharArray(String input){
  char* output = ""; 
  input.toCharArray(output,input.length()+1);
  return(output);
}

//read the file from the SD card and serve it to the user
bool handleFileRead(String path){
  Serial.println("handleFileRead: " + path);
  String contentType = getContentType(path);
  Serial.println(StringToCharArray(path));
  if(SD.exists(StringToCharArray(path))){
    File file = SD.open(path, FILE_READ);
    size_t sent = webServer.streamFile(file, contentType);
    file.close();
    return true;
  }
  return false;
}

//try to read the file "filename" from the SD card and send it to the user
//if the SD card read fails, send text saying that the SD card couldn't be read
void handleRoot() {
  if(!handleFileRead(filename)){
    //if the file read fails, try initializing the SD card again
    initializeSD();
    if(SD.exists(StringToCharArray(filename))){
      Serial.println("reinitialization successful!");
    }
    if(!handleFileRead(filename)){
      //the read failed twice, give up
      webServer.send(404, "text/plain", "Couldn't read file from SD card");
    }
  }
}

// see if the card is present and can be initialized:
void initializeSD(){
  //there is an issue with SD library so that when an SDcard is removed and then replaced, SD.begin will fail even if 
  //reinitialization has been successful.
  if (!SD.begin(chipSelect)) {
    Serial.println("Card initialization failed");
  }
  else{
    Serial.println("card initialized.");
  }
}

void setup() {
  Serial.begin(115200);
  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(apIP, apIP, IPAddress(255, 255, 255, 0));
  WiFi.softAP(ssid,password);
  //dnsServer.setTTL(300);
  //dnsServer.setErrorReplyCode(DNSReplyCode::ServerFailure);

  // start DNS server for *host which was declared above
  dnsServer.start(DNS_PORT, host, apIP);

  // simple HTTP server to see that DNS server is working
  webServer.onNotFound([]() {
    String message = "Not Found!\n\n";
    message += "URI: ";
    message += webServer.uri();
    webServer.send(200, "text/plain", message);
  });

  //respond to correctly addressed web request
  webServer.on("/", handleRoot);

  //start the web server
  webServer.begin();
  Serial.println("HTTP server started");

  //initialize SD card
  Serial.print("Initializing SD card...");
  initializeSD();
  
}

void loop() {
  dnsServer.processNextRequest();
  webServer.handleClient();
}
