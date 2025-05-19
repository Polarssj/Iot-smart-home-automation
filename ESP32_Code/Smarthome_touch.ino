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
#include <AceButton.h>
using namespace ace_button;

const char *service_name = "Remote Lab Smart Board";
const char *pop = "Remote987";

// define the Chip Id
uint32_t espChipId = 0;

// define the Node Name
char nodeName[] = "Remote Lab Smart Board";

// define the Device Names
char deviceName_1[] = "Fan 1";
char deviceName_2[] = "Fan 2";
char deviceName_3[] = "Fan 3";
char deviceName_4[] = "Fan 4";
char deviceName_5[] = "Light 1";
char deviceName_6[] = "Light 2";
char deviceName_7[] = "Light 3";
char deviceName_8[] = "Light 4";
char deviceName_9[] = "Smart Door";

// define the GPIO connected with Relays and switches
static uint8_t RelayPin1 = 23;  //D23
static uint8_t RelayPin2 = 22;  //D22
static uint8_t RelayPin3 = 21;  //D21
static uint8_t RelayPin4 = 5;   //D5
static uint8_t RelayPin5 = 18;  //D18
static uint8_t RelayPin6 = 19;  //D19
static uint8_t RelayPin7 = 25;  //D25
static uint8_t RelayPin8 = 26;  //D26
static uint8_t MotorPin1 = 1;  //D26
static uint8_t MotorPin2 = 3;  //D26

static uint8_t SwitchPin1 = 13;  //D13
static uint8_t SwitchPin2 = 12;  //D12
static uint8_t SwitchPin3 = 14;  //D14
static uint8_t SwitchPin4 = 27;  //D27
static uint8_t SwitchPin5 = 33;  //D33
static uint8_t SwitchPin6 = 32;  //D32
static uint8_t SwitchPin7 = 15;  //D15
static uint8_t SwitchPin8 = 4;   //D4

static uint8_t wifiLed      = 2;  // D2
static uint8_t gpio_reset   = 0;  // Press BOOT for reset WiFi

static uint8_t RX2Pin       = 16;  // RX2
static uint8_t TX2Pin       = 17;  // TX2

/* Variable for reading pin status*/
bool toggleState_1 = LOW; //Define integer to remember the toggle state for relay 1
bool toggleState_2 = LOW; //Define integer to remember the toggle state for relay 2
bool toggleState_3 = LOW; //Define integer to remember the toggle state for relay 3
bool toggleState_4 = LOW; //Define integer to remember the toggle state for relay 4
bool toggleState_5 = LOW; //Define integer to remember the toggle state for relay 5
bool toggleState_6 = LOW; //Define integer to remember the toggle state for relay 6
bool toggleState_7 = LOW; //Define integer to remember the toggle state for relay 7
bool toggleState_8 = LOW; //Define integer to remember the toggle state for relay 8
bool toggleState_9 = LOW; //Define integer to remember the toggle state for relay 9
bool toggleState_10 = LOW; //Define integer to remember the toggle state for relay 10

String bt_data = ""; // variable for storing bluetooth data
bool motor_prov = 0;

ButtonConfig config1;
AceButton button1(&config1);
ButtonConfig config2;
AceButton button2(&config2);
ButtonConfig config3;
AceButton button3(&config3);
ButtonConfig config4;
AceButton button4(&config4);
ButtonConfig config5;
AceButton button5(&config5);
ButtonConfig config6;
AceButton button6(&config6);
ButtonConfig config7;
AceButton button7(&config7);
ButtonConfig config8;
AceButton button8(&config8);

void handleEvent1(AceButton*, uint8_t, uint8_t);
void handleEvent2(AceButton*, uint8_t, uint8_t);
void handleEvent3(AceButton*, uint8_t, uint8_t);
void handleEvent4(AceButton*, uint8_t, uint8_t);
void handleEvent5(AceButton*, uint8_t, uint8_t);
void handleEvent6(AceButton*, uint8_t, uint8_t);
void handleEvent7(AceButton*, uint8_t, uint8_t);
void handleEvent8(AceButton*, uint8_t, uint8_t);

//The framework provides some standard device types like switch, lightbulb, fan, temperature sensor.
static Fan my_switch1(deviceName_1, &RelayPin1);
static Fan my_switch2(deviceName_2, &RelayPin2);
static Fan my_switch3(deviceName_3, &RelayPin3);
static Fan my_switch4(deviceName_4, &RelayPin4);
static LightBulb my_switch5(deviceName_5, &RelayPin5);
static LightBulb my_switch6(deviceName_6, &RelayPin6);
static LightBulb my_switch7(deviceName_7, &RelayPin7);
static LightBulb my_switch8(deviceName_8, &RelayPin8);
static Switch my_switch9(deviceName_9, &MotorPin1);

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
        motor_prov = 1;
        Serial.println(WiFi.localIP());
        break;
    }
}

void write_callback(Device *device, Param *param, const param_val_t val, void *priv_data, write_ctx_t *ctx)
{
    const char *device_name = device->getDeviceName();
    const char *param_name = param->getParamName();

    if(strcmp(device_name, deviceName_1) == 0) {
      
      Serial.printf("Lightbulb = %s\n", val.val.b? "true" : "false");
      
      if(strcmp(param_name, "Power") == 0) {
          Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_1 = val.val.b;
        (toggleState_1 == false) ? digitalWrite(RelayPin1, HIGH) : digitalWrite(RelayPin1, LOW);
        param->updateAndReport(val);
        Serial2.println(toggleState_1 ? "A" : "a");
      }
      
    } else if(strcmp(device_name, deviceName_2) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_2 = val.val.b;
        (toggleState_2 == false) ? digitalWrite(RelayPin2, HIGH) : digitalWrite(RelayPin2, LOW);
        param->updateAndReport(val);
        Serial2.println(toggleState_2 ? "B" : "b");
      }
  
    } else if(strcmp(device_name, deviceName_3) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_3 = val.val.b;
        (toggleState_3 == false) ? digitalWrite(RelayPin3, HIGH) : digitalWrite(RelayPin3, LOW);
        param->updateAndReport(val);
        Serial2.println(toggleState_3 ? "C" : "c");
      }
  
    } else if(strcmp(device_name, deviceName_4) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_4 = val.val.b;
        (toggleState_4 == false) ? digitalWrite(RelayPin4, HIGH) : digitalWrite(RelayPin4, LOW);
        param->updateAndReport(val);
        Serial2.println(toggleState_4 ? "D" : "d");
      } 
       
    } else if(strcmp(device_name, deviceName_5) == 0) {
      
      Serial.printf("Lightbulb = %s\n", val.val.b? "true" : "false");
      
      if(strcmp(param_name, "Power") == 0) {
          Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_5 = val.val.b;
        (toggleState_5 == false) ? digitalWrite(RelayPin5, LOW) : digitalWrite(RelayPin5, HIGH);
        param->updateAndReport(val);
        Serial2.println(toggleState_5 ? "E" : "e");
      }
      
    } else if(strcmp(device_name, deviceName_6) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_6 = val.val.b;
        (toggleState_6 == false) ? digitalWrite(RelayPin6, LOW) : digitalWrite(RelayPin6, HIGH);
        param->updateAndReport(val);
        Serial2.println(toggleState_6 ? "F" : "f");
      }
  
    } else if(strcmp(device_name, deviceName_7) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_7 = val.val.b;
        (toggleState_7 == false) ? digitalWrite(RelayPin7, LOW) : digitalWrite(RelayPin7, HIGH);
        param->updateAndReport(val);
        Serial2.println(toggleState_7 ? "G" : "g");
      }
  
    } else if(strcmp(device_name, deviceName_8) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_8 = val.val.b;
        (toggleState_8 == false) ? digitalWrite(RelayPin8, LOW) : digitalWrite(RelayPin8, HIGH);
        param->updateAndReport(val);
        Serial2.println(toggleState_8 ? "H" : "h");
      }  
    } else if(strcmp(device_name, deviceName_9) == 0) {
      
      Serial.printf("Switch value = %s\n", val.val.b? "true" : "false");

      if(strcmp(param_name, "Power") == 0) {
        Serial.printf("Received value = %s for %s - %s\n", val.val.b? "true" : "false", device_name, param_name);
        toggleState_9 = val.val.b;
        (toggleState_9 == false) ? door_close() : door_open();
        param->updateAndReport(val);
        Serial2.println(toggleState_8 ? "J" : "j");
      }  
    } 
}

void bluetooth_control()
{
  if(Serial2.available()) {
    bt_data = Serial2.readString();
    Serial.println(bt_data.substring(bt_data.lastIndexOf(",") + 1));
    
    if (bt_data.substring(bt_data.lastIndexOf(",") + 1) == "A"){
      digitalWrite(RelayPin1, LOW);  toggleState_1 = 1; // if "A1" received Turn on Relay1
      my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_1);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "a"){
      digitalWrite(RelayPin1, HIGH);  toggleState_1 = 0; // if "A0" received Turn off Relay1
      my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_1);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "B"){
      digitalWrite(RelayPin2, LOW);  toggleState_2 = 1; // if "B1" received Turn on Relay2
      my_switch2.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_2);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "b"){
      digitalWrite(RelayPin2, HIGH);  toggleState_2 = 0; // if "B0" received Turn off Relay2
      my_switch2.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_2);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "C"){
      digitalWrite(RelayPin3, LOW);  toggleState_3 = 1; // if "C1" received Turn on Relay3
      my_switch3.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_3);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "c"){
      digitalWrite(RelayPin3, HIGH);  toggleState_3 = 0; // if "C0" received Turn off Relay3
      my_switch3.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_3);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "D"){
      digitalWrite(RelayPin4, LOW);  toggleState_4 = 1; // if "D1" received Turn on Relay4
      my_switch4.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_4);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "d"){
      digitalWrite(RelayPin4, HIGH);  toggleState_4 = 0; // if "D0" received Turn off Relay4
      my_switch4.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_4);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "E"){
      digitalWrite(RelayPin5, HIGH);  toggleState_5 = 1; // if "E1" received Turn on Relay5
      my_switch5.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_5);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "e"){
      digitalWrite(RelayPin5, LOW);  toggleState_5 = 0; // if "E0" received Turn off Relay5
      my_switch5.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_5);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "F"){
      digitalWrite(RelayPin6, HIGH);  toggleState_6 = 1; // if "F1" received Turn on Relay6
      my_switch6.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_6);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "f"){
      digitalWrite(RelayPin6, LOW);  toggleState_6 = 0; // if "F0" received Turn off Relay6
      my_switch6.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_6);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "G"){
      digitalWrite(RelayPin7, HIGH);  toggleState_7 = 1; // if "G1" received Turn on Relay7
      my_switch7.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_7);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "g"){
      digitalWrite(RelayPin7, LOW);  toggleState_7 = 0; // if "G0" received Turn off Relay7
      my_switch7.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_7);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "H"){
      digitalWrite(RelayPin8, HIGH);  toggleState_8 = 1; // if "H1" received Turn on Relay8
      my_switch8.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_8);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "h"){
      digitalWrite(RelayPin8, LOW);  toggleState_8 = 0; // if "H0" received Turn off Relay8
      my_switch8.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_8);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "J") {
      door_open();
      my_switch9.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_9);
    } 
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "j") {
      door_close();
      my_switch9.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_9);
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "Z1"){    // if "Z1" received Turn on all Relays
      all_SwitchOn();
    }
    else if(bt_data.substring(bt_data.lastIndexOf(",") + 1) == "Z0"){    // if "Z0" received Turn off all Relays
      all_SwitchOff();
    }
    else if (bt_data.substring(bt_data.lastIndexOf(",") + 1) == "I"){
      // Define a function to convert a byte to its binary representation as a string

// Pack the toggle states into a single byte
byte digitalData = 0;
digitalData |= toggleState_1 ? 0b100000000 : 0;
digitalData |= toggleState_2 ? 0b010000000 : 0;
digitalData |= toggleState_3 ? 0b001000000 : 0;
digitalData |= toggleState_4 ? 0b000100000 : 0;
digitalData |= toggleState_5 ? 0b000010000 : 0;
digitalData |= toggleState_6 ? 0b000001000 : 0;
digitalData |= toggleState_7 ? 0b000000100 : 0;
digitalData |= toggleState_8 ? 0b000000010 : 0;
digitalData |= toggleState_9 ? 0b000000001 : 0;

// Convert the digital data byte into a binary string
String digitalDataString = byteToBinaryString(digitalData);

// Send the binary string via Serial2
Serial2.println(digitalDataString);

    }
  } 
}

void door_open() {
  toggleState_9 = 1;
  toggleState_10 = 0;
  digitalWrite(MotorPin1, HIGH);
  digitalWrite(MotorPin2, LOW);
  delay(6600);
  toggleState_9 = 0;
  toggleState_10 = 0;
  digitalWrite(MotorPin1, LOW);
  digitalWrite(MotorPin2, LOW);
}

void door_close() {
  toggleState_9 = 0;
  toggleState_10 = 1;
  digitalWrite(MotorPin1, LOW);
  digitalWrite(MotorPin2, HIGH);
  delay(6600);
  toggleState_9 = 0;
  toggleState_10 = 0;
  digitalWrite(MotorPin1, LOW);
  digitalWrite(MotorPin2, LOW);
}

void all_SwitchOff(){
  toggleState_1 = 0; digitalWrite(RelayPin1, HIGH); my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_1); delay(100);
  toggleState_2 = 0; digitalWrite(RelayPin2, HIGH); my_switch2.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_2); delay(100);
  toggleState_3 = 0; digitalWrite(RelayPin3, HIGH); my_switch3.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_3); delay(100);
  toggleState_4 = 0; digitalWrite(RelayPin4, HIGH); my_switch4.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_4); delay(100);
  toggleState_5 = 0; digitalWrite(RelayPin5, LOW); my_switch5.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_5); delay(100);
  toggleState_6 = 0; digitalWrite(RelayPin6, LOW); my_switch6.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_6); delay(100);
  toggleState_7 = 0; digitalWrite(RelayPin7, LOW); my_switch7.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_7); delay(100);
  toggleState_8 = 0; digitalWrite(RelayPin8, LOW); my_switch8.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_8); delay(100);
}

void all_SwitchOn(){
  toggleState_1 = 1; digitalWrite(RelayPin1, LOW); my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_1); delay(100);
  toggleState_2 = 1; digitalWrite(RelayPin2, LOW); my_switch2.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_2); delay(100);
  toggleState_3 = 1; digitalWrite(RelayPin3, LOW); my_switch3.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_3); delay(100);
  toggleState_4 = 1; digitalWrite(RelayPin4, LOW); my_switch4.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_4); delay(100);
  toggleState_5 = 1; digitalWrite(RelayPin5, HIGH); my_switch5.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_5); delay(100);
  toggleState_6 = 1; digitalWrite(RelayPin6, HIGH); my_switch6.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_6); delay(100);
  toggleState_7 = 1; digitalWrite(RelayPin7, HIGH); my_switch7.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_7); delay(100);
  toggleState_8 = 1; digitalWrite(RelayPin8, HIGH); my_switch8.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_8); delay(100);
}

void setup()
{   
    Serial.begin(115200);
    Serial2.begin(9600, SERIAL_8N1, RX2Pin, TX2Pin);
    
    // Set the Relays GPIOs as output mode
    pinMode(RelayPin1, OUTPUT);
    pinMode(RelayPin2, OUTPUT);
    pinMode(RelayPin3, OUTPUT);
    pinMode(RelayPin4, OUTPUT);
    pinMode(RelayPin5, OUTPUT);
    pinMode(RelayPin6, OUTPUT);
    pinMode(RelayPin7, OUTPUT);
    pinMode(RelayPin8, OUTPUT);  
    pinMode(wifiLed, OUTPUT);
    
    // Configure the input GPIOs
    pinMode(SwitchPin1, INPUT_PULLUP);
    pinMode(SwitchPin2, INPUT_PULLUP);
    pinMode(SwitchPin3, INPUT_PULLUP);
    pinMode(SwitchPin4, INPUT_PULLUP);
    pinMode(SwitchPin5, INPUT_PULLUP);
    pinMode(SwitchPin6, INPUT_PULLUP);
    pinMode(SwitchPin7, INPUT_PULLUP);
    pinMode(SwitchPin8, INPUT_PULLUP);
    pinMode(gpio_reset, INPUT);
    
    // Write to the GPIOs the default state on booting
    digitalWrite(RelayPin1, !toggleState_1);
    digitalWrite(RelayPin2, !toggleState_2);
    digitalWrite(RelayPin3, !toggleState_3);
    digitalWrite(RelayPin4, !toggleState_4);
    digitalWrite(RelayPin5, toggleState_5);
    digitalWrite(RelayPin6, toggleState_6);
    digitalWrite(RelayPin7, toggleState_7);
    digitalWrite(RelayPin8, toggleState_8);
    digitalWrite(MotorPin1, toggleState_9);
    digitalWrite(MotorPin2, toggleState_10);
    digitalWrite(wifiLed, LOW);

    config1.setEventHandler(button1Handler);
    config2.setEventHandler(button2Handler);
    config3.setEventHandler(button3Handler);
    config4.setEventHandler(button4Handler);
    config5.setEventHandler(button5Handler);
    config6.setEventHandler(button6Handler);
    config7.setEventHandler(button7Handler);
    config8.setEventHandler(button8Handler);
    
    button1.init(SwitchPin1);
    button2.init(SwitchPin2);
    button3.init(SwitchPin3);
    button4.init(SwitchPin4);
    button5.init(SwitchPin5);
    button6.init(SwitchPin6);
    button7.init(SwitchPin7);
    button8.init(SwitchPin8);

    Node my_node;    
    my_node = RMaker.initNode(nodeName);

    //Standard switch device
    my_switch1.addCb(write_callback);
    my_switch2.addCb(write_callback);
    my_switch3.addCb(write_callback);
    my_switch4.addCb(write_callback);
    my_switch5.addCb(write_callback);
    my_switch6.addCb(write_callback);
    my_switch7.addCb(write_callback);
    my_switch8.addCb(write_callback);
    my_switch9.addCb(write_callback);

    //Add switch device to the node   
    my_node.addDevice(my_switch1);
    my_node.addDevice(my_switch2);
    my_node.addDevice(my_switch3);
    my_node.addDevice(my_switch4);
    my_node.addDevice(my_switch5);
    my_node.addDevice(my_switch6);
    my_node.addDevice(my_switch7);
    my_node.addDevice(my_switch8);
    my_node.addDevice(my_switch9);
    
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
    my_switch2.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    my_switch3.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    my_switch4.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    my_switch5.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    my_switch6.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    my_switch7.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    my_switch8.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
    my_switch9.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, false);
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
   


    
    button1.check();
    button2.check();
    button3.check();
    button4.check();
    button5.check();
    button6.check();
    button7.check();
    button8.check();

    if(motor_prov){
      pinMode(MotorPin1, OUTPUT);  
      pinMode(MotorPin2, OUTPUT); 
      motor_prov = 0;
    }
    if(motor_prov){
      pinMode(MotorPin1, OUTPUT);  
      pinMode(MotorPin2, OUTPUT); 
      motor_prov = 0;
    }
}

String byteToBinaryString(byte b) {
    String binaryString = "";
    for (int i = 8; i >= 0; --i) {
      binaryString += ((b >> i) & 1) ? '1' : '0';
   }
    return binaryString;
  }

void button1Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT1");
  switch (eventType) {
    case AceButton::kEventPressed:
      digitalWrite(RelayPin1, toggleState_1);
      toggleState_1 = !toggleState_1;
      my_switch1.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_1);
      Serial2.println(toggleState_1 ? "A" : "a");
      break;
  }
}
void button2Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT2");
  switch (eventType) {
    case AceButton::kEventPressed:
      digitalWrite(RelayPin2, toggleState_2);
      toggleState_2 = !toggleState_2;
      my_switch2.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_2);
      Serial2.println(toggleState_2 ? "B" : "b");
      break;
  }
}
void button3Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT3");
  switch (eventType) {
    case AceButton::kEventPressed:
      digitalWrite(RelayPin3, toggleState_3);
      toggleState_3 = !toggleState_3;
      my_switch3.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_3);
      Serial2.println(toggleState_3 ? "C" : "c");
      break;
  }
}
void button4Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT4");
  switch (eventType) {
    case AceButton::kEventPressed:
      digitalWrite(RelayPin4, toggleState_4);
      toggleState_4 = !toggleState_4;
      my_switch4.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_4);
      Serial2.println(toggleState_4 ? "D" : "d");
      break;
  }
}
void button5Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT5");
  switch (eventType) {
    case AceButton::kEventPressed:
      toggleState_5 = !toggleState_5;
      digitalWrite(RelayPin5, toggleState_5);
      my_switch5.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_5);
      Serial2.println(toggleState_5 ? "E" : "e");
      break;
  }
}
void button6Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT6");
  switch (eventType) {
    case AceButton::kEventPressed:
      toggleState_6 = !toggleState_6;
      digitalWrite(RelayPin6, toggleState_6);
      my_switch6.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_6);
      Serial2.println(toggleState_6 ? "F" : "f");
      break;
  }
}
void button7Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT7");
  switch (eventType) {
    case AceButton::kEventPressed:
      toggleState_7 = !toggleState_7;
      digitalWrite(RelayPin7, toggleState_7);
      my_switch7.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_7);
      Serial2.println(toggleState_7 ? "G" : "g");
      break;
  }
}
void button8Handler(AceButton* button, uint8_t eventType, uint8_t buttonState) {
  Serial.println("EVENT8");
  switch (eventType) {
    case AceButton::kEventPressed:
      toggleState_8 = !toggleState_8;
      digitalWrite(RelayPin8, toggleState_8);
      my_switch8.updateAndReportParam(ESP_RMAKER_DEF_POWER_NAME, toggleState_8);
      Serial2.println(toggleState_8 ? "H" : "h");
      break;
  }
}
