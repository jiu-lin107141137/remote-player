<!--
# remote-player
A repository for the final exam of MCU design<br>
This project is just for my final exam, so I didn't manage to ensure there is no bugs in my code.<br>
But they do work fine for me.

BlueToothTestWithAndroid12  --The raw code of the app.<br>
BlueToothTestWithAndroid12.zip  --The zip file of the app.<br>
blue_tooth_test.ino --The main program of Arduino<br>
music.h --The header file included in the main file<br>
-->
---
###### tag：`Arduino` `bluetooth` `buzzer`
---


# Arduino 電路實驗 藍芽電子琴-Remote player
[TOC]
* 系級：資工三乙
* 學號：1110832064
* 座號：16
* 姓名：王君翔


---

## 1.實驗目的  Purpose
透過操作電路，並實作程式碼，完成藍芽電子琴之設計。


---


## 2.實驗原理  Principle
先將藍芽模組之角色設定為master，之後將電路接好，並安裝APP。
在APP端與藍芽模組配對成功後，進入彈琴頁面，當按下APP上的按鈕後，傳遞訊息給藍芽模組，Arduino端便會讀取訊息，並使蜂鳴器發出相對應的頻率音高。
另外，也具有升降key的功能。

---


## 3.實驗材料 Materials

| 名稱<br>Name | 數量<br>Quantity | 備註<br>Memo |
| -------- | -------- | -------- |
| Arduino Uno R3 | 1 | 附USB線|
| 麵包板 | 1 |  |
| 杜邦線 | 7 | 公對公 |
| HC-05 | 1 | 主從一體 |
| 蜂鳴器 | 1 | 無源蜂鳴器 |
| Vivo V2027 | 1 | Android 12 ，附USB傳輸線|


---


## 4.實驗步驟 Steps
* APP使用android studio進行開發，限定環境為android12(含)以上

1. 進入AT mode，將HC-05進行設定，角色改為master

2. 依照電路圖連接電路
![](https://i.imgur.com/ZT2Q3Yz.png =500x300)
Δ電路圖 (Circuit Diagram)

3. 將以下程式完成後，透過USB線上傳至Uno板 
* 主程式
``` C++=
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
```
* music.h
```c++=
const float f[] = {
    16.35,    // C0
    17.32,    // C#0/Db0
    18.35,    // D0
    19.45,    // D#0/Eb0
    20.6,     // E0
    21.83,    // F0
    23.12,    // F#0/Gb0
    24.5,     // G0
    25.96,    // G#0/Ab0
    27.5,     // A0
    29.14,    // A#0/Bb0
    30.87,    // B0
    32.7,     // C1
    34.65,    // C#1/Db1
    36.71,    // D1
    38.89,    // D#1/Eb1
    41.2,     // E1
    43.65,    // F1
    46.25,    // F#1/Gb1
    49.0,     // G1
    51.91,    // G#1/Ab1
    55.0,     // A1
    58.27,    // A#1/Bb1
    61.74,    // B1
    65.41,    // C2
    69.3,     // C#2/Db2
    73.42,    // D2
    77.78,    // D#2/Eb2
    82.41,    // E2
    87.31,    // F2
    92.5,     // F#2/Gb2
    98.0,     // G2
    103.83,   // G#2/Ab2
    110.0,    // A2
    116.54,   // A#2/Bb2
    123.47,   // B2
    130.81,   // C3
    138.59,   // C#3/Db3
    146.83,   // D3
    155.56,   // D#3/Eb3
    164.81,   // E3
    174.61,   // F3
    185.0,    // F#3/Gb3
    196.0,    // G3
    207.65,   // G#3/Ab3
    220.0,    // A3
    233.08,   // A#3/Bb3
    246.94,   // B3
    261.63,   // C4
    277.18,   // C#4/Db4
    293.66,   // D4
    311.13,   // D#4/Eb4
    329.63,   // E4
    349.23,   // F4
    369.99,   // F#4/Gb4
    392.0,    // G4
    415.3,    // G#4/Ab4
    440.0,    // A4
    466.16,   // A#4/Bb4
    493.88,   // B4
    523.25,   // C5
    554.37,   // C#5/Db5
    587.33,   // D5
    622.25,   // D#5/Eb5
    659.25,   // E5
    698.46,   // F5
    739.99,   // F#5/Gb5
    783.99,   // G5
    830.61,   // G#5/Ab5
    880.0,    // A5
    932.33,   // A#5/Bb5
    987.77,   // B5
    1046.5,   // C6
    1108.73,  // C#6/Db6
    1174.66,  // D6
    1244.51,  // D#6/Eb6
    1318.51,  // E6
    1396.91,  // F6
    1479.98,  // F#6/Gb6
    1567.98,  // G6
    1661.22,  // G#6/Ab6
    1760.0,   // A6
    1864.66,  // A#6/Bb6
    1975.53,  // B6
    2093.0,   // C7
    2217.46,  // C#7/Db7
    2349.32,  // D7
    2489.02,  // D#7/Eb7
    2637.02,  // E7
    2793.83,  // F7
    2959.96,  // F#7/Gb7
    3135.96,  // G7
    3322.44,  // G#7/Ab7
    3520.0,   // A7
    3729.31,  // A#7/Bb7
    3951.07,  // B7
  };
const int mid = 24;
//const int scale[] = { 0, 2, 2, 1, 2, 2, 2, 1 }; // X full full half full full full half
const int perfix_scale[] = { 0, 2, 4, 5, 7, 9, 11};
int fix = 0;

double getF(int ind){
  ind += fix;
  int k = ind / 7 * 12;
  return f[((mid+k) + perfix_scale[((ind+14) % 7)])];
}

void keyUp(){
  fix++;
}

void keyDown(){
  fix--;
}
```
4. 將App完成並安裝至手機
App程式碼 [github連結](https://github.com/jiu-lin107141137/remote-player)

5. 執行並測試結果


---


## 5.實驗結果 Result
![](https://i.imgur.com/l7Y3DyY.png)
Δ接線完成圖

![](https://i.imgur.com/oMAjITB.png)
ΔAPP首頁

![](https://i.imgur.com/K0N612I.png)
Δ彈琴介面

當按下APP上的按鈕後，蜂鳴器可發出正確的音高，且升降key的功能有運作無誤，符合預期結果。

---

## 6.實驗心得 Reflection
這次透過藍芽完成App與Arduino的溝通，對我而言是一個非常嶄新的體驗，因為不管是Android端或是Arduino端，我都沒有自行開發藍芽功能的相關經驗。開始撰寫程式碼之後，才知道原來要注意的地方十分得多，像是一開始要進入AT mode設定藍芽模組的名稱、密碼以及角色，就花了一些時間來查閱資料並實作，更不用說之後App開發時遇到的各種Exception，以及Arduino在接收藍芽模組訊息的一些細節，整個專案實作下來，可謂獲益良多。
在完成藍芽模組的實作之後，接下來我想進行Wi-Fi模組的實作，透過Firebase完成遠端操控，觀察與藍芽模組的相異之處。


---
## 7.外部連結 Link
[My GitHub (程式碼)](https://github.com/jiu-lin107141137/remote-player)
