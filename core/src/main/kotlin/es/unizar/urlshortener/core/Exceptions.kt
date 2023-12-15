package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class RetryAfterException : Exception("Rediccion no es segura, por favor espere 20 segundos")

class RedirectionForbidden(key: String) : Exception("Redirection for [$key] is forbiden")

class CSVCouldNotBeProcessed : Exception("The CSV could not be processed")
