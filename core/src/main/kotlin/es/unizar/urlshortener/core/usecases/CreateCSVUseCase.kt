package es.unizar.urlshortener.core.usecases


import es.unizar.urlshortener.core.CsvOutput


interface CreateCSVUseCase {
    fun buildCsvContent(outputList: List<CsvOutput>): String
}
class CreateCSVUseCaseImpl : CreateCSVUseCase{
    override fun buildCsvContent(outputList: List<CsvOutput>): String {
        val csvContent = StringBuilder()
        csvContent.append("URI,URI_recortada,Mensaje")
        csvContent.append("\n")
    
        for (output in outputList) {
            csvContent.append("${output.originalUri},${output.shortenedUri},${output.explanation}")
            csvContent.append("\n")
        }
    
        return csvContent.toString()
    }
}
