/*
 * BLE learning peripheral for the Android BLE Master app.
 * Board: any ESP32 (Arduino core 2.x or 3.x, library "BLE" bundled with the core).
 *
 * Service  4fafc201-...-c331914b
 *   Echo     beb5483e-...-26a8   READ | WRITE | WRITE_NO_RESPONSE   (store what you write, read it back)
 *   Counter  beb5483e-...-26a9   READ | NOTIFY                       (uint32 little-endian, +1 every second)
 */
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID  "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define ECHO_UUID     "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define COUNTER_UUID  "beb5483e-36e1-4688-b7f5-ea07361b26a9"

BLECharacteristic *echoChar;
BLECharacteristic *counterChar;
volatile bool deviceConnected = false;
uint32_t counter = 0;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *) override {
    deviceConnected = true;
    Serial.println("Central connected");
  }
  void onDisconnect(BLEServer *) override {
    deviceConnected = false;
    Serial.println("Central disconnected, advertising again");
    BLEDevice::startAdvertising();   // without this the board is invisible after the first connection
  }
};

class EchoCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *c) override {
    auto v = c->getValue();
    Serial.printf("Write received: %d byte(s)\n", (int)v.length());
  }
};

void setup() {
  Serial.begin(115200);
  BLEDevice::init("BLE-Learning-ESP32");
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService *service = server->createService(SERVICE_UUID);

  echoChar = service->createCharacteristic(
      ECHO_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  echoChar->setCallbacks(new EchoCallbacks());
  echoChar->setValue((uint8_t *)"hello", 5);

  counterChar = service->createCharacteristic(
      COUNTER_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  counterChar->addDescriptor(new BLE2902());   // creates the CCCD (0x2902) the phone writes to enable notifications

  service->start();

  BLEAdvertising *adv = BLEDevice::getAdvertising();
  adv->addServiceUUID(SERVICE_UUID);
  adv->setScanResponse(true);
  BLEDevice::startAdvertising();
  Serial.println("Advertising as BLE-Learning-ESP32");
}

void loop() {
  if (deviceConnected) {
    counter++;
    counterChar->setValue((uint8_t *)&counter, 4);
    counterChar->notify();
  }
  delay(1000);
}
