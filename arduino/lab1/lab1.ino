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

const int rx = 13;
const int tx = 12;

DHT11 dht11(2);
SoftwareSerial BTSerial(rx, tx); // object to read data from HC-06 Bluetooth device

unsigned long previousMS = 0;
unsigned long currentMS;

float temperature = 0;

void setup() {
  lcd.begin(16, 2);              // set up the LCD's number of columns and rows:
  Serial.begin(9600);          // set baurd rate for temperature sensor
  BTSerial.begin(9600);          // set baud rate for HC-06 bluetooth device
  pinMode(buttonPin, INPUT);     // set input for button
  pinMode(backlight, OUTPUT);    // set an output for the LCD backlight
  pinMode(rx, INPUT);
  pinMode(tx, OUTPUT);
  digitalWrite(backlight, HIGH); // turn on backlight

  temperature = dht11.readTemperature();
}

void loop() {
  lcd.clear();
  // we want to pulse the time in arduino rather than java code so grab time in ms
  currentMS = millis();
  // check every half second
  if (currentMS - previousMS >= 500) {
    previousMS = currentMS;
    temperature = dht11.readTemperature();
    // check is button is being pressed
    while(digitalRead(buttonPin) == LOW) {
      digitalWrite(backlight, HIGH); // turn on backlight
      lcd.display();
      temperature = dht11.readTemperature();
      if (temperature == -1 || temperature == 254) {
        printToLCD("     Error!     ", "Sensor not found");
      } else if (temperature != 253) {
        printToLCD("Temp:           ", String(temperature));
        BTSerial.print(String(temperature) + "\n"); // send to HC-06 device
      } else {
        Serial.println("253: timeout error from sensor"); // we ignore this error code
        printToLCD("     Error!     ", "Sensor not found");
      }
      // delay similar to the if statement to only read in every so often
      delay(500);
    }
    BTSerial.print(String(temperature) + "\n");
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
