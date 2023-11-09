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
                var apiUrl = msg.properties.qr.split("/")[3] + "/qr";
                console.log("API URL: ", apiUrl); // Imprime en consola en lugar de alerta
                $("#result").html(
                    "<div class='alert alert-success lead'><a id='shortUrlLink' target='_blank' href='"
                    + shortUrl
                    + "'>"
                    + shortUrl
                    + "</a></div>"
                );
                $('#qrCodeImage').attr('src', apiUrl);
                $('#qrCodeContainer').show();
            },
            error: function () {
                $('#qrCodeContainer').hide();
                $("#result").html("<div class='alert alert-danger lead'>ERROR</div>");
            }
        });
    });
});
