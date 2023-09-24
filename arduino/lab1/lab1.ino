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

void setup() {
  lcd.begin(16, 2);              // set up the LCD's number of columns and rows:
  Serial.begin(115200);          // set baurd rate for temperature sensor
  BTSerial.begin(9600);          // set baud rate for HC-06 bluetooth device
  pinMode(buttonPin, INPUT);     // set input for button
  pinMode(backlight, OUTPUT);    // set an output for the LCD backlight
  digitalWrite(backlight, HIGH); // turn on backlight

  getTemperature(); // grab temperature froms sensor
}

void loop() {
  // set the cursor to column 0, line 1
  // (note: line 1 is the second row, since counting begins with 0):
  buttonState = digitalRead(buttonPin);

  if (Serial.available() > 0) {
    
    char command = Serial.read();
    Serial.println(Serial.read());
    if (command == 'ON') {
      digitalWrite(buttonState, LOW); // Turn on the sensor
    } else if (command == 'OFF') {
      digitalWrite(buttonState, HIGH); // Turn off the sensor
    }

    if (command == 0) {
      digitalWrite(buttonState, LOW); // Turn on the sensor
    } else if (command == 1) {
      digitalWrite(buttonState, HIGH); // Turn off the sensor
    }
  }

  float temperature = dht11.readTemperature();

  String temp2Java = String(temperature);

  if(temperature!= -1 && temperature != 253.0){
    temp2Java = String(temperature);
    Serial.println(temp2Java); 
  }

  if (buttonState == LOW) {
    digitalWrite(backlight, HIGH); // turn on backlight

    lcd.setCursor(0, 1);
    lcd.display();
    // print the number of seconds since reset:
    if(temperature!= -1 && temperature != 253.0){
      lcd.print("temp: ");
      lcd.print(temperature);
      temp2Java = String(temperature);
      Serial.println(temp2Java); 
    }
  }
  else {
    lcd.clear();
    digitalWrite(backlight, LOW); // turn on backlight
  }

}

// helper function
void getTemperature() {

}