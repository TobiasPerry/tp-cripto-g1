
# Cripto Steganography Project

## Contributors
- Manuel Esteban Dithurbide
- Tomas Camilo Gay Bare
- Gaston Alasia
- Tobias Perry

## Overview

This project provides a tool to hide and extract files within BMP images using various steganography techniques and optional encryption. It supports embedding files using LSB1 LSB4 and LSBI techniques and encrypting hidden data with algorithms like AES and 3DES.

## Features

- **Steganography Methods**: LSB1, LSB4, LSBI
- **Encryption Support**: AES (128, 192, 256) and 3DES
- **Encryption Modes**: ECB, CFB, OFB, CBC
- **Password Protection**: Required for encryption

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- Maven

## Building the Project

1. Clone the repository.
2. Navigate to the project directory.
3. Run the following command to build the project:
   ```bash
   mvn package
   ```

## Running the Application

After building the project, the compiled JAR file is located in the `target` directory. Use the following command to run the application:

```bash
java -jar target/Cripto-1.0-SNAPSHOT.jar <parameters>
```

## Command-line Parameters

### Embedding a File

```bash
-embed -in <file> -p <bitmapfile> -out <filename> -steg <LSB1 | LSB4 | LSBI> [optional encryption parameters]
```

#### Parameters:
- `-embed`: Indicates embedding mode.
- `-in <file>`: File to hide.
- `-p <bitmapfile>`: BMP file to be used as the carrier.
- `-out <bitmapfile>`: Output BMP file with the embedded file.
- `-steg <LSB1 | LSB4 | LSBI>`: Steganography method.
- **Optional Encryption Parameters**:
  - `-a <aes128 | aes192 | aes256 | 3des>`: Encryption algorithm.
  - `-m <ecb | cfb | ofb | cbc>`: Encryption mode.
  - `-pass <password>`: Encryption password.

### Example 1 (With Encryption)
```bash
java -jar target/Cripto-1.0-SNAPSHOT.jar -embed -in "mensaje1.txt" -p "imagen1.bmp" -out "imagenmas1.bmp" -steg LSBI -a 3des -m cbc -pass "oculto"
```

### Example 2 (Without Encryption)
```bash
java -jar target/Cripto-1.0-SNAPSHOT.jar -embed -in "mensaje1.txt" -p "imagen1.bmp" -out "imagenmas1.bmp" -steg LSBI
```

### Extracting a File

```bash
-extract -p <bitmapfile> -out <file> -steg <LSB1 | LSB4 | LSBI> [optional decryption parameters]
```

#### Parameters:
- `-extract`: Indicates extraction mode.
- `-p <bitmapfile>`: BMP file containing the hidden file.
- `-out <file>`: Output file to save the extracted data.
- `-steg <LSB1 | LSB4 | LSBI>`: Steganography method.
- **Optional Decryption Parameters**:
  - `-a <aes128 | aes192 | aes256 | 3des>`: Decryption algorithm.
  - `-m <ecb | cfb | ofb | cbc>`: Decryption mode.
  - `-pass <password>`: Decryption password.

### Example (With Decryption)
```bash
java -jar target/Cripto-1.0-SNAPSHOT.jar -extract -p "imagenmas1.bmp" -out "mensaje1" -steg LSBI -a 3des -m cbc -pass "oculto"
```

## Notes

- Encryption requires a password. Without a password, only steganography is applied.
- Ensure BMP files are used as carriers to maintain compatibility with the LSB methods.

