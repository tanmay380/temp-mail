package com.tanmay.tempmail.temp_mail

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TempMailApplication

fun main(args: Array<String>) {
	runApplication<TempMailApplication>(*args)
}
