# Crypto Penscrux

Compact Spring Boot + Vaadin demo for Shamir Secret Sharing with a BIP-39 wordlist. Provides a UI to generate a mnemonic, split it into Shamir shares and recover the mnemonic from shares.

## Build

- Build the project:
  - `mvn clean package`

## Run

- Run with Maven:
  - `mvn spring-boot:run`
- Or run built jar:
  - `java -jar target/*.jar`
- Open UI:
  - `http://localhost:8080`

## Tests

- Execute unit tests:
  - `mvn test`

## Security Notes

- This project is a demonstration. It has not been audited for production-grade security.
- Never store mnemonics or shares unencrypted in insecure locations.
- Treat shares as sensitive secrets; distribute and store them securely.
- Do not use this demo with real funds without a full security review.