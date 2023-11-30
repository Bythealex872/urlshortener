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
    ws = new WebSocket("ws://localhost:8080/api/bulk-fast"); // Ajusta la URL según sea necesario

    ws.onopen = function() {
        console.log("Conectado al WebSocket");
    };

    ws.onmessage = function(event) {
        console.log("Mensaje recibido: " + event.data);
    };

    ws.onclose = function(event) {
        console.log("Conexión cerrada: " + event.reason);
    };

    ws.onerror = function(error) {
        console.log("Error en WebSocket: " + error);
    };
}

function sendMessage(message) {
    ws.send(message);
}

document.addEventListener("DOMContentLoaded", function() {
    connect();

    const sendButton = document.getElementById("sendButton");
    sendButton.addEventListener("click", function() {
        const uri = "tu_uri_aquí"; // Reemplaza con la URI que deseas enviar
        sendMessage(uri);
    });
});