var stompClient = null;

function displayMessage(message) {
    console.log("displayMessage: ", message);
    var messagesDiv = document.getElementById('messages');
    var node = document.createElement('div');
    message.date = convertDateToTime(message.date)
    var nodeText = document.createTextNode(message.from + ': ' + message.content + ' [' + message.date + ']');
    node.appendChild(nodeText);
    messagesDiv.appendChild(node);
    messagesDiv.appendChild(document.createElement('br'));
}

function connect() {
    var myUsername = document.getElementById('from').value;
    var host = 'http://localhost:8080';
    var socketString = host + '/tim-websocket';
    var socket = new SockJS(socketString);
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        displayMessage(createMessage('System', '', 'Connected to websocket'));
        console.log('Connected: ' + frame);
        stompClient.send('/app/status', {}, JSON.stringify(createUserStatus('AVAILABLE')));
        stompClient.subscribe('/topic/status', function (message) {
            console.log('stomp/topic/status');
            console.log(message);
            receiveStatus(JSON.parse(message.body));
        });
        stompClient.subscribe('/topic/private.message.' + myUsername, function (message) {
            console.log('stomp/topic/private.message.' + myUsername);
            console.log(message);
            displayMessage(JSON.parse(message.body));
        });
    });
}

function disconnect() {
    if (stompClient === null) {
        console.log("Not connected");
        return;
    }

    stompClient.send('/app/status', {}, JSON.stringify(createUserStatus('OFFLINE')));
    stompClient.disconnect();

    displayMessage(createMessage('System', '', 'Disconnected from websocket'));
    console.log('Disconnected');
}

function sendMessage() {
    if (stompClient === null) {
        console.log("Not connected");
        return;
    }

    var from = document.getElementById('from').value;
    var to = document.getElementById('to').value;
    var content = document.getElementById('content').value;
    var message = createMessage(from, to, content)
    stompClient.send('/app/private.message.' + to, {}, JSON.stringify(message));
    displayMessage(message);
}

function receiveStatus(user) {
    console.log("receiveStatus: ", user);
    var message = createMessage(user.fullName, '', user.status);
    message.date = user.statusTime;
    displayMessage(message);
}

function setAvailable() {
    if (stompClient === null) {
        console.log("Not connected");
        return;
    }

    stompClient.send('/app/status', {}, JSON.stringify(createUserStatus('AVAILABLE')));
}

function setAway(n) {
    if (stompClient === null) {
        console.log("Not connected");
        return;
    }

    stompClient.send('/app/status', {}, JSON.stringify(createUserStatus('AWAY')));
}

function str_pad(n) {
    return String('00' + n).slice(-2);
}

function getCurrentTime() {
    return convertDateToTime(new Date());
}

function convertDateToTime(date) {
    date = new Date(date);
    return String(str_pad(date.getHours()) + ':' + str_pad(date.getMinutes()) + ':' + str_pad(date.getSeconds()));
}

function createMessage(from, to, content) {
    return {'from': from,
            'to': to,
            'content': content,
            'date': new Date()};
}

function createUserStatus(status) {
    var myUsername = document.getElementById('from').value;
    var fullname = document.getElementById('fullname').value;
    return {'userId': myUsername,
            'fullName': fullname,
            'status': status,
            'statusTime': new Date()};
}