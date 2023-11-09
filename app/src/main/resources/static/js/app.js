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
                $("#result1").html(
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
                $("#result1").html("<div class='alert alert-danger lead'>ERROR</div>");
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
                $("#result2").html("<div class='alert alert-success'>El CSV ha sido procesado. Descargue el archivo haciendo clic <a href='" + downloadUrl + "' download='output.csv'>aquí</a>.</div>");
            },
            error: function () {
                console.log("mal ");
                $("#result2").html("<div class='alert alert-danger'>Ha ocurrido un error al procesar el CSV.</div>");
            }
        });
    });
});
