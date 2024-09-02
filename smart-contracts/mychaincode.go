package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
	"io/ioutil"
	"net/http"
	"strings"
)

type SmartContract struct{}

type Parcel struct {
	ID               string `json:"id"`
	Sender           string `json:"sender"`
	Receiver         string `json:"receiver"`
	Status           string `json:"status"`
	Location         string `json:"location"`
	CreatedTime      string `json:"createdTime"`
	CompletedTime    string `json:"completedTime"`
	LastModifiedTime string `json:"lastModifiedTime"`
	QRCode           string `json:"qrCode"`
}

func (s *SmartContract) Init(APIstub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success(nil)
}

func (s *SmartContract) Invoke(APIstub shim.ChaincodeStubInterface) sc.Response {
	function, args := APIstub.GetFunctionAndParameters()
	switch function {
	case "createParcel":
		return s.createParcel(APIstub, args)
	case "updateStatus":
		return s.updateStatus(APIstub, args)
	case "queryParcel":
		return s.queryParcel(APIstub, args)
	case "queryAllParcels":
		return s.queryAllParcels(APIstub)
	case "getHistoryForParcel":
		return s.getHistoryForParcel(APIstub, args)
	case "getCurrentStatus":
		return s.getCurrentStatus(APIstub, args)
	case "createParcelWithEncryption":
		return s.createParcelWithEncryption(APIstub, args)
	case "createParcelsBatch":
		return s.createParcelsBatch(APIstub, args)
	case "queryEncryptedParcels":
		return s.queryEncryptedParcels(APIstub)
	default:
		return shim.Error("Invalid Smart Contract function name.")
	}
}

func (s *SmartContract) createParcel(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 9 {
		return shim.Error("Incorrect number of arguments. Expecting 9")
	}

	parcel := Parcel{
		ID:               args[0],
		Sender:           args[1],
		Receiver:         args[2],
		Status:           args[3],
		Location:         args[4],
		CreatedTime:      args[5],
		CompletedTime:    args[6],
		LastModifiedTime: args[7],
		QRCode:           args[8],
	}

	parcelAsBytes, err := json.Marshal(parcel)
	if err != nil {
		return shim.Error("Error marshaling parcel data: " + err.Error())
	}

	err = APIstub.PutState(args[0], parcelAsBytes)
	if err != nil {
		return shim.Error("Failed to create parcel: " + err.Error())
	}

	return shim.Success(nil)
}

func (s *SmartContract) createParcelsBatch(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) == 0 {
		return shim.Error("Incorrect number of arguments. Expecting at least 1")
	}

	for _, arg := range args {
		parcel := Parcel{}
		err := json.Unmarshal([]byte(arg), &parcel)
		if err != nil {
			return shim.Error("Error unmarshaling parcel data: " + err.Error())
		}

		parcelAsBytes, err := json.Marshal(parcel)
		if err != nil {
			return shim.Error("Error marshaling parcel data: " + err.Error())
		}

		err = APIstub.PutState(parcel.ID, parcelAsBytes)
		if err != nil {
			return shim.Error("Failed to create parcel: " + err.Error())
		}
	}

	return shim.Success(nil)
}

func (s *SmartContract) createParcelWithEncryption(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2 (key, encryptedData)")
	}

	key := "encrypted_" + args[0]
	encryptedData := args[1]

	err := APIstub.PutState(key, []byte(encryptedData))
	if err != nil {
		return shim.Error("Failed to put state: " + err.Error())
	}

	return shim.Success(nil)
}

func generateQRCode(text string) (string, error) {
	resp, err := http.Get("https://api.qrserver.com/v1/create-qr-code/?data=" + text + "&size=200x200")
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	imgBytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	return base64.StdEncoding.EncodeToString(imgBytes), nil
}

func (s *SmartContract) updateStatus(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}

	parcelAsBytes, err := APIstub.GetState(args[0])
	if err != nil {
		return shim.Error("Could not retrieve parcel: " + err.Error())
	} else if parcelAsBytes == nil {
		return shim.Error("Could not find parcel")
	}

	parcel := Parcel{}
	err = json.Unmarshal(parcelAsBytes, &parcel)
	if err != nil {
		return shim.Error("Error unmarshaling parcel data: " + err.Error())
	}

	parcel.Status = args[1]
	parcel.Location = args[2]
	parcel.LastModifiedTime = args[3]
	parcel.CompletedTime = args[4]

	parcelAsBytes, err = json.Marshal(parcel)
	if err != nil {
		return shim.Error("Error marshaling parcel data: " + err.Error())
	}

	err = APIstub.PutState(args[0], parcelAsBytes)
	if err != nil {
		return shim.Error("Failed to update parcel: " + err.Error())
	}

	return shim.Success(parcelAsBytes)
}

func (s *SmartContract) getCurrentStatus(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	parcelAsBytes, err := APIstub.GetState(args[0])
	if err != nil {
		return shim.Error("Could not retrieve parcel: " + err.Error())
	} else if parcelAsBytes == nil {
		return shim.Error("Could not find parcel")
	}

	parcel := Parcel{}
	err = json.Unmarshal(parcelAsBytes, &parcel)
	if err != nil {
		return shim.Error("Error unmarshaling parcel data: " + err.Error())
	}

	return shim.Success([]byte(parcel.Status))
}

func (s *SmartContract) queryParcel(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	parcelAsBytes, err := APIstub.GetState(args[0])
	if err != nil {
		return shim.Error("Could not retrieve parcel: " + err.Error())
	} else if parcelAsBytes == nil {
		return shim.Error("Could not find parcel")
	}

	return shim.Success(parcelAsBytes)
}

func (s *SmartContract) queryAllParcels(APIstub shim.ChaincodeStubInterface) sc.Response {
	startKey := ""
	endKey := ""

	resultsIterator, err := APIstub.GetStateByRange(startKey, endKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	var parcels []Parcel
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		// 过滤掉加密数据
		if !strings.HasPrefix(queryResponse.Key, "encrypted_") {
			var parcel Parcel
			if json.Valid(queryResponse.Value) {
				err = json.Unmarshal(queryResponse.Value, &parcel)
				if err != nil {
					fmt.Printf("Error unmarshaling parcel data: %s\n", err.Error())
					continue
				}
				parcels = append(parcels, parcel)
			} else {
				fmt.Printf("Invalid JSON data: %s\n", string(queryResponse.Value))
			}
		}
	}

	parcelsAsBytes, err := json.Marshal(parcels)
	if err != nil {
		return shim.Error("Error marshaling parcels data: " + err.Error())
	}

	return shim.Success(parcelsAsBytes)
}

func (s *SmartContract) queryEncryptedParcels(APIstub shim.ChaincodeStubInterface) sc.Response {
	startKey := "encrypted_"
	endKey := "encrypted_\uffff" // 使用一个合理的最大值来结束范围

	resultsIterator, err := APIstub.GetStateByRange(startKey, endKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	var encryptedParcels []map[string]string
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		// 确认返回的 key 是否以 "encrypted_" 开头
		if strings.HasPrefix(queryResponse.Key, "encrypted_") {
			encryptedParcel := map[string]string{
				"Key":  queryResponse.Key,
				"Data": string(queryResponse.Value),
			}
			encryptedParcels = append(encryptedParcels, encryptedParcel)
		}
	}

	encryptedParcelsAsBytes, err := json.Marshal(encryptedParcels)
	if err != nil {
		return shim.Error("Error marshaling encrypted parcels data: " + err.Error())
	}

	return shim.Success(encryptedParcelsAsBytes)
}

func (s *SmartContract) getHistoryForParcel(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	parcelID := args[0]

	resultsIterator, err := APIstub.GetHistoryForKey(parcelID)
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	var history []map[string]interface{}
	for resultsIterator.HasNext() {
		response, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		var parcel Parcel
		err = json.Unmarshal(response.Value, &parcel)
		if err != nil {
			return shim.Error("Error unmarshaling parcel data: " + err.Error())
		}

		historyItem := map[string]interface{}{
			"TxId":      response.TxId,
			"Timestamp": response.Timestamp,
			"IsDelete":  response.IsDelete,
			"Value":     parcel,
		}

		history = append(history, historyItem)
	}

	historyAsBytes, err := json.Marshal(history)
	if err != nil {
		return shim.Error("Error marshaling history data: " + err.Error())
	}

	return shim.Success(historyAsBytes)
}

func main() {
	err := shim.Start(new(SmartContract))
	if err != nil {
		fmt.Printf("Error creating new Smart Contract: %s", err)
	}
}
