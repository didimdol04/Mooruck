package com.example.mooruckapp.network.parser

import android.util.Xml
import com.example.mooruckapp.network.dto.PlantItem
import com.example.mooruckapp.network.dto.PlantSearchResult
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object PlantXmlParser {

    fun parsePlantSearchResult(
        xml: String,
    ): PlantSearchResult {

        val plants = mutableListOf<PlantItem>()

        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
        }

        var eventType = parser.eventType

        var contentNo = ""
        var title = ""

        var pageNo = 1
        var numOfRows = 20
        var totalCount = 0

        var isPlantItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            isPlantItem = true
                            contentNo = ""
                            title = ""
                        }

                        "cntntsNo" -> {
                            if (isPlantItem) {
                                contentNo = parser.nextText()
                            }
                        }

                        "cntntsSj" -> {
                            if (isPlantItem) {
                                title = parser.nextText()
                            }
                        }

                        "pageNo" -> {
                            pageNo = parser.nextText()
                                .toIntOrNull()
                                ?: 1
                        }

                        "numOfRows" -> {
                            numOfRows = parser.nextText()
                                .toIntOrNull()
                                ?: 20
                        }

                        "totalCount" -> {
                            totalCount = parser.nextText()
                                .toIntOrNull()
                                ?: 0
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (
                        parser.name == "item" &&
                        isPlantItem
                    ) {
                        if (
                            contentNo.isNotBlank() &&
                            title.isNotBlank()
                        ) {
                            plants.add(
                                PlantItem(
                                    contentNo = contentNo,
                                    title = title,
                                ),
                            )
                        }

                        isPlantItem = false
                    }
                }
            }

            eventType = parser.next()
        }

        return PlantSearchResult(
            plants = plants,
            pageNo = pageNo,
            numOfRows = numOfRows,
            totalCount = totalCount,
        )
    }
}