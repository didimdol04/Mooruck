package com.example.mooruckapp.network.parser

import android.util.Xml
import com.example.mooruckapp.network.dto.PlantDetail
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object PlantDetailXmlParser {

    fun parse(xml: String): PlantDetail {

        var contentNo = ""
        var name = ""

        // 광도
        var lightDemand = ""

        // 습도
        var humidity = ""

        // 온도
        var temperature = ""

        // 봄철 물주기 코드
        var springWaterCode = ""

        // 봄철 물주기 설명
        var springWaterDescription = ""

        // 대표 이미지
        var imageUrl = ""

        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
        }

        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {

                when (parser.name) {

                    "cntntsNo" -> {
                        contentNo = parser.nextText()
                    }

                    "distbNm" -> {
                        name = parser.nextText()
                    }

                    // 광도
                    "lighttdemanddoCodeNm" -> {
                        lightDemand = parser.nextText()
                    }

                    // 습도
                    "hdCodeNm" -> {
                        humidity = parser.nextText()
                    }

                    // 생육 온도
                    "grwhTpCodeNm" -> {
                        temperature = parser.nextText()
                    }

                    // 물주기 코드 (기본 - 봄철)
                    "watercycleSprngCode" -> {
                        springWaterCode = parser.nextText()
                    }
                }
            }

            eventType = parser.next()
        }

        return PlantDetail(
            contentNo = contentNo,
            name = name,
            lightDemand = lightDemand,
            humidity = humidity,
            temperature = temperature,
            springWaterCode = springWaterCode
        )
    }
}