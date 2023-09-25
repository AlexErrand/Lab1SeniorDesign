// include the library code:
#include <LiquidCrystal.h>
#include <DHT11.h>
#include <AnalogButtons.h>
#include <SoftwareSerial.h>

// initialize the library by associating any needed LCD interface pin
// with the arduino pin number it is connected to
const int rs = 8, 
          en = 9, 
          d4 = 4, 
          d5 = 5, 
          d6 = 6, 
          d7 = 7;
LiquidCrystal lcd(rs, en, d4, d5, d6, d7);
const int buttonPin = 10;
const int backlight = 11;
int buttonState = 0;

DHT11 dht11(2);
SoftwareSerial BTSerial(0, 1); // object to read data from HC-06 Bluetooth device

unsigned long previousMS = 0;
unsigned long currentMS;

void setup() {
  lcd.begin(16, 2);              // set up the LCD's number of columns and rows:
  Serial.begin(9600);          // set baurd rate for temperature sensor
  BTSerial.begin(9600);          // set baud rate for HC-06 bluetooth device
  pinMode(buttonPin, INPUT);     // set input for button
  pinMode(backlight, OUTPUT);    // set an output for the LCD backlight
  digitalWrite(backlight, HIGH); // turn on backlight
}

void loop() {
  lcd.clear();
  float temperature = getTemperature();
  // we want to pulse the time in arduino rather than java code so grab time in ms
  currentMS = millis();
  // check every half second
  if (currentMS - previousMS >= 500) {
    previousMS = currentMS;
    temperature = getTemperature();
    // check is button is being pressed
    while(digitalRead(buttonPin) == LOW) {
      digitalWrite(backlight, HIGH); // turn on backlight
      lcd.display();

      if (temperature == -1 || temperature == 254) {
        printToLCD("     Error!     ", "Sensor not found");
      } else if (temperature != 253) {
        printToLCD("Temp:           ", String(temperature));
      } else {
        Serial.println("253: timeout error from sensor"); // we ignore this error code
      }
      // we double pulse every second to get a correct temperature value reading before sending to HC-06
      temperature = getTemperature();
      Serial.println(String(temperature));
      if (temperature == -1 || temperature == 254) {
        printToLCD("     Error!     ", "Sensor not found");
      } else if (temperature != 253) {
        printToLCD("Temp:           ", String(temperature));
        BTSerial.print(String(temperature) + "\n"); // send to HC-06 device
      } else {
        Serial.println("253: timeout error from sensor"); // we ignore this error code
      }
    }

    BTSerial.print(String(temperature) + "\n");
    BTSerial.write('A');
    lcd.clear();
    digitalWrite(backlight, LOW); // turn off backlight
  }
}

// helper functions
void printToLCD(String str1, String str2) {
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print(str1);

  lcd.setCursor(0,1);
  lcd.print(str2);
}

float getTemperature() {
  return dht11.readTemperature();
}