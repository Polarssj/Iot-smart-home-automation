/**********************************************************************************
 *  TITLE: ESP RainMaker + Bluetooth + Manual Switch control 8 Relays using ESP32 (Real time feedback + no WiFi control)
 *  Preferences--> Aditional boards Manager URLs : 
 *  http://arduino.esp8266.com/stable/package_esp8266com_index.json,https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
 *  Download Board ESP32 (2.0.3): https://github.com/espressif/arduino-esp32
 *  Download the libraries 
 *  AceButton Library (1.9.2): https://github.com/bxparks/AceButton
 *  To Erase Code: python -m esptool --chip esp32 --port COM5 --baud 115200 --after hard_reset erase_flash
 *  To Flash ESP32: python -m esptool --port COM5 write_flash_status --non-volatile 0
 **********************************************************************************/

#include "RMaker.h"
#include "WiFi.h"
#include "WiFiProv.h"

const char *service_name = "Remote Lab Smart Door";
const char *pop = "Remote987";

// define the Chip Id
uint32_t espChipId = 1;

// define the Node Name
char nodeName[] = "Remote Lab Smart Door";

// define the Device Names
char deviceName_1[] = "Smart Door";

// define the GPIO connected with Relays and switches
static uint8_t MotorPin1 = 23;  //D23
static uint8_t MotorPin2 = 22;  //D22
static uint8_t LockPin = 21;  //D21

static uint8_t wifiLed      = 2;  // D2
static uint8_t gpio_reset   = 0;  // Press BOOT for reset WiFi

static uint8_t RX2Pin       = 16;  // RX2
static uint8_t TX2Pin       = 17;  // TX2

/* Variable for reading pin status*/
bool toggleState = LOW; //Define integer to remember the toggle state for Motor

String bt_data = ""; // variable for storing bluetooth data

//The framework provides some standard device types like switch, lightbulb, fan, temperature sensor.
static Switch my_switch1(deviceName_1, &MotorPin1);

void sysProvEvent(arduino_event_t *sys_event)
{
    switch (sys_event->event_id) {      
        case ARDUINO_EVENT_PROV_START:
#if CONFIG_IDF_TARGET_ESP32
        Serial.printf("\nProvisioning Started with name \"%s\" and PoP \"%s\" on BLE\n", service_name, pop);
        printQR(service_name, pop, "ble");
#else
        Serial.printf("\nProvisioning Started with name \"%s\" and PoP \"%s\" on SoftAP\n", service_name, pop);
        printQR(service_name, pop, "softap");
#endif        
        break;
        case ARDUINO_EVENT_WIFI_STA_CONNECTED:
        Serial.printf("\nConnected to Wi-Fi!\n");
        digitalWrite(wifiLed, true);
        Serial.println(WiFi.localIP());
        break;
    }
}

void write_callback(Device *device, Param *param, const param_val_t val, void *priv_data, write_ctx_t *ctx)
{
    const char *device_name = device->getDeviceName();
    const char *param_name = param->getParamName();

     if(strcmp(device_name, deviceName_1) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState = val.val.b;
        Serial2.println(toggleState ? "J" : "j");
        (toggleState == false) ? door_close() : door_open();
        param->updateAndReport(val);
      }  
    }
}

void door_open() {
  digitalWrite(LockPin, HIGH);
  delay(500);
  digitalWrite(MotorPin1, HIGH);
  digitalWrite(MotorPin2, LOW);
  delay(1500);
  digitalWrite(LockPin, LOW);
  delay(5500);
  digitalWrite(MotorPin1, LOW);
  digitalWrite(MotorPin2, LOW);
}

void door_close() {
  digitalWrite(MotorPin1, LOW);
  digitalWrite(MotorPin2, HIGH);
  delay(7500);
  digitalWrite(MotorPin1, LOW);
  digitalWrite(MotorPin2, LOW);
}

void setup()
{   
    Serial.begin(115200);
    Serial2.begin(9600, SERIAL_8N1, RX2Pin, TX2Pin);
    
    // Set the Relays GPIOs as output mode
    pinMode(MotorPin1, OUTPUT);
    pinMode(MotorPin2, OUTPUT);
    pinMode(LockPin, OUTPUT);
    pinMode(wifiLed, OUTPUT);
    
    // Configure the input GPIOs
    pinMode(gpio_reset, INPUT);
    
    // Write to the GPIOs the default state on booting
    digitalWrite(MotorPin1, LOW);
    digitalWrite(MotorPin2, LOW);
    digitalWrite(LockPin, LOW);
    digitalWrite(wifiLed, LOW);

    Node my_node;    
    my_node = RMaker.initNode(nodeName);

    //Standard switch device
    my_switch1.addCb(write_callback);

    //Add switch device to the node   
    my_node.addDevice(my_switch1);
    
    //This is optional 
    RMaker.enableOTA(OTA_USING_PARAMS);
    //If you want to enable scheduling, set time zone for your region using setTimeZone(). 
    //The list of available values are provided here https://rainmaker.espressif.com/docs/time-service.html
    // RMaker.setTimeZone("Asia/Shanghai");
    // Alternatively, enable the Timezone service and let the phone apps set the appropriate timezone
    RMaker.enableTZService();
    RMaker.enableSchedule();

    //Service Name
    for(int i=0; i<17; i=i+8) {
      espChipId |= ((ESP.getEfuseMac() >> (40 - i)) & 0xff) << i;
    }

    Serial.printf("\nChip ID:  %d Service Name: %s\n", espChipId, service_name);

    Serial.printf("\nStarting ESP-RainMaker\n");
    RMaker.start();

    WiFi.onEvent(sysProvEvent);
#if CONFIG_IDF_TARGET_ESP32
    WiFiProv.beginProvision(WIFI_PROV_SCHEME_BLE, WIFI_PROV_SCHEME_HANDLER_FREE_BTDM, WIFI_PROV_SECURITY_1, pop, service_name);
#else
    WiFiProv.beginProvision(WIFI_PROV_SCHEME_SOFTAP, WIFI_PROV_SCHEME_HANDLER_NONE, WIFI_PROV_SECURITY_1, pop, service_name);
#endif

    my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    }

unsigned long previousMillis=0;
unsigned long interval=10000;

void loop()
{
  unsigned long currentMillis=millis();
  if((WiFi.status()!=WL_CONNECTED)&&(currentMillis-previousMillis>=interval))
  {
    Serial.print(millis());
    WiFi.disconnect();
    WiFi.reconnect();
    previousMillis=currentMillis;
  }
    // Read GPIO0 (external button to reset device
    if(digitalRead(gpio_reset) == LOW) { //Push button pressed
        Serial.printf("Reset Button Pressed!\n");
        // Key debounce handling
        delay(100);
        int startTime = millis();
        while(digitalRead(gpio_reset) == LOW) delay(50);
        int endTime = millis();

        if ((endTime - startTime) > 10000) {
          // If key pressed for more than 10secs, reset all
          Serial.printf("Reset to factory.\n");
          RMakerFactoryReset(2);
        } else if ((endTime - startTime) > 3000) {
          Serial.printf("Reset Wi-Fi.\n");
          // If key pressed for more than 3secs, but less than 10, reset Wi-Fi
          RMakerWiFiReset(2);
        }
    }
    delay(100);

    if (WiFi.status() != WL_CONNECTED)
    {
      //Serial.println("WiFi Not Connected");
      digitalWrite(wifiLed, false);
    }
    else
    {
      //Serial.println("WiFi Connected");
      digitalWrite(wifiLed, true);
     
    }

    bluetooth_control();
}

void bluetooth_control()
{
  if(Serial2.available()) {
    bt_data = Serial2.readString();
    Serial.println(bt_data.substring(bt_data.lastIndexOf(",") + 1));
  
    if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "J") {
      toggleState = 1;
      my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState);
      door_open();
    } 
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "j") {
      toggleState = 0;
      my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState);
      door_close();
    }
    else if (bt_data.substring(bt_data.lastIndexOf(",") + 1) == "I"){
      // Define a function to convert a byte to its binary representation as a string

// Pack the toggle states into a single byte
byte digitalData = 0;
digitalData |= toggleState ? 0b000000001 : 0;

// Convert the digital data byte into a binary string
String digitalDataString = byteToBinaryString(digitalData);

// Send the binary string via Serial2
Serial2.println(digitalDataString);

    }
  } 
}

String byteToBinaryString(byte b) {
    String binaryString = "";
    for (int i = 8; i >= 0; --i) {
      binaryString += ((b >> i) & 1) ? '1' : '0';
   }
    return binaryString;
}