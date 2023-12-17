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
    fun processAndBuildCsv(inputStream: InputStream, ip: String?): Pair<String, String?>
}

class CreateCSVUseCaseImpl(
    private val shortUrlUseCase: CreateShortUrlUseCase,
    private val linkToService: LinkToService
) : CreateCSVUseCase{
    private val logger: Logger = LoggerFactory.getLogger(CreateCSVUseCaseImpl::class.java)

    override fun processAndBuildCsv(inputStream: InputStream, ip: String?): Pair<String, String?> {
        val byteArrayInputStream = toByteArrayInputStream(inputStream)

        // Detectar el separador
        val separator = detectCsvSeparator(byteArrayInputStream)
        byteArrayInputStream.reset() // Reiniciar para la próxima lectura

        // Procesar el archivo CSV
        val csvOutputs = processCsvFile(byteArrayInputStream, separator, ip)

        // Construir el contenido del CSV
        val firstShortenedUri = csvOutputs.firstOrNull()?.shortenedUri

        logger.info("CSV generado")

        return Pair(buildCsvContent(csvOutputs, separator), firstShortenedUri)
    }

    private fun buildCsvContent(outputList: List<CsvOutput>, separator: Char): String {
        val csvContent = StringBuilder()
        csvContent.append("URI${separator}URI_recortada${separator}QR${separator}" +
                "Mensaje${separator}Estado de validación\n")

        for (output in outputList) {
            csvContent.append("${output.originalUri}$separator${output.shortenedUri}$separator" +
                    "${output.qr}$separator${output.explanation}$separator${output.status}\n")
        }

        return csvContent.toString()
    }

    private fun processCsvFile(inputStream: InputStream, separator: Char, ip: String?): List<CsvOutput> {
        val csvOutputList = mutableListOf<CsvOutput>()

        val csvReader = CSVReaderBuilder(InputStreamReader(inputStream)).withCSVParser(
            CSVParserBuilder().withSeparator(separator).build()
        ).build()

        val lines = csvReader.readAll()

        // Verificar los encabezados
        if (lines.isEmpty() || lines.first().toList() != listOf("URI", "QR")) {
            throw CSVCouldNotBeProcessed()
        }

        // Ignorar la primera línea (encabezados) y procesar las demás
        for (line in lines.drop(1)) {
            if (line.size != 2) {
                throw CSVCouldNotBeProcessed()
            }
            csvOutputList.add(processCsvLine(line.toList(), ip))
        }

        return csvOutputList
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processCsvLine(line: List<String>, ip: String?): CsvOutput {
        val uri = line[0].trim()
        val qrCodeIndicator = line[1].trim()
        var errorMessage: String? = "no error"
        var shortUrl: String? = null
        var qrUrl: String? = null
        lateinit var safe : String
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
            errorMessage = e.message
        }


        return CsvOutput(uri, shortUrl ?: "Error", qrUrl ?: "Error", errorMessage!!, safe)
    }

    private fun detectCsvSeparator(inputStream: InputStream): Char {
        inputStream.bufferedReader().use { reader ->
            val line = reader.readLine() ?: return ',' // Default to comma if file is empty

            val possibleSeparators = arrayOf(',', ';', '\t', '|')
            val separatorCounts = possibleSeparators.associateWith { line.count { ch -> ch == it } }

            return separatorCounts.maxByOrNull { it.value }?.key ?: ','
        }
    }

    private fun toByteArrayInputStream(inputStream: InputStream): ByteArrayInputStream {
        val bytes = inputStream.readAllBytes()
        return ByteArrayInputStream(bytes)
    }
}
