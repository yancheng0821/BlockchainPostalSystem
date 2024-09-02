package main

import (
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
	"time"
)

// SmartContract defines the structure of the chaincode
type SmartContract struct{}

// AuditRecord defines the structure of the audit record
type AuditRecord struct {
	ParcelId      string   `json:"parcelId"`
	OperationType string   `json:"operationType"`
	Params        []string `json:"params"`
	Timestamp     string   `json:"timestamp"`
	PrevHash      string   `json:"prevHash"`
	Hash          string   `json:"hash"`
}

// Init is called during chaincode instantiation to initialize any data
func (s *SmartContract) Init(APIstub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success(nil)
}

// Invoke is called per transaction on the chaincode
func (s *SmartContract) Invoke(APIstub shim.ChaincodeStubInterface) sc.Response {
	function, args := APIstub.GetFunctionAndParameters()
	if function == "auditLog" {
		return s.auditLog(APIstub, args)
	} else if function == "queryAllLogs" {
		return s.queryAllLogs(APIstub)
	} else if function == "getHistory" {
		return s.getHistory(APIstub, args)
	}
	return shim.Error("Invalid Smart Contract function name.")
}

// auditLog creates a new audit record and stores it in the ledger
func (s *SmartContract) auditLog(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) < 3 {
		return shim.Error("Incorrect number of arguments. Expecting at least 3 (parcelId, operationType, params...)")
	}

	parcelId := args[0]
	operationType := args[1]
	params := args[2:]
	timestamp := time.Now().Format(time.RFC3339)

	// Get the previous hash
	prevHash, err := s.getLastHash(APIstub, parcelId)
	if err != nil {
		return shim.Error("Failed to get previous hash")
	}

	// Create a new audit record
	auditRecord := AuditRecord{
		ParcelId:      parcelId,
		OperationType: operationType,
		Params:        params,
		Timestamp:     timestamp,
		PrevHash:      prevHash,
	}
	auditRecord.Hash = s.calculateHash(auditRecord)

	// Marshal the audit record to JSON
	auditRecordAsBytes, err := json.Marshal(auditRecord)
	if err != nil {
		return shim.Error("Failed to marshal audit record")
	}

	// Store the audit record in the ledger
	auditRecordKey := fmt.Sprintf("AUDIT_%s_%s", parcelId, APIstub.GetTxID())
	err = APIstub.PutState(auditRecordKey, auditRecordAsBytes)
	if err != nil {
		return shim.Error("Failed to create audit record")
	}

	return shim.Success(nil)
}

// getLastHash retrieves the hash of the last audit record for a specific parcel
func (s *SmartContract) getLastHash(APIstub shim.ChaincodeStubInterface, parcelId string) (string, error) {
	resultsIterator, err := APIstub.GetStateByRange("", "")
	if err != nil {
		return "", err
	}
	defer resultsIterator.Close()

	var lastHash string
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return "", err
		}
		var auditRecord AuditRecord
		json.Unmarshal(queryResponse.Value, &auditRecord)
		if auditRecord.ParcelId == parcelId {
			lastHash = auditRecord.Hash
		}
	}
	return lastHash, nil
}

// calculateHash calculates the hash of an audit record
func (s *SmartContract) calculateHash(record AuditRecord) string {
	recordAsBytes, _ := json.Marshal(record)
	hash := sha256.New()
	hash.Write(recordAsBytes)
	return fmt.Sprintf("%x", hash.Sum(nil))
}

// queryAllLogs retrieves all audit records from the ledger
func (s *SmartContract) queryAllLogs(APIstub shim.ChaincodeStubInterface) sc.Response {
	startKey := ""
	endKey := ""

	resultsIterator, err := APIstub.GetStateByRange(startKey, endKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	var records []AuditRecord
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		var record AuditRecord
		json.Unmarshal(queryResponse.Value, &record)
		records = append(records, record)
	}

	recordsAsBytes, _ := json.Marshal(records)
	return shim.Success(recordsAsBytes)
}

// getHistory retrieves the history of a specific audit record using parcelId
func (s *SmartContract) getHistory(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1 (parcelId)")
	}

	parcelId := args[0]
	resultsIterator, err := APIstub.GetStateByRange("", "")
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	var history []AuditRecord
	for resultsIterator.HasNext() {
		response, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		var record AuditRecord
		json.Unmarshal(response.Value, &record)

		if record.ParcelId == parcelId {
			history = append(history, record)
		}
	}

	historyAsBytes, _ := json.Marshal(history)
	return shim.Success(historyAsBytes)
}

func main() {
	err := shim.Start(new(SmartContract))
	if err != nil {
		fmt.Printf("Error creating new Smart Contract: %s", err)
	}
}
