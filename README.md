# Public Ledger for Decentralized Auctions

This repository contains the final project for the **Security of Systems and Data** course 2024/2025. It implements a **secure and decentralized auction platform** using a custom blockchain and a secure peer-to-peer (P2P) network based on the S/Kademlia protocol.

## Project Summary

The system enables the creation, propagation, and validation of auction transactions in a distributed environment without a central authority. It ensures **authenticity**, **integrity**, and **transparency** of all operations through cryptographic guarantees and consensus mechanisms.

The platform was tested across **multiple virtual machines (VMs)** in the Google Cloud Platform (GCP), simulating real-world distributed operation.

## Core Technologies & Concepts

- **Blockchain with Merkle Trees**
- **Proof-of-Work (PoW)** for block mining
- **Digital Signatures (RSA + SHA-256)** for authentication
- **Secure P2P network** using the S/Kademlia protocol
- **Consensus and fork resolution** mechanisms
- **Fault injection handling** (malicious node testing)
- **Optional Proof-of-Reputation (PoR)** extension

## Architecture Overview

- **Distributed Ledger**: Implements a secure blockchain with digital signatures and tamper-proof structure.
- **P2P Network**: Each node has a unique identity derived from its public key and maintains a routing table.
- **Auction Logic**: Implemented with transaction types: `CREATE_AUCTION`, `START_AUCTION`, `BID`, `CLOSE_AUCTION`.
- **Consensus Mechanism**: Forks resolved by timestamp and chain height; PoW used to validate new blocks.
- **Security Checks**: Signature validation, Merkle root comparison, transaction timestamp enforcement.
- **Fault Tolerance**: Includes detection of invalid/malicious blocks and recovery from orphan blocks.

## Repository Structure
```
/
├── data/ # Keys and local network state (blockchain and routing table)
├── lib/ # External libraries
├── src/ # Java source code (blockchain, Kademlia, auction logic)
├── .gitignore 
├── README.md 
├── Report.pdf 
├── dependency-reduced-pom.xml 
├── execution.md # Instructions to run and test the system
├── fault_injection_test.txt 
├── pom.xml 
├── run.sh # Bash script to launch node(s)
└── sistemasdedados.iml
```
## Project Report

The full design, implementation, security mechanisms, and evaluation are detailed in **Report.pdf**




