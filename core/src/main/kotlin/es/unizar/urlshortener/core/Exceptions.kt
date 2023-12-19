package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class RetryAfterException : Exception("Redirection not safe, please wait 60 seconds")

class RedirectionForbidden(key: String) : Exception("Redirection for [$key] is forbidden")

class CSVCouldNotBeProcessed : Exception("The CSV could not be processed")
