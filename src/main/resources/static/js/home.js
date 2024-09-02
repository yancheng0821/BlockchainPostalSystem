document.addEventListener('DOMContentLoaded', function() {
    const newExpressLink = document.getElementById('newExpressLink');
    const newEncryptedExpressLink = document.getElementById('newEncryptedExpressLink');
    const queryLink = document.getElementById('queryLink');
    const updateStatusLink = document.getElementById('updateStatusLink');
    const historyLink = document.getElementById('historyLink');
    const auditLogLink = document.getElementById('auditLogLink');
    const contentDiv = document.getElementById('content');
    const newExpressForm = document.getElementById('newExpressForm');
    const newEncryptedExpressForm = document.getElementById('newEncryptedExpressForm');
    const queryResults = document.getElementById('queryResults');
    const parcelHistory = document.getElementById('parcelHistory');
    const updateStatusForm = document.getElementById('updateStatusForm');
    const auditLogResults = document.getElementById('auditLogResults');
    const parcelTableBody = document.getElementById('parcelTableBody');
    const expressForm = document.getElementById('expressForm');
    const encryptedExpressForm = document.getElementById('encryptedExpressForm');
    const statusForm = document.getElementById('statusForm');
    const logoutButton = document.getElementById('logout');
    const queryHistoryButton = document.getElementById('queryHistoryButton');
    const historyResults = document.getElementById('historyResults');
    const auditLogTableBody = document.getElementById('auditLogTableBody');
    const chainHistoryResults = document.getElementById('chainHistoryResults');
    const chainHistoryDetails = document.getElementById('chainHistoryDetails');

    const modal = document.getElementById("myModal");
    const modalImg = document.getElementById("modalImg");
    const span = document.getElementsByClassName("close")[0];

    function formatDate(dateString) {
        if (!dateString) {
            return "NA";
        }
        let dateParts = dateString.split(" ");
        if (dateParts.length < 6) {
            return "NA";
        }
        const months = {
            Jan: "01",
            Feb: "02",
            Mar: "03",
            Apr: "04",
            May: "05",
            Jun: "06",
            Jul: "07",
            Aug: "08",
            Sep: "09",
            Oct: "10",
            Nov: "11",
            Dec: "12"
        };
        let year = dateParts[5];
        let month = months[dateParts[1]];
        let day = dateParts[2];
        let time = dateParts[3];
        if (!year || !month || !day || !time) {
            return "NA";
        }
        return `${year}-${month}-${day} ${time}`;
    }

    function convertTimestampToDate(timestamp) {
        if (!timestamp) {
            return "Invalid Timestamp";
        }
        const date = new Date(parseInt(timestamp));
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }

    function formatTimestamp(isoTimestamp) {
        const date = new Date(isoTimestamp);
        if (isNaN(date.getTime())) {
            return "Invalid Timestamp";
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');

        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }


    function sortParcelsByCreatedTime(parcels) {
        return parcels.sort((a, b) => new Date(b.createdTime) - new Date(a.createdTime));
    }

    function sortHistoryByTimestamp(history) {
        return history.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    }

    function sortLogsByTimestamp(history) {
        return history.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    }

    function hideAllForms() {
        contentDiv.querySelector('h1').style.display = 'none';
        contentDiv.querySelector('p').style.display = 'none';
        newExpressForm.style.display = 'none';
        newEncryptedExpressForm.style.display = 'none';
        queryResults.style.display = 'none';
        parcelHistory.style.display = 'none';
        updateStatusForm.style.display = 'none';
        auditLogResults.style.display = 'none';
        chainHistoryResults.style.display = 'none'; // 新增部分
    }

    newExpressLink.addEventListener('click', function(event) {
        event.preventDefault();
        hideAllForms();
        newExpressForm.style.display = 'block';
    });

    newEncryptedExpressLink.addEventListener('click', function(event) {
        event.preventDefault();
        hideAllForms();
        newEncryptedExpressForm.style.display = 'block';
    });

    queryLink.addEventListener('click', function(event) {
        event.preventDefault();
        hideAllForms();
        queryResults.style.display = 'block';

        fetch('/api/parcel/queryAll')
            .then(response => response.json())
            .then(data => {
                parcelTableBody.innerHTML = '';
                const sortedParcels = sortParcelsByCreatedTime(data);
                sortedParcels.forEach(parcel => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                    <td>${parcel.id}</td>
                    <td>${parcel.sender}</td>
                    <td>${parcel.receiver}</td>
                    <td>${parcel.status}</td>
                    <td>${parcel.location}</td>
                    <td>${formatDate(parcel.createdTime)}</td>
                    <td>${formatDate(parcel.lastModifiedTime)}</td>
                    <td>${formatDate(parcel.completedTime)}</td>
                    <td><img src="data:image/png;base64, ${parcel.qrCode}" alt="QR Code" class="qr-code" style="width:50px;cursor:pointer;"></td>
                    <td><button type="button" class="get-history btn" data-id="${parcel.id}">Audit Logs</button></td>
                `;
                    parcelTableBody.appendChild(row);
                });

                document.querySelectorAll('.qr-code').forEach(img => {
                    img.addEventListener('click', function() {
                        modal.style.display = "block";
                        modalImg.src = this.src;
                    });
                });

                document.querySelectorAll('.get-history').forEach(button => {
                    button.addEventListener('click', function() {
                        const parcelId = this.getAttribute('data-id');
                        fetch(`/api/audit/getHistory?parcelId=${parcelId}`)
                            .then(response => response.json())
                            .then(data => {
                                chainHistoryDetails.innerHTML = '';

                                const sortedData = sortHistoryByTimestamp(data);

                                sortedData.forEach(historyItem => {
                                    const itemDiv = document.createElement('div');
                                    itemDiv.style.wordBreak = 'break-all';
                                    itemDiv.innerHTML = `
                        <p><strong>ParcelId:</strong> ${historyItem.parcelId}</p>
                        <p><strong>Operation Type:</strong> ${historyItem.operationType}</p>
                        <p><strong>Details:</strong> ${historyItem.params.join(', ')}</p>
                        <p><strong>Timestamp:</strong> ${formatTimestamp(historyItem.timestamp)}</p>
                        <p><strong>Prev Hash:</strong> ${historyItem.prevHash}</p>
                        <p><strong>Hash:</strong> ${historyItem.hash}</p>
                        <hr>
                    `;
                                    chainHistoryDetails.appendChild(itemDiv);
                                });
                                hideAllForms();
                                chainHistoryResults.style.display = 'block';
                            })
                            .catch(error => {
                                console.error('Error:', error);
                                alert('Error querying audit history');
                            });
                    });
                });

            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error querying parcels');
            });
    });

    span.onclick = function() {
        modal.style.display = "none";
    }

    window.onclick = function(event) {
        if (event.target == modal) {
            modal.style.display = "none";
        }
    }

    updateStatusLink.addEventListener('click', function(event) {
        event.preventDefault();
        hideAllForms();
        updateStatusForm.style.display = 'block';
    });

    expressForm.addEventListener('submit', function(event) {
        event.preventDefault();

        const formData = new FormData(expressForm);
        const parcelData = {
            sender: formData.get('sender'),
            receiver: formData.get('receiver'),
            createdTime: new Date().toISOString()
        };

        fetch('/api/parcel/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(parcelData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.message === "Parcel created successfully") {
                    alert(data.message);
                    expressForm.reset();
                    hideAllForms();
                    contentDiv.querySelector('h1').style.display = 'block';
                    contentDiv.querySelector('p').style.display = 'block';
                } else {
                    alert(data.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error creating parcel');
            });
    });

    encryptedExpressForm.addEventListener('submit', function(event) {
        event.preventDefault();

        const formData = new FormData(encryptedExpressForm);
        const encryptedParcelData = {
            sender: formData.get('sender'),
            receiver: formData.get('receiver')
        };

        fetch('/api/parcelEncryption/createWithEncryption', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(encryptedParcelData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.message === "Parcel created successfully with encryption") {
                    alert(data.message);
                    encryptedExpressForm.reset();
                    hideAllForms();
                    contentDiv.querySelector('h1').style.display = 'block';
                    contentDiv.querySelector('p').style.display = 'block';
                } else {
                    alert(data.message);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error creating parcel with encryption');
            });
    });

    statusForm.addEventListener('submit', function(event) {
        event.preventDefault();

        const formData = new FormData(statusForm);
        const statusData = {
            id: formData.get('parcelId'),
            status: formData.get('newStatus')
        };

        fetch('/api/parcel/updateStatus', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(statusData)
        })
            .then(response => response.json())
            .then(data => {
                alert('Parcel status updated successfully');
                statusForm.reset();
                hideAllForms();
                contentDiv.querySelector('h1').style.display = 'block';
                contentDiv.querySelector('p').style.display = 'block';
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error updating parcel status');
            });
    });

    historyLink.addEventListener('click', function(event) {
        event.preventDefault();
        hideAllForms();
        parcelHistory.style.display = 'block';
    });

    queryHistoryButton.addEventListener('click', function(event) {
        event.preventDefault();
        const parcelId = document.getElementById('historyParcelId').value;

        fetch(`/api/parcel/history?id=${parcelId}`)
            .then(response => response.json())
            .then(data => {
                historyResults.innerHTML = '';
                data.forEach(historyItem => {
                    const itemDiv = document.createElement('div');
                    itemDiv.style.wordBreak = 'break-all';
                    itemDiv.innerHTML = `
                    <p><strong>Transaction ID:</strong> ${historyItem.TxId}</p>
                    <p><strong>Timestamp:</strong> ${convertTimestampToDate(historyItem.Timestamp.seconds * 1000)}</p>
                    <p><strong>Status:</strong> ${historyItem.Value.status}</p>
                    <p><strong>Location:</strong> ${historyItem.Value.location}</p>
                    <hr>
                `;
                    historyResults.appendChild(itemDiv);
                });
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error querying parcel history');
            });
    });

    auditLogLink.addEventListener('click', function(event) {
        event.preventDefault();
        hideAllForms();
        auditLogResults.style.display = 'block';

        fetch('/api/audit/queryAllLogs')
            .then(response => response.json())
            .then(data => {
                auditLogTableBody.innerHTML = '';
                const sortedData = sortLogsByTimestamp(data);

                sortedData.forEach(log => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${log.parcelId}</td>
                        <td>${log.operationType}</td>
                        <td>${formatTimestamp(log.timestamp)}</td>
                        <td>${log.params.join(', ')}</td>
                    `;
                    auditLogTableBody.appendChild(row);
                });
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error querying audit logs');
            });
    });

    logoutButton.addEventListener('click', function(event) {
        event.preventDefault();

        fetch('/api/logout', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        })
            .then(response => {
                if (response.ok) {
                    window.location.href = 'index.html';
                } else {
                    alert('Logout failed');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error logging out');
            });
    });
});
