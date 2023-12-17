@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import es.unizar.urlshortener.core.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface CreateCSVUseCase {
    /** Método para procesar un archivo CSV y construir su contenido
     * @param inputStream: Flujo de entrada que representa el contenido del archivo CSV
     * @param ip: Dirección IP opcional
     * @return Pair<String, String?>: Par de cadenas, donde la primera es el contenido del CSV construido
     */
    fun processAndBuildCsv(inputStream: InputStream, ip: String?): Pair<String, String?>
}

class CreateCSVUseCaseImpl(
    private val shortUrlUseCase: CreateShortUrlUseCase,
    private val linkToService: LinkToService
) : CreateCSVUseCase{
    private val logger: Logger = LoggerFactory.getLogger(CreateCSVUseCaseImpl::class.java)

    /**
    * Implementación del método de la interfaz para procesar y construir el archivo CSV
    * @param inputStream: Flujo de entrada que representa el contenido del archivo CSV
    * @param ip: Dirección IP opcional
    * @return Pair<String, String?>: Par de cadenas, donde la primera es el contenido del CSV construido
    */
    override fun processAndBuildCsv(inputStream: InputStream, ip: String?): Pair<String, String?> {
        val byteArrayInputStream = toByteArrayInputStream(inputStream)

        logger.info("Detectando separador de CSV")
        val separator = detectCsvSeparator(byteArrayInputStream)
        byteArrayInputStream.reset() // Reiniciar para la próxima lectura

        val csvOutputs = processCsvFile(byteArrayInputStream, separator, ip)

        // Construir el contenido del CSV

        val firstShortenedUri = csvOutputs.firstOrNull()?.shortenedUri

        logger.info("CSV generado")

        return Pair(buildCsvContent(csvOutputs, separator), firstShortenedUri)
    }
    /** Método para construir el contenido del CSV a partir de la salida procesada
    * @param outputList: Lista de objetos CsvOutput, que contiene información procesada del CSV
    * @param separator: Carácter utilizado como separador en el CSV
    * @return String: Contenido del CSV construido como una cadena
    */
    private fun buildCsvContent(outputList: List<CsvOutput>, separator: Char): String {
        val csvContent = StringBuilder()
        logger.info("Construyendo CSV")
        csvContent.append("URI${separator}URI_recortada${separator}QR${separator}" +
                "Mensaje${separator}Estado de validacion\n")

        for (output in outputList) {
            csvContent.append("${output.originalUri}$separator${output.shortenedUri}$separator" +
                    "${output.qr}$separator${output.explanation}$separator${output.status}\n")
        }
        logger.info("CSV construido")
        return csvContent.toString()
    }
    /** Método para procesar el archivo CSV y generar una lista de salida
    * @param inputStream: Flujo de entrada que representa el contenido del archivo CSV
    * @param separator: Carácter utilizado como separador en el CSV
    * @param ip: Dirección IP opcional
    * @return List<CsvOutput>: Lista de objetos CsvOutput, que contiene información procesada del CSV
    */
    private fun processCsvFile(inputStream: InputStream, separator: Char, ip: String?): List<CsvOutput> {
        val csvOutputList = mutableListOf<CsvOutput>()

        val csvReader = CSVReaderBuilder(InputStreamReader(inputStream)).withCSVParser(
            CSVParserBuilder().withSeparator(separator).build()
        ).build()

        val lines = csvReader.readAll()

        logger.info("Procesando CSV")
        if (lines.isEmpty() || lines.first().toList() != listOf("URI", "QR")) {
            logger.error("Formato de CSV no válido")
            throw CSVCouldNotBeProcessed()
        }

        // Ignorar la primera línea (encabezados) y procesar las demás
        for (line in lines.drop(1)) {
            if (line.size != 2) {
                logger.error("Formato de CSV no válido")
                throw CSVCouldNotBeProcessed()
            }
            csvOutputList.add(processCsvLine(line.toList(), ip))
        }
        logger.info("CSV procesado")
        return csvOutputList
    }

    /**
     * Método para procesar una línea del CSV y generar un objeto CsvOutput
     * @param [line]: Lista de cadenas que representa una línea del CSV
     * @param [ip]: Dirección IP opcional
     * @return [CsvOutput]: Objeto que contiene información procesada de una línea del CSV
     */
    @Suppress("TooGenericExceptionCaught")
    private fun processCsvLine(line: List<String>, ip: String?): CsvOutput {
        val uri = line[0].trim()
        val qrCodeIndicator = line[1].trim()
        var errorMessage: String? = "no error"
        var shortUrl: String? = null
        var qrUrl: String? = null
        lateinit var safe : String
        logger.info("Procesando URI: $uri")
        try {
            val create = shortUrlUseCase.create(
                url = uri,
                qrRequest = qrCodeIndicator == "1",
                data = ShortUrlProperties(ip = ip)
            )
            val uriObj = linkToService.link(create.hash)
            shortUrl = uriObj.toString()
            qrUrl = if (qrCodeIndicator == "1") "$shortUrl/qr" else "no_qr"
            safe = if (create.properties.safe == null) {
                "URL pendiente de validacion"
            } else {
                if (create.properties.safe) {
                    "URL segura"
                } else {
                    "URL no segura"
                }
            }
        } catch (e: Exception) {
            logger.error("Error al procesar URI: $uri")
            errorMessage = e.message
        }

        logger.info("URI procesada: $uri")
        return CsvOutput(uri, shortUrl ?: "Error", qrUrl ?: "Error", errorMessage!!, safe)
    }
    /**
    * Método para detectar el separador del CSV a partir de la primera línea
    * @param[inputStream]: Flujo de entrada que representa el contenido del archivo CSV
    * @return [Char]: Carácter que se detecta como el separador más probable en el CSV
    */
    private fun detectCsvSeparator(inputStream: InputStream): Char {
        logger.info("Detectando separador de CSV")
        inputStream.bufferedReader().use { reader ->
            val line = reader.readLine() ?: return ',' // Si el archivo está vacío, devuelve la coma como separador

            val possibleSeparators = arrayOf(',', ';', '\t', '|')
            val separatorCounts = possibleSeparators.associateWith { line.count { ch -> ch == it } }
            logger.info("Separador detectado: ${separatorCounts.maxByOrNull { it.value }?.key}")
            return separatorCounts.maxByOrNull { it.value }?.key ?: ','
        }
    }
    /**
    * Método para convertir la entrada a un array de bytes
    * @param inputStream: Flujo de entrada que se convertirá
    * @return ByteArrayInputStream: Flujo de entrada como un objeto ByteArrayInputStream
    */
    private fun toByteArrayInputStream(inputStream: InputStream): ByteArrayInputStream {
        val bytes = inputStream.readAllBytes()
        return ByteArrayInputStream(bytes)
    }
}
