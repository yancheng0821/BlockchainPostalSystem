package main

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/des"
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"io"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
)

type SmartContract struct{}

func (s *SmartContract) Init(APIstub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success(nil)
}

func (s *SmartContract) Invoke(APIstub shim.ChaincodeStubInterface) sc.Response {
	function, args := APIstub.GetFunctionAndParameters()
	if function == "encrypt" {
		return s.encrypt(APIstub, args)
	} else if function == "decrypt" {
		return s.decrypt(APIstub, args)
	}
	return shim.Error("Invalid Smart Contract function name.")
}

func (s *SmartContract) encrypt(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2 (algorithm, text)")
	}

	algorithm := args[0]
	text := []byte(args[1])
	var cipherText string
	var err error

	switch algorithm {
	case "AES":
		key := []byte("myverystrongpasswordo32bitlength")
		cipherText, err = encryptAES(key, text)
	case "DES":
		key := []byte("mydeskey") // DES key should be 8 bytes
		cipherText, err = encryptDES(key, text)
	case "3DES":
		key := []byte("myverystrongpasswordo24b") // 3DES key should be 24 bytes
		cipherText, err = encrypt3DES(key, text)
	default:
		return shim.Error("Unsupported algorithm")
	}

	if err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success([]byte(cipherText))
}

func (s *SmartContract) decrypt(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2 (algorithm, cipherText)")
	}

	algorithm := args[0]
	cipherText := args[1]
	var plainText []byte
	var err error

	switch algorithm {
	case "AES":
		key := []byte("myverystrongpasswordo32bitlength")
		plainText, err = decryptAES(key, cipherText)
	case "DES":
		key := []byte("mydeskey")
		plainText, err = decryptDES(key, cipherText)
	case "3DES":
		key := []byte("myverystrongpasswordo24b")
		plainText, err = decrypt3DES(key, cipherText)
	default:
		return shim.Error("Unsupported algorithm")
	}

	if err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success(plainText)
}

// AES encryption and decryption
func encryptAES(key, text []byte) (string, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return "", err
	}

	ciphertext := make([]byte, aes.BlockSize+len(text))
	iv := ciphertext[:aes.BlockSize]
	if _, err := io.ReadFull(rand.Reader, iv); err != nil {
		return "", err
	}

	stream := cipher.NewCFBEncrypter(block, iv)
	stream.XORKeyStream(ciphertext[aes.BlockSize:], text)

	return base64.StdEncoding.EncodeToString(ciphertext), nil
}

func decryptAES(key []byte, cryptoText string) ([]byte, error) {
	ciphertext, _ := base64.StdEncoding.DecodeString(cryptoText)
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	if len(ciphertext) < aes.BlockSize {
		return nil, fmt.Errorf("ciphertext too short")
	}
	iv := ciphertext[:aes.BlockSize]
	ciphertext = ciphertext[aes.BlockSize:]

	stream := cipher.NewCFBDecrypter(block, iv)
	stream.XORKeyStream(ciphertext, ciphertext)

	return ciphertext, nil
}

// DES encryption and decryption
func encryptDES(key, text []byte) (string, error) {
	block, err := des.NewCipher(key)
	if err != nil {
		return "", err
	}

	ciphertext := make([]byte, des.BlockSize+len(text))
	iv := ciphertext[:des.BlockSize]
	if _, err := io.ReadFull(rand.Reader, iv); err != nil {
		return "", err
	}

	stream := cipher.NewCFBEncrypter(block, iv)
	stream.XORKeyStream(ciphertext[des.BlockSize:], text)

	return base64.StdEncoding.EncodeToString(ciphertext), nil
}

func decryptDES(key []byte, cryptoText string) ([]byte, error) {
	ciphertext, _ := base64.StdEncoding.DecodeString(cryptoText)
	block, err := des.NewCipher(key)
	if err != nil {
		return nil, err
	}

	if len(ciphertext) < des.BlockSize {
		return nil, fmt.Errorf("ciphertext too short")
	}
	iv := ciphertext[:des.BlockSize]
	ciphertext = ciphertext[des.BlockSize:]

	stream := cipher.NewCFBDecrypter(block, iv)
	stream.XORKeyStream(ciphertext, ciphertext)

	return ciphertext, nil
}

// 3DES encryption and decryption
func encrypt3DES(key, text []byte) (string, error) {
	block, err := des.NewTripleDESCipher(key)
	if err != nil {
		return "", err
	}

	ciphertext := make([]byte, des.BlockSize+len(text))
	iv := ciphertext[:des.BlockSize]
	if _, err := io.ReadFull(rand.Reader, iv); err != nil {
		return "", err
	}

	stream := cipher.NewCFBEncrypter(block, iv)
	stream.XORKeyStream(ciphertext[des.BlockSize:], text)

	return base64.StdEncoding.EncodeToString(ciphertext), nil
}

func decrypt3DES(key []byte, cryptoText string) ([]byte, error) {
	ciphertext, _ := base64.StdEncoding.DecodeString(cryptoText)
	block, err := des.NewTripleDESCipher(key)
	if err != nil {
		return nil, err
	}

	if len(ciphertext) < des.BlockSize {
		return nil, fmt.Errorf("ciphertext too short")
	}
	iv := ciphertext[:des.BlockSize]
	ciphertext = ciphertext[des.BlockSize:]

	stream := cipher.NewCFBDecrypter(block, iv)
	stream.XORKeyStream(ciphertext, ciphertext)

	return ciphertext, nil
}

func main() {
	err := shim.Start(new(SmartContract))
	if err != nil {
		fmt.Printf("Error creating new Smart Contract: %s", err)
	}
}
