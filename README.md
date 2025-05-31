# Execution of the Distributed Solution

This document explains how to compile and run the distributed system, both locally and over a public network.

Master in Information Security

---

## Demonstration

Watch the execution example on YouTube:

[Demonstration Video](https://www.youtube.com/watch?v=VD3HqWd1Iuk)

---

## Prerequisites

- Java 21
- Maven (`mvn`) installed
- Execution permission for the `run.sh` script

---

## Step-by-Step Execution

### 1. Compile the project

```bash
mvn compile
```

---

## Running Locally

### Start the first node (port between `5000` and `5004`):

```bash
./run.sh 127.0.0.1:5000
```

### Add a second node (using the bootstrap node's IP and port):

```bash
./run.sh 127.0.0.1:5001 127.0.0.1:5000
```

Repeat this step with other ports (`5002`, `5003`, etc.) to simulate a local network with multiple nodes.

---

## Running on a Public Network

1. Open the configuration file:

```bash
data/infonode.json
```

2. Replace the local IPs with the public IPs of each machine in the network.

3. On each machine, run the node with the appropriate IP/port and the IP of the bootstrap node:

```bash
./run.sh <PUBLIC_IP:PORT> [BOOTSTRAP_IP:PORT]
```

Example:

```bash
./run.sh 34.123.45.67:5001 34.111.22.10:5000
```

---
