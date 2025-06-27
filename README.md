# 📡 NMEA MQTT Publisher Simulation

This Java application reads NMEA 0183 messages from a file, parses them into timestamped GPS sentences, and publishes them to an MQTT broker. It simulates real-time streaming by replaying recorded GPS messages at a per-second resolution.

---

## 📦 Project Structure

- `Main.java` — Main class that loads NMEA data and runs the MQTT publishing loop.
- `NmeaMessageParser.java` — Parses the input file, extracts timestamps, and groups messages by second.
- `mosaic.txt` The text file containing the NMEA messages
---

## 🚀 Features

- ⌛ Replays NMEA GPS messages in real-time (1-second interval)
- 📤 MQTT publishing to configurable topics
- 🧠 Smart grouping of NMEA messages per second using timestamp parsing
- 🔐 Supports username/password-based authentication to broker

---

## 🧰 Requirements

- Java 17+
- Maven 3.6+
- MQTT Broker (e.g. ActiveMQ, RabbitMQ)

---

## 📦 Maven Setup

This project uses Maven. The dependency to use MQTT is:

```xml
<dependency>
  <groupId>com.hivemq</groupId>
  <artifactId>hivemq-mqtt-client</artifactId>
  <version>1.3.7</version>
</dependency>
```

---

## ▶️ How to Run

### Build the project

```bash
mvn clean compile
```

### Run the simulation

```bash
mvn exec:java
```

Or with custom arguments:

```bash
mvn exec:java -Dexec.args="tcp://localhost:1883 admin admin producers/mycar/data ./mosaic.txt \"2023-09-21 07:40:56\" \"2023-09-21 07:59:00\""```
```

**Arguments:**
1. MQTT broker URL (default: `tcp://localhost:1883`)
2. MQTT username (default: `admin`)
3. MQTT password (default: `admin`)
4. Base topic (default: `producers/mycar/data`)
5. Path to the .txt file with NMEA messages (default: `./mosaic.txt`)
6. Start datetime (format: yyyy-MM-dd HH:mm:ss, default: `2023-09-21 07:40:56`)
7. End datetime (format: yyyy-MM-dd HH:mm:ss, default: `2023-09-21 07:59:00`)
---

## ⚠️ MQTT/STOMP Compatibility

Catalog Explorer communicates via the **STOMP** protocol and does **not** natively support MQTT.
To bridge MQTT messages into STOMP (so they appear in Catalog Explorer), your broker must support both protocols **and translate between them**.

### ✅ Recommended: Use ActiveMQ

If you use **ActiveMQ**, you can enable both MQTT and STOMP connectors. ActiveMQ will automatically and transparently map MQTT topics to STOMP destinations.

#### Example Mapping:
- MQTT topic: `producers/mycar/data` to
- STOMP topic: `/topic/producers.mycar.data`

This bridging lets MQTT publishers and STOMP consumers (like Catalog Explorer) work together seamlessly.

## 🛰️ Example MQTT Payload

```
$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
```

> Payloads are published exactly as parsed, ideal for backends that consume raw NMEA.

**Arguments:**
1. MQTT broker URL (default: `tcp://localhost:1883`)
2. MQTT username (default: `admin`)
3. MQTT password (default: `admin`)
4. Base topic (default: `producers/mycar/data`)

---

## 🔄 How It Works

1. NMEA messages are parsed from a file.
2. Each message's timestamp is extracted and used to group it into a 1-second slot.
3. Messages are replayed in a continuous loop, publishing to MQTT each second.
4. Topics are constructed with `baseTopic + "/trackId"`.

---

## 🛰️ Example MQTT NMEA Payload
Each MQTT message contains one NMEA message:

```
$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
```

> Payloads are published exactly as parsed, ideal for backends that consume raw NMEA.

---

## 📄 License

MIT License

---

## 🙌 Acknowledgments

- Built using the [HiveMQ MQTT Client](https://github.com/hivemq/hivemq-mqtt-client)
- Loosely based on real-world NYC ferry routes and pier coordinates
