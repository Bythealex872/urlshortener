$(document).ready(function () {
    $("#shortener").submit(function (event) {
        event.preventDefault();

        $.ajax({
            type: "POST",
            url: "/api/link",
            data: $(this).serialize(),
            success: function (msg, status, request) {
                var shortUrl = request.getResponseHeader('Location'); // Obtiene la URL corta de los encabezados de la respuesta
                console.log("Short URL: ", shortUrl); // Imprime en consola en lugar de alerta
                $("#result1").html(
                    "<a id='shortUrlLink' target='_blank' href='"
                    + shortUrl
                    + "'>"
                    + shortUrl
                    + "</a>"
                );
                if (msg.properties.qr && msg.properties.qr !== "") {
                    var apiUrl = msg.properties.qr.split("/")[3] + "/qr";
                    console.log("QR URL: ", apiUrl);
                    $('#qrCodeImage').attr('src', apiUrl);
                    $('#qrCodeContainer').show();
                } else {
                    $('#qrCodeContainer').hide(); // Oculta el contenedor si no hay qr
                }
            },
            error: function () {
                $('#qrCodeContainer').hide();
                $("#result1").html("<div>ERROR</div>");
            }
        });
    });

    $("#csvForm").submit(function (event) {
        event.preventDefault();

        // Formulario de datos para enviar archivos
        var formData = new FormData(this);

        $.ajax({
            type: "POST",
            url: "/api/bulk",
            data: formData,
            processData: false,
            contentType: false,
            success: function (data, status, request) {
                console.log("bien ");
                var downloadUrl = window.URL.createObjectURL(new Blob([data]));
                var link = document.createElement('a');
                link.href = downloadUrl;
                link.setAttribute('download', 'output.csv');
                document.body.appendChild(link);
                link.click();
                link.remove();
                $("#result2").html("<div>El CSV ha sido procesado. Descargue el archivo haciendo clic <a href='" + downloadUrl + "' download='output.csv'>aquí</a>.</div>");
            },
            error: function () {
                console.log("mal ");
                $("#result2").html("<div>ERROR</div>");
            }
        });
    });
});
let ws;

function connect() {
    return new Promise((resolve, reject) => {
        const host = window.location.hostname;
        const port = 8080; // Replace with your actual server port
        const wsUrl = `ws://${host}:${port}/api/fast-bulk`;

        ws = new WebSocket(wsUrl);

        ws.onopen = function () {
            console.log("Connected to WebSocket");
            resolve();
        };

        ws.onmessage = function (event) {
            console.log("Message received: " + event.data);
        };

        ws.onclose = function (event) {
            console.log("WebSocket connection closed: " + event.reason);
            reject();
        };

        ws.onerror = function (error) {
            console.log("WebSocket error: " + error);
            reject();
        };
    });
}

function sendMessage(message) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(message);
    } else {
        console.log("WebSocket connection not open");
    }
}

document.addEventListener("DOMContentLoaded", function () {
    const csvForm = document.getElementById("csvForm");
    const sendButton = document.getElementById("Premium");

    sendButton.addEventListener("click", function () {
        connect()
            .then(() => {
                // WebSocket connection is established, now you can send CSV data
                const fileInput = document.querySelector('input[type="file"]');
                const file = fileInput.files[0];

                if (file) {
                    processCSV(file);
                } else {
                    console.log("Please select a CSV file");
                }
            })
            .catch((reason) => {
                console.log("WebSocket connection failed: " + reason);
            });
    });
});

function processCSV(file) {
    const reader = new FileReader();

    reader.onload = function (event) {
        const csvData = event.target.result.split('\n');
        csvData.forEach((line, index) => {
            // Ignore the first line (index 0)
            if (index > 0 && line !== null && line !== "" && line !== undefined) {
                sendMessage(line);
            }
        });
    };

    reader.readAsText(file);
}

