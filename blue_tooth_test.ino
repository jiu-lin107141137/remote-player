#include <SoftwareSerial.h> 
#include "music.h"
#define BUZ 7
SoftwareSerial BT(10, 11);
void r();

void setup() {
  pinMode(BUZ, HIGH);
  
  Serial.begin(9600);
  Serial.println("BT is ready!");

  BT.begin(9600);
}

void loop() {
  if (BT.available()) {
    delay(10);
    r();
  }
}

void r(){
  char c = BT.read();
  int n = 0;
  if(c == 43){
    BT.read();
    keyUp();
    return;
  }
  if(c == 45){
    BT.read();
    keyDown();
    return;
  }
  if(!(c > 47 && c < 58)){
    return;
  }
  n = c&15;
  while((c = BT.read()) > 47 && c < 58){
    n *= 10;
    n += c & 15;
  }
  tone(BUZ, getF(n), 50);
}
