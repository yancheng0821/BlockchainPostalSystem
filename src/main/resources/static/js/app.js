document.getElementById('loginForm').addEventListener('submit', function(event) {
    event.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    fetch('/api/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
        .then(response => {
            if (response.ok) {
                response.json().then(data => {
                    sessionStorage.setItem('username', data.username);
                    window.location.href = '/home.html';
                });
            } else {
                alert('Login failed');
            }
        })
        .catch(error => {
            console.error('Error:', error);
        });
});
